package com.enterprise.asset.ai.service;

import com.enterprise.asset.ai.dto.MessageRequest;
import com.enterprise.asset.ai.dto.MessageResponse;
import com.enterprise.asset.ai.entity.AiConversation;
import com.enterprise.asset.ai.entity.AiMessage;
import com.enterprise.asset.ai.exception.BusinessException;
import com.enterprise.asset.ai.exception.ForbiddenException;
import com.enterprise.asset.ai.exception.NotFoundException;
import com.enterprise.asset.ai.repository.AiConversationRepository;
import com.enterprise.asset.ai.repository.AiMessageRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 聊天服务：会话/消息管理 + 上下文缓存 + RAG 检索 + 流式响应。
 */
@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final TaskExecutor chatTaskExecutor;

    @Value("${chat.max-history-size:20}")
    private int maxHistorySize;

    @Value("${chat.max-message-length:4000}")
    private int maxMessageLength;

    @Value("${chat.max-tokens:4096}")
    private int maxTokens;

    @Value("${chat.cache-expire-hours:24}")
    private int cacheExpireHours;

    @Value("${rag.enabled:true}")
    private boolean ragEnabled;

    @Value("${rag.retrieval.top-k:3}")
    private int ragTopK;

    @Value("${rag.retrieval.similarity-threshold:0.30}")
    private double ragSimilarityThreshold;

    @Value("${rag.knowledge.default-tenant:asset}")
    private String ragTenant;

    @Autowired(required = false)
    private VectorStore vectorStore;

    private static final String CHAT_HISTORY_KEY_PREFIX = "ai:chat:history:";

    /** 缓存专用轻量消息结构，Spring AI 的 Message 实现类无法被 Jackson 直接反序列化 */
    private static final class CachedMessage {
        public String role;
        public String content;

        CachedMessage() {
        }

        CachedMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public ChatService(ChatClient.Builder chatClientBuilder,
            AiConversationRepository conversationRepository,
            AiMessageRepository messageRepository,
            RedisTemplate<String, Object> redisTemplate,
            @Qualifier("chatTaskExecutor") TaskExecutor chatTaskExecutor) {
        this.chatClient = chatClientBuilder.build();
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.chatTaskExecutor = chatTaskExecutor;
    }

    @Transactional
    public AiConversation createConversation(Long userId, String title) {
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(normalizeTitle(title));
        return conversationRepository.save(conversation);
    }

    private String normalizeTitle(String title) {
        if (title == null) {
            return "新对话";
        }
        String cleaned = title.replaceAll("\\p{Cntrl}", " ").trim();
        if (cleaned.isEmpty()) {
            return "新对话";
        }
        return cleaned.length() > 200 ? cleaned.substring(0, 200) : cleaned;
    }

    public List<AiConversation> getConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdateTimeDesc(userId);
    }

    public AiConversation getConversation(Long userId, Long conversationId) {
        AiConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("会话不存在"));

        if (!conversation.getUserId().equals(userId)) {
            throw new ForbiddenException("无权访问该会话");
        }

        return conversation;
    }

    @Transactional
    public void deleteMessage(Long userId, Long messageId) {
        AiMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("消息不存在"));

        Long conversationId = message.getConversationId();
        getConversation(userId, conversationId);

        messageRepository.delete(message);
        deleteChatHistoryAfterCommit(conversationId);
    }

    @Transactional
    public void batchDeleteMessages(Long userId, List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            throw new BusinessException("消息ID列表不能为空");
        }

        List<AiMessage> messages = messageRepository.findAllById(messageIds);
        if (messages.isEmpty()) {
            throw new NotFoundException("未找到任何消息");
        }

        Long conversationId = messages.get(0).getConversationId();
        getConversation(userId, conversationId);

        for (AiMessage message : messages) {
            if (!message.getConversationId().equals(conversationId)) {
                throw new BusinessException("所有消息必须属于同一个会话");
            }
        }

        messageRepository.deleteAll(messages);
        deleteChatHistoryAfterCommit(conversationId);
    }

    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        AiConversation conversation = getConversation(userId, conversationId);

        messageRepository.deleteByConversationId(conversationId);
        conversationRepository.delete(conversation);

        deleteChatHistoryAfterCommit(conversationId);
    }

    public List<MessageResponse> getMessages(Long userId, Long conversationId) {
        return getMessages(userId, conversationId, null, null);
    }

    /**
     * limit 为空时返回全部；返回结果按时间正序，beforeId 用于向上加载更早历史。
     */
    public List<MessageResponse> getMessages(Long userId, Long conversationId, Long beforeId, Integer limit) {
        getConversation(userId, conversationId);

        if (limit == null && beforeId == null) {
            return messageRepository.findByConversationIdOrderByIdAsc(conversationId)
                    .stream()
                    .map(this::convertToMessageResponse)
                    .collect(Collectors.toList());
        }

        int pageSize = Math.min(Math.max(limit == null ? 100 : limit, 1), 500);
        Pageable pageable = PageRequest.of(0, pageSize);
        List<AiMessage> page = beforeId == null
                ? messageRepository.findByConversationIdOrderByIdDesc(conversationId, pageable)
                : messageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(
                        conversationId, beforeId, pageable);

        List<AiMessage> ordered = new ArrayList<>(page);
        Collections.reverse(ordered);

        return ordered.stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());
    }

    private List<Message> getOrCreateChatHistory(Long conversationId) {
        String cacheKey = CHAT_HISTORY_KEY_PREFIX + conversationId;

        try {
            Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
            if (cachedObj != null) {
                List<CachedMessage> cached = objectMapper.readValue(cachedObj.toString(),
                        new TypeReference<List<CachedMessage>>() {
                        });
                List<Message> history = toMessages(cached);
                if (!history.isEmpty()) {
                    trimChatHistory(history);
                    return history;
                }
            }
        } catch (Exception e) {
            logger.warn("read chat history cache failed, conversationId={}, cause={}",
                    conversationId, e.toString());
        }

        List<Message> historyFromDb = loadChatHistoryFromDb(conversationId);
        if (!historyFromDb.isEmpty()) {
            saveChatHistory(conversationId, historyFromDb);
        }
        return historyFromDb;
    }

    private List<Message> loadChatHistoryFromDb(Long conversationId) {
        List<Message> history = new ArrayList<>();

        try {
            // 上下文最终只保留 maxHistorySize 条，按 id 倒序取最近一页即可
            Pageable recentPage = PageRequest.of(0, Math.max(maxHistorySize + 2, 10));
            List<AiMessage> dbMessages = new ArrayList<>(
                    messageRepository.findByConversationIdOrderByIdDesc(conversationId, recentPage));
            Collections.reverse(dbMessages);

            for (AiMessage dbMsg : dbMessages) {
                if ("user".equals(dbMsg.getRole())) {
                    history.add(new UserMessage(dbMsg.getContent()));
                } else if ("assistant".equals(dbMsg.getRole())) {
                    history.add(new AssistantMessage(dbMsg.getContent()));
                }
            }

            trimChatHistory(history);
        } catch (Exception e) {
            logger.warn("load chat history from db failed, conversationId={}, cause={}",
                    conversationId, e.toString());
        }

        return history;
    }

    private void saveChatHistory(Long conversationId, List<Message> chatHistory) {
        String cacheKey = CHAT_HISTORY_KEY_PREFIX + conversationId;

        try {
            List<CachedMessage> cached = new ArrayList<>(chatHistory.size());
            for (Message message : chatHistory) {
                String role = resolveRole(message);
                if (role != null && message.getContent() != null) {
                    cached.add(new CachedMessage(role, message.getContent()));
                }
            }
            String historyJson = objectMapper.writeValueAsString(cached);
            redisTemplate.opsForValue().set(cacheKey, historyJson, Duration.ofHours(cacheExpireHours));
        } catch (Exception e) {
            logger.warn("saveChatHistory failed, conversationId={}, cause={}", conversationId, e.toString());
        }
    }

    private List<Message> toMessages(List<CachedMessage> cached) {
        List<Message> history = new ArrayList<>();
        if (cached == null) {
            return history;
        }
        for (CachedMessage message : cached) {
            if (message == null || message.content == null) {
                continue;
            }
            if ("user".equals(message.role)) {
                history.add(new UserMessage(message.content));
            } else if ("assistant".equals(message.role)) {
                history.add(new AssistantMessage(message.content));
            }
        }
        return history;
    }

    private String resolveRole(Message message) {
        MessageType type = message.getMessageType();
        if (type == MessageType.USER) {
            return "user";
        }
        if (type == MessageType.ASSISTANT) {
            return "assistant";
        }
        return null;
    }

    private void deleteChatHistoryAfterCommit(Long conversationId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteChatHistory(conversationId);
                }
            });
        } else {
            deleteChatHistory(conversationId);
        }
    }

    private void deleteChatHistory(Long conversationId) {
        String cacheKey = CHAT_HISTORY_KEY_PREFIX + conversationId;

        // 单次尝试即可：重试只会让请求线程在 Redis 故障时长时间阻塞，缓存最终由 TTL 兜底
        try {
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            logger.warn("Redis cache delete failed, key={}, cause={}", cacheKey, e.toString());
        }
    }

    /**
     * 同步发送。不开启事务：模型调用可能持续数秒，包在事务里会长期占用数据库连接。
     */
    public MessageResponse sendMessage(Long userId, MessageRequest request) {
        validateRequest(request);

        Long conversationId = request.getConversationId();
        if (conversationId == null) {
            conversationId = createConversation(userId, null).getId();
        } else {
            getConversation(userId, conversationId);
        }

        List<Message> chatHistory = getOrCreateChatHistory(conversationId);
        saveUserMessage(conversationId, request.getContent());

        String aiResponse = callAiModel(chatHistory, request.getContent());

        chatHistory.add(new UserMessage(request.getContent()));
        chatHistory.add(new AssistantMessage(aiResponse));
        trimChatHistory(chatHistory);

        AiMessage aiMessage = new AiMessage();
        aiMessage.setConversationId(conversationId);
        aiMessage.setRole("assistant");
        aiMessage.setContent(aiResponse);
        messageRepository.save(aiMessage);

        saveChatHistory(conversationId, chatHistory);
        touchConversation(conversationId, request.getContent());

        return convertToMessageResponse(aiMessage);
    }

    public Flux<String> streamMessage(Long userId, MessageRequest request) {
        logger.info("streamMessage start, userId={}, conversationId={}, contentLength={}",
                userId,
                request != null ? request.getConversationId() : null,
                request != null && request.getContent() != null ? request.getContent().length() : null);

        try {
            validateRequest(request);
        } catch (RuntimeException e) {
            return Flux.error(e);
        }

        Long conversationId = request.getConversationId();
        if (conversationId == null) {
            conversationId = createConversation(userId, null).getId();
        } else {
            getConversation(userId, conversationId);
        }

        final Long finalConversationId = conversationId;
        final String userContent = request.getContent();

        // 先读历史再落库用户消息，保证模型入参中的历史不包含当前这条
        List<Message> chatHistory = getOrCreateChatHistory(finalConversationId);
        saveUserMessage(finalConversationId, userContent);
        scheduleTitleUpdate(finalConversationId, userContent);

        return streamAiResponse(chatHistory, userContent, finalConversationId);
    }

    private void validateRequest(MessageRequest request) {
        if (request == null) {
            throw new BusinessException("请求不能为空");
        }
        String content = request.getContent();
        if (content == null || content.isBlank()) {
            throw new BusinessException("消息内容不能为空");
        }
        if (content.length() > maxMessageLength) {
            throw new BusinessException("消息长度不能超过" + maxMessageLength + "个字符");
        }
    }

    private void saveUserMessage(Long conversationId, String content) {
        AiMessage userMessage = new AiMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setRole("user");
        userMessage.setContent(content);
        messageRepository.save(userMessage);
        logger.debug("User message saved, conversationId={}", conversationId);
    }

    private void scheduleTitleUpdate(Long conversationId, String userContent) {
        try {
            chatTaskExecutor.execute(() -> {
                try {
                    updateConversationTitle(conversationId, userContent);
                } catch (Exception e) {
                    logger.error("Failed to update conversation title asynchronously", e);
                }
            });
        } catch (RejectedExecutionException e) {
            logger.warn("chatTaskExecutor rejected title update, running inline, conversationId={}", conversationId);
            updateConversationTitle(conversationId, userContent);
        }
    }

    private void updateConversationTitle(Long conversationId, String userContent) {
        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            String newTitle = generateTitle(userContent, conversation.getTitle());
            if (!newTitle.equals(conversation.getTitle())) {
                conversation.setTitle(newTitle);
            }
            conversation.setUpdateTime(LocalDateTime.now());
            conversationRepository.save(conversation);
            logger.debug("Conversation touched, conversationId={}, title={}", conversationId, newTitle);
        });
    }

    private void touchConversation(Long conversationId, String userContent) {
        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            conversation.setTitle(generateTitle(userContent, conversation.getTitle()));
            conversation.setUpdateTime(LocalDateTime.now());
            conversationRepository.save(conversation);
        });
    }

    private String callAiModel(List<Message> chatHistory, String userContent) {
        return chatClient.prompt()
                .messages(buildMessages(chatHistory, userContent))
                .call()
                .content();
    }

    private Flux<String> streamAiResponse(List<Message> chatHistory,
            String userContent, Long conversationId) {
        int maxRetries = 2;
        AtomicInteger retryCount = new AtomicInteger(0);
        // 一旦有内容发出就不再自动重试，避免重复内容拼接
        AtomicInteger emittedChars = new AtomicInteger(0);
        // RAG 检索（含 Embedding 网络调用）延迟到订阅时执行，重试复用同一份请求消息
        AtomicReference<List<Message>> requestMessagesRef = new AtomicReference<>();

        Supplier<Flux<String>> attempt = () -> {
            List<Message> requestMessages = requestMessagesRef.get();
            if (requestMessages == null) {
                requestMessages = buildMessages(chatHistory, userContent);
                requestMessagesRef.set(requestMessages);
            }
            return doStreamAiResponse(requestMessages, chatHistory, userContent, conversationId, emittedChars);
        };

        // subscribeOn：把向量检索等阻塞调用从 Servlet 请求线程移到弹性线程池
        return Flux.defer(attempt)
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(error -> {
                    int currentRetry = retryCount.incrementAndGet();
                    boolean retryable = currentRetry <= maxRetries && isRetryableError(error);
                    if (!retryable) {
                        logger.error("streamAiResponse failed after {} attempts, conversationId={}",
                                currentRetry, conversationId, error);
                        return Flux.error(error);
                    }
                    if (emittedChars.get() > 0) {
                        logger.warn("streamAiResponse skip retry: {} chars already emitted, conversationId={}",
                                emittedChars.get(), conversationId, error);
                        return Flux.error(error);
                    }
                    logger.warn("streamAiResponse retry {}/{}, conversationId={}",
                            currentRetry, maxRetries, conversationId);
                    Flux<String> retryFlux = Flux.defer(attempt);
                    return isBackoffError(error) ? retryFlux.delaySubscription(Duration.ofMillis(400)) : retryFlux;
                });
    }

    private Flux<String> doStreamAiResponse(List<Message> requestMessages,
            List<Message> chatHistory, String userContent, Long conversationId,
            AtomicInteger emittedChars) {
        StringBuilder fullResponse = new StringBuilder();
        AtomicBoolean persisted = new AtomicBoolean(false);

        return chatClient.prompt()
                .messages(requestMessages)
                .stream()
                .content()
                // 落库/写缓存是阻塞操作，切到弹性线程池，避免占用事件循环
                .publishOn(Schedulers.boundedElastic())
                .map(content -> {
                    fullResponse.append(content);
                    emittedChars.addAndGet(content.length());
                    return content;
                })
                .doOnComplete(() -> {
                    logger.info("streamAiResponse completed, conversationId={}, responseLength={}",
                            conversationId, fullResponse.length());
                    persistResponseOnce(persisted, conversationId, fullResponse, chatHistory, userContent);
                })
                .doOnError(error -> logger.error(
                        "streamAiResponse failed, conversationId=" + conversationId, error))
                .doFinally(signal -> {
                    // 客户端断开/上游异常时也落库已生成的部分回答
                    if (signal != SignalType.ON_COMPLETE) {
                        persistResponseOnce(persisted, conversationId, fullResponse, chatHistory, userContent);
                    }
                });
    }

    private void persistResponseOnce(AtomicBoolean persisted, Long conversationId, StringBuilder fullResponse,
            List<Message> chatHistory, String userContent) {
        if (!persisted.compareAndSet(false, true)) {
            return;
        }
        try {
            if (fullResponse.length() == 0) {
                // 未产生任何内容也要刷新缓存，否则已落库的用户消息在下一轮上下文里会缺失
                refreshHistoryCache(conversationId, chatHistory, userContent);
            } else {
                saveAiResponse(conversationId, fullResponse.toString(), chatHistory, userContent);
            }
        } catch (Exception e) {
            logger.error("Failed to save AI response, conversationId={}", conversationId, e);
        }
    }

    private void refreshHistoryCache(Long conversationId, List<Message> chatHistory, String userContent) {
        if (!conversationRepository.existsById(conversationId)) {
            return;
        }
        List<Message> updatedHistory = new ArrayList<>(chatHistory);
        updatedHistory.add(new UserMessage(userContent));
        trimChatHistory(updatedHistory);
        saveChatHistory(conversationId, updatedHistory);
    }

    private boolean isRetryableError(Throwable error) {
        String message = error.getMessage();
        return message != null && (message.contains("Connection reset")
                || message.contains("connect timed out")
                || message.contains("timeout")
                || message.contains("429")
                || message.contains("502")
                || message.contains("503")
                || message.contains("504"));
    }

    private boolean isBackoffError(Throwable error) {
        String message = error.getMessage();
        if (message == null) {
            return false;
        }
        return message.contains("429") || message.matches("(?s).*\\b5\\d\\d\\b.*");
    }

    private void saveAiResponse(Long conversationId, String aiContent,
            List<Message> chatHistory, String userContent) {
        // 流式响应期间会话可能已被删除，避免写入孤儿消息和重建缓存
        if (!conversationRepository.existsById(conversationId)) {
            logger.warn("Skip saving AI response, conversation already deleted, conversationId={}", conversationId);
            return;
        }

        List<Message> updatedHistory = new ArrayList<>(chatHistory);
        updatedHistory.add(new UserMessage(userContent));
        updatedHistory.add(new AssistantMessage(aiContent));
        trimChatHistory(updatedHistory);

        AiMessage aiMessage = new AiMessage();
        aiMessage.setConversationId(conversationId);
        aiMessage.setRole("assistant");
        aiMessage.setContent(aiContent);
        AiMessage saved = messageRepository.save(aiMessage);

        if (!conversationRepository.existsById(conversationId)) {
            messageRepository.delete(saved);
            logger.warn("Conversation deleted during stream, removed orphan message, conversationId={}", conversationId);
            return;
        }

        saveChatHistory(conversationId, updatedHistory);
    }

    private List<Message> buildMessages(List<Message> chatHistory, String userContent) {
        List<Message> messages = new ArrayList<>();

        String ragContext = "";
        if (ragEnabled && vectorStore != null) {
            ragContext = retrieveRagContext(userContent);
        }

        messages.add(new SystemMessage(buildSystemMessage(ragContext)));
        messages.addAll(chatHistory);
        messages.add(new UserMessage(userContent));

        return messages;
    }

    private String buildSystemMessage(String ragContext) {
        StringBuilder systemMessage = new StringBuilder();
        systemMessage.append("你是企业资产智能管理平台的智能助手。\n\n");

        if (ragEnabled) {
            systemMessage.append("请参考以下内部资料回答用户问题：\n");
            systemMessage.append("------------------------\n");
            systemMessage.append(ragContext.isEmpty() ? "[暂无相关内部资料]" : ragContext);
            systemMessage.append("\n------------------------\n\n");
            systemMessage.append("要求：\n");
            systemMessage.append("1. 直接把答案讲清楚，不要提及“参考资料”“资料编号”“知识库”“来源”等字眼，也不要输出【参考资料1】这类引用标记。\n");
            systemMessage.append("2. 答案必须以上述内部资料为依据，禁止编造资料中没有的信息。\n");
            systemMessage.append("3. 如果内部资料中确实没有相关信息，直接说明“暂时没有查到相关内容”，并简要说明可以补充的方向。\n");
        } else {
            systemMessage.append("请用自然、友好的语言回答用户的问题。\n");
        }

        return systemMessage.toString();
    }

    private String retrieveRagContext(String query) {
        try {
            SearchRequest searchRequest = SearchRequest.query(query)
                    .withTopK(Math.max(ragTopK, ragTopK * 3))
                    .withSimilarityThreshold(ragSimilarityThreshold);

            List<Document> documents = vectorStore.similaritySearch(searchRequest);
            if (documents == null || documents.isEmpty()) {
                logger.debug("No RAG documents found for query: {}", query);
                return "";
            }

            // Redis 索引未把 tenant 设为可检索字段，应用层做租户隔离
            List<Document> matched = new ArrayList<>();
            for (Document document : documents) {
                Object tenant = document.getMetadata().get("tenant");
                String documentTenant = tenant == null ? ragTenant : tenant.toString();
                if (ragTenant.equals(documentTenant)) {
                    matched.add(document);
                    if (matched.size() >= ragTopK) {
                        break;
                    }
                }
            }

            if (matched.isEmpty()) {
                logger.debug("No RAG documents matched tenant={} for query: {}", ragTenant, query);
                return "";
            }

            logger.debug("RAG retrieved {} documents for query: {}", matched.size(), query);

            StringBuilder context = new StringBuilder();
            for (Document document : matched) {
                context.append(document.getContent()).append("\n\n");
            }
            return context.toString();
        } catch (Exception e) {
            logger.warn("RAG retrieval failed: {}", e.getMessage());
            return "";
        }
    }

    private void trimChatHistory(List<Message> chatHistory) {
        if (chatHistory == null || chatHistory.isEmpty()) {
            return;
        }

        int prefix = isSystemMessage(chatHistory.get(0)) ? 1 : 0;

        while (chatHistory.size() - prefix > maxHistorySize) {
            chatHistory.remove(prefix);
        }

        int totalTokens = estimateTokens(chatHistory);
        while (chatHistory.size() - prefix > 1 && totalTokens > maxTokens) {
            totalTokens -= estimateMessageTokens(chatHistory.get(prefix));
            chatHistory.remove(prefix);
        }

        // 裁剪后必须以用户消息开头，避免孤立回答
        while (chatHistory.size() - prefix > 0 && !isUserMessage(chatHistory.get(prefix))) {
            chatHistory.remove(prefix);
        }
    }

    private boolean isSystemMessage(Message message) {
        return message.getMessageType() == MessageType.SYSTEM;
    }

    private boolean isUserMessage(Message message) {
        return message.getMessageType() == MessageType.USER;
    }

    private int estimateTokens(List<Message> messages) {
        int total = 0;
        for (Message msg : messages) {
            total += estimateMessageTokens(msg);
        }
        return total;
    }

    private int estimateMessageTokens(Message message) {
        String content = message.getContent();
        if (content == null) {
            return 0;
        }
        // 粗略估算：中文按2字符=1token，英文按4字符=1token
        int chineseChars = content.replaceAll("[\\x00-\\xff]", "").length();
        int asciiChars = content.length() - chineseChars;
        return (chineseChars / 2) + (asciiChars / 4) + 4;
    }

    private String generateTitle(String userMessage, String currentTitle) {
        if (currentTitle != null && !currentTitle.equals("新对话")) {
            return currentTitle;
        }

        String title = userMessage.trim();
        if (title.length() > 30) {
            title = title.substring(0, 30) + "...";
        }
        return title;
    }

    private MessageResponse convertToMessageResponse(AiMessage message) {
        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getRole(),
                message.getContent(),
                message.getCreateTime());
    }
}

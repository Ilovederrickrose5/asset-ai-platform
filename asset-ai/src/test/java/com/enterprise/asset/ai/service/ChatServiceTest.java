package com.enterprise.asset.ai.service;

import com.enterprise.asset.ai.dto.MessageRequest;
import com.enterprise.asset.ai.entity.AiConversation;
import com.enterprise.asset.ai.entity.AiMessage;
import com.enterprise.asset.ai.repository.AiConversationRepository;
import com.enterprise.asset.ai.repository.AiMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ChatService 纯单元测试：仅使用 Mockito，不启动 Spring 上下文，
 * 不连接 MySQL / Redis / DashScope，可在任意环境离线运行。
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec chatClientRequestSpec;

    @Mock
    private ChatClient.StreamResponseSpec streamResponseSpec;

    @Mock
    private AiConversationRepository conversationRepository;

    @Mock
    private AiMessageRepository messageRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        lenient().when(chatClientBuilder.build()).thenReturn(chatClient);
        chatService = new ChatService(chatClientBuilder, conversationRepository, messageRepository, redisTemplate,
                Runnable::run);
        ReflectionTestUtils.setField(chatService, "maxHistorySize", 20);
        ReflectionTestUtils.setField(chatService, "maxMessageLength", 4000);
        ReflectionTestUtils.setField(chatService, "maxTokens", 4096);
        ReflectionTestUtils.setField(chatService, "cacheExpireHours", 24);
        ReflectionTestUtils.setField(chatService, "ragEnabled", false);
        ReflectionTestUtils.setField(chatService, "ragTopK", 3);
        ReflectionTestUtils.setField(chatService, "ragSimilarityThreshold", 0.30);
        ReflectionTestUtils.setField(chatService, "ragTenant", "asset");
    }

    @Test
    void streamMessage_rejectsOverlongContent() {
        String content = "x".repeat(4001);

        List<String> received = chatService.streamMessage(1L, new MessageRequest(1L, content))
                .onErrorResume(e -> Mono.just("ERR:" + e.getMessage()))
                .collectList()
                .block();

        assertThat(received).containsExactly("ERR:消息长度不能超过4000个字符");
        verify(conversationRepository, never()).findById(any());
    }

    @Test
    void streamMessage_rejectsBlankContent() {
        List<String> received = chatService.streamMessage(1L, new MessageRequest(1L, "   "))
                .onErrorResume(e -> Mono.just("ERR:" + e.getMessage()))
                .collectList()
                .block();

        assertThat(received).containsExactly("ERR:消息内容不能为空");
        verify(conversationRepository, never()).findById(any());
    }

    @Test
    void streamMessage_sendsCurrentUserMessageOnlyOnce() {
        stubConversation(5L, 1L);
        when(messageRepository.findByConversationIdOrderByIdDesc(eq(5L), any())).thenReturn(List.of());
        stubStreaming(Flux.just("回答"));

        chatService.streamMessage(1L, new MessageRequest(5L, "你好")).collectList().block();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<org.springframework.ai.chat.messages.Message>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(chatClientRequestSpec).messages(captor.capture());

        long currentUserMessageCount = captor.getValue().stream()
                .filter(message -> message instanceof UserMessage && "你好".equals(message.getContent()))
                .count();
        assertThat(currentUserMessageCount).isEqualTo(1);
    }

    @Test
    void streamMessage_reusesCachedHistory() {
        stubConversation(5L, 1L);
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ai:chat:history:5")).thenReturn(
                "[{\"role\":\"user\",\"content\":\"历史问题\"},{\"role\":\"assistant\",\"content\":\"历史回答\"}]");
        stubStreaming(Flux.just("回答"));

        chatService.streamMessage(1L, new MessageRequest(5L, "新问题")).collectList().block();

        verify(messageRepository, never()).findByConversationIdOrderByIdDesc(any(), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<org.springframework.ai.chat.messages.Message>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(chatClientRequestSpec).messages(captor.capture());
        assertThat(captor.getValue())
                .anyMatch(message -> "历史问题".equals(message.getContent()))
                .anyMatch(message -> "历史回答".equals(message.getContent()));
    }

    @Test
    void streamMessage_retryDoesNotDuplicateUserMessage() {
        stubConversation(5L, 1L);
        when(messageRepository.findByConversationIdOrderByIdDesc(eq(5L), any())).thenReturn(List.of());

        AtomicInteger attempts = new AtomicInteger();
        Flux<String> flaky = Flux.defer(() -> attempts.getAndIncrement() == 0
                ? Flux.error(new RuntimeException("Connection reset"))
                : Flux.just("回答"));
        stubStreaming(flaky);

        List<String> received = chatService.streamMessage(1L, new MessageRequest(5L, "你好"))
                .collectList()
                .block();

        assertThat(received).containsExactly("回答");
        assertThat(attempts.get()).isEqualTo(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<org.springframework.ai.chat.messages.Message>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(chatClientRequestSpec, times(2)).messages(captor.capture());
        for (List<org.springframework.ai.chat.messages.Message> sentMessages : captor.getAllValues()) {
            long currentUserMessageCount = sentMessages.stream()
                    .filter(message -> message instanceof UserMessage && "你好".equals(message.getContent()))
                    .count();
            assertThat(currentUserMessageCount).isEqualTo(1);
        }
    }

    @Test
    void deleteMessage_rejectsForeignConversation() {
        AiMessage message = new AiMessage();
        message.setId(11L);
        message.setConversationId(5L);
        message.setRole("user");
        message.setContent("hi");

        AiConversation conversation = new AiConversation();
        conversation.setId(5L);
        conversation.setUserId(99L);
        conversation.setTitle("标题");
        conversation.setCreateTime(LocalDateTime.now());
        conversation.setUpdateTime(LocalDateTime.now());

        when(messageRepository.findById(11L)).thenReturn(Optional.of(message));
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> chatService.deleteMessage(1L, 11L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("无权访问该会话");

        verify(messageRepository, never()).delete(any());
    }

    @Test
    void deleteMessage_deletesOwnMessageAndCache() {
        AiMessage message = new AiMessage();
        message.setId(11L);
        message.setConversationId(5L);
        message.setRole("user");
        message.setContent("hi");

        AiConversation conversation = new AiConversation();
        conversation.setId(5L);
        conversation.setUserId(1L);
        conversation.setTitle("标题");
        conversation.setCreateTime(LocalDateTime.now());
        conversation.setUpdateTime(LocalDateTime.now());

        when(messageRepository.findById(11L)).thenReturn(Optional.of(message));
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conversation));

        chatService.deleteMessage(1L, 11L);

        verify(messageRepository).delete(message);
        verify(redisTemplate).delete("ai:chat:history:5");
    }

    @Test
    void streamMessage_doesNotRetryAfterPartialOutput() {
        stubConversation(5L, 1L);
        when(messageRepository.findByConversationIdOrderByIdDesc(eq(5L), any())).thenReturn(List.of());
        stubStreaming(Flux.just("A").concatWith(Flux.error(new RuntimeException("Connection reset"))));

        List<String> received = new ArrayList<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        chatService.streamMessage(1L, new MessageRequest(5L, "你好"))
                .doOnNext(received::add)
                .onErrorResume(e -> {
                    errorRef.set(e);
                    return Mono.empty();
                })
                .then()
                .block();

        assertThat(received).containsExactly("A");
        assertThat(errorRef.get()).hasMessageContaining("Connection reset");
        verify(streamResponseSpec, times(1)).content();
    }

    @Test
    void streamMessage_persistsAiMessageWhenRedisWriteFails() {
        stubConversation(5L, 1L);
        when(messageRepository.findByConversationIdOrderByIdDesc(eq(5L), any())).thenReturn(List.of());
        stubStreaming(Flux.just("回答"));

        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RedisConnectionFailureException("redis down"))
                .when(valueOperations).set(anyString(), any(), any(Duration.class));

        List<String> received = chatService.streamMessage(1L, new MessageRequest(5L, "你好"))
                .collectList()
                .block();

        assertThat(received).containsExactly("回答");

        ArgumentCaptor<AiMessage> captor = ArgumentCaptor.forClass(AiMessage.class);
        verify(messageRepository, timeout(2000).atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(m -> "assistant".equals(m.getRole()) && "回答".equals(m.getContent()));
    }

    private void stubConversation(Long conversationId, Long ownerId) {
        AiConversation conversation = new AiConversation();
        conversation.setId(conversationId);
        conversation.setUserId(ownerId);
        conversation.setTitle("新对话");
        conversation.setCreateTime(LocalDateTime.now());
        conversation.setUpdateTime(LocalDateTime.now());
        lenient().when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
    }

    private void stubStreaming(Flux<String> flux) {
        lenient().when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        lenient().when(chatClientRequestSpec.messages(anyList())).thenReturn(chatClientRequestSpec);
        lenient().when(chatClientRequestSpec.stream()).thenReturn(streamResponseSpec);
        lenient().when(streamResponseSpec.content()).thenReturn(flux);
    }
}

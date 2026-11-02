package com.enterprise.asset.ai.controller;

import com.enterprise.asset.ai.dto.BatchDeleteRequest;
import com.enterprise.asset.ai.dto.ConversationResponse;
import com.enterprise.asset.ai.dto.MessageRequest;
import com.enterprise.asset.ai.dto.MessageResponse;
import com.enterprise.asset.ai.entity.AiConversation;
import com.enterprise.asset.ai.exception.BusinessException;
import com.enterprise.asset.ai.service.ChatService;
import com.enterprise.asset.common.dto.UserDTO;
import com.enterprise.asset.common.util.Result;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 知识助手接口：会话管理、消息管理、SSE 流式问答。
 */
@RestController
@RequestMapping("/api/ai/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<Result<List<ConversationResponse>>> getConversations(
            @AuthenticationPrincipal UserDTO user) {
        List<ConversationResponse> conversations = chatService.getConversations(user.getId()).stream()
                .map(this::convertToConversationResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Result.success(conversations));
    }

    @PostMapping("/conversations")
    public ResponseEntity<Result<ConversationResponse>> createConversation(
            @AuthenticationPrincipal UserDTO user,
            @RequestBody(required = false) Map<String, String> request) {
        String title = request != null ? request.get("title") : null;
        AiConversation conversation = chatService.createConversation(user.getId(), title);
        return ResponseEntity.ok(Result.success(convertToConversationResponse(conversation)));
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<Result<ConversationResponse>> getConversation(
            @AuthenticationPrincipal UserDTO user,
            @PathVariable Long conversationId) {
        AiConversation conversation = chatService.getConversation(user.getId(), conversationId);
        return ResponseEntity.ok(Result.success(convertToConversationResponse(conversation)));
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Result<String>> deleteConversation(
            @AuthenticationPrincipal UserDTO user,
            @PathVariable Long conversationId) {
        chatService.deleteConversation(user.getId(), conversationId);
        return ResponseEntity.ok(Result.success("会话删除成功"));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Result<String>> deleteMessage(
            @AuthenticationPrincipal UserDTO user,
            @PathVariable Long messageId) {
        chatService.deleteMessage(user.getId(), messageId);
        return ResponseEntity.ok(Result.success("消息删除成功"));
    }

    @PostMapping("/messages/batch-delete")
    public ResponseEntity<Result<String>> batchDeleteMessages(
            @AuthenticationPrincipal UserDTO user,
            @Valid @RequestBody BatchDeleteRequest request) {
        logger.info("批量删除请求 - 用户ID: {}, 消息数: {}", user.getId(), request.getMessageIds().size());

        List<Long> messageIds = request.getMessageIds().stream()
                .map(this::toMessageId)
                .collect(Collectors.toList());

        chatService.batchDeleteMessages(user.getId(), messageIds);
        return ResponseEntity.ok(Result.success("批量删除成功"));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Result<List<MessageResponse>>> getMessages(
            @AuthenticationPrincipal UserDTO user,
            @PathVariable Long conversationId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(required = false) Integer limit) {
        List<MessageResponse> messages =
                chatService.getMessages(user.getId(), conversationId, beforeId, limit);
        return ResponseEntity.ok(Result.success(messages));
    }

    @PostMapping("/messages")
    public ResponseEntity<Result<MessageResponse>> sendMessage(
            @AuthenticationPrincipal UserDTO user,
            @Valid @RequestBody MessageRequest request) {
        MessageResponse response = chatService.sendMessage(user.getId(), request);
        return ResponseEntity.ok(Result.success(response));
    }

    /**
     * 流式问答：正常内容使用 message 事件，异常使用 error 事件。
     */
    @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamMessage(
            @AuthenticationPrincipal UserDTO user,
            @Valid @RequestBody MessageRequest request) {
        logger.info("streamMessage called, user={}, conversationId={}",
                user != null ? user.getUsername() : null,
                request.getConversationId());

        if (user == null) {
            return Flux.just(ServerSentEvent.<String>builder("用户未登录或Token无效").event("error").build());
        }

        return chatService.streamMessage(user.getId(), request)
                .doOnError(error -> logger.error("streamMessage controller error, user={}, conversationId={}",
                        user.getUsername(), request.getConversationId(), error))
                .map(content -> ServerSentEvent.<String>builder(content).event("message").build())
                .onErrorResume(error -> Flux.just(ServerSentEvent.<String>builder(errorMessage(error))
                        .event("error")
                        .build()));
    }

    private String errorMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? "服务异常，请稍后重试" : message;
    }

    private Long toMessageId(Object id) {
        if (id instanceof Long value) {
            return value;
        }
        if (id instanceof String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new BusinessException("无效的消息ID: " + value);
            }
        }
        if (id instanceof Number value) {
            return value.longValue();
        }
        throw new BusinessException("无效的消息ID格式");
    }

    private ConversationResponse convertToConversationResponse(AiConversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getUserId(),
                conversation.getTitle(),
                conversation.getCreateTime(),
                conversation.getUpdateTime());
    }
}

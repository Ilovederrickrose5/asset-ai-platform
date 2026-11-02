package com.enterprise.asset.ai.dto;

import jakarta.validation.constraints.NotBlank;

public class MessageRequest {

    private Long conversationId;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    public MessageRequest() {
    }

    public MessageRequest(Long conversationId, String content) {
        this.conversationId = conversationId;
        this.content = content;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

package com.enterprise.asset.ai.dto;

import java.time.LocalDateTime;

public class MessageResponse {

    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    private LocalDateTime createTime;

    public MessageResponse() {
    }

    public MessageResponse(Long id, Long conversationId, String role, String content, LocalDateTime createTime) {
        this.id = id;
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.createTime = createTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}

package com.enterprise.asset.ai.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class BatchDeleteRequest {

    @NotEmpty(message = "请选择要删除的消息")
    private List<Object> messageIds;

    public List<Object> getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(List<Object> messageIds) {
        this.messageIds = messageIds;
    }
}

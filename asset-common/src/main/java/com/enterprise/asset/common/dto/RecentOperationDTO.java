package com.enterprise.asset.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecentOperationDTO {
    
    private LocalDateTime time;
    private String timeText;
    private String content;
    private String type;
    private String operator;
    private boolean isPending;
}
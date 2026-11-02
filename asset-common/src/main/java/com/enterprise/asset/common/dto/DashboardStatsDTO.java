package com.enterprise.asset.common.dto;

import lombok.Data;

@Data
public class DashboardStatsDTO {
    
    private Long assetCount;
    private Long pendingCount;
    private Long departmentCount;
    private Long userCount;
    private String departmentName;
}
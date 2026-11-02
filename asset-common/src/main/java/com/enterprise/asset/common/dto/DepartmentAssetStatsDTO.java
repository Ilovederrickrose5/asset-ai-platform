package com.enterprise.asset.common.dto;

import lombok.Data;

@Data
public class DepartmentAssetStatsDTO {
    private Long departmentId;
    private String departmentName;
    private long assetCount;
    private double totalValue;
    private double averageValue;
    private long inUseCount;
    private long idleCount;
    private long maintenanceCount;
    private long scrappedCount;
}
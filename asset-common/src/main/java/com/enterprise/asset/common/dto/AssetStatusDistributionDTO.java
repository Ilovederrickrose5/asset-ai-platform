package com.enterprise.asset.common.dto;

import lombok.Data;

@Data
public class AssetStatusDistributionDTO {
    private String status;
    private String statusName;
    private long count;
    private double percentage;
    private double totalValue;
}
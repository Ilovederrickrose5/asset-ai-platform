package com.enterprise.asset.common.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepreciationSummaryDTO {
    
    private Long categoryId;
    private String categoryName;
    private Long departmentId;
    private String departmentName;
    private Integer assetCount;
    private BigDecimal totalOriginalValue;
    private BigDecimal totalNetValue;
    private BigDecimal totalDepreciation;
    private BigDecimal totalAccumulatedDepreciation;
    private BigDecimal depreciationRate;
}
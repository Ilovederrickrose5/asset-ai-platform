package com.enterprise.asset.common.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AssetNetValueDTO {
    
    private Long assetId;
    private String assetNo;
    private String assetName;
    private String categoryName;
    private String departmentName;
    private String status;
    private BigDecimal originalValue;
    private BigDecimal netValue;
    private BigDecimal accumulatedDepreciation;
    private BigDecimal depreciationRate;
    private LocalDate purchaseDate;
    private Integer usefulLife;
    private Integer usedMonths;
}
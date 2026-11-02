package com.enterprise.asset.common.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DepreciationReportDTO {
    
    private Long assetId;
    private String assetNo;
    private String assetName;
    private String categoryName;
    private String departmentName;
    private String depreciationMethod;
    private BigDecimal originalValue;
    private BigDecimal currentNetValue;
    private BigDecimal depreciationAmount;
    private BigDecimal accumulatedDepreciation;
    private Integer usefulLife;
    private Integer usedMonths;
    private String depreciationPeriod;
    private LocalDate calculationDate;
    private String status;
}
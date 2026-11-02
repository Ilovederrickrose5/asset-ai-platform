package com.enterprise.asset.business.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset_depreciation")
@Data
public class DepreciationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "asset_no", nullable = false)
    private String assetNo;

    @Column(name = "asset_name", nullable = false)
    private String assetName;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "depreciation_method", nullable = false)
    private String depreciationMethod;

    @Column(name = "original_value", nullable = false)
    private BigDecimal originalValue;

    @Column(name = "current_net_value", nullable = false)
    private BigDecimal currentNetValue;

    @Column(name = "depreciation_amount", nullable = false)
    private BigDecimal depreciationAmount;

    @Column(name = "accumulated_depreciation", nullable = false)
    private BigDecimal accumulatedDepreciation;

    @Column(name = "useful_life", nullable = false)
    private Integer usefulLife;

    @Column(name = "used_months", nullable = false)
    private Integer usedMonths;

    @Column(name = "depreciation_period", nullable = false)
    private String depreciationPeriod;

    @Column(name = "depreciation_month", nullable = false)
    private String depreciationMonth;

    @Column(name = "net_value", nullable = false)
    private BigDecimal netValue;

    @Column(name = "work_hours")
    private BigDecimal workHours;

    @Column(name = "work_units")
    private BigDecimal workUnits;

    @Column(name = "total_work_hours")
    private BigDecimal totalWorkHours;

    @Column(name = "total_work_units")
    private BigDecimal totalWorkUnits;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "remark")
    private String remark;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_name")
    private String operatorName;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Transient
    public LocalDate getCalculationDate() {
        return createTime != null ? createTime.toLocalDate() : LocalDate.now();
    }
}
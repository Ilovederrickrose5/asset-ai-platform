package com.enterprise.asset.business.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset_application")
@Data
public class AssetApplication {

    public static final String APPLICATION_DATE = "applicationDate";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id")
    private Long assetId;

    @Column(name = "asset_no")
    private String assetNo;

    @Column(name = "asset_name")
    private String assetName;

    @Column(name = "application_type", nullable = false)
    private String applicationType;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "applicant_name", nullable = false)
    private String applicantName;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "department_name")
    private String departmentName;

    @Column(name = "application_reason", columnDefinition = "TEXT")
    private String applicationReason;

    @Column(name = "application_date", nullable = false)
    private LocalDateTime applicationDate;

    @Column(name = "status")
    private String status;

    @Column(name = "approver_id")
    private Long approverId;

    @Column(name = "approver_name")
    private String approverName;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Column(name = "approval_remark", columnDefinition = "TEXT")
    private String approvalRemark;

    @Column(name = "maintenance_type")
    private String maintenanceType;

    @Column(name = "maintenance_start_time")
    private LocalDateTime maintenanceStartTime;

    @Column(name = "maintenance_end_time")
    private LocalDateTime maintenanceEndTime;

    @Column(name = "maintenance_cost")
    private BigDecimal maintenanceCost;

    @Column(name = "maintenance_remark", columnDefinition = "TEXT")
    private String maintenanceRemark;

    @Column(name = "previous_asset_status")
    private String previousAssetStatus;

    @Column(name = "disposal_type")
    private String disposalType;

    @Column(name = "estimated_value")
    private BigDecimal estimatedValue;

    @Column(name = "disposal_time")
    private LocalDateTime disposalTime;

    @Column(name = "transferor_id")
    private Long transferorId;

    @Column(name = "transferor_name")
    private String transferorName;

    @Column(name = "transferor_dept_id")
    private Long transferorDeptId;

    @Column(name = "transferor_dept_name")
    private String transferorDeptName;

    @Column(name = "transferee_id")
    private Long transfereeId;

    @Column(name = "transferee_name")
    private String transfereeName;

    @Column(name = "transferee_dept_id")
    private Long transfereeDeptId;

    @Column(name = "transferee_dept_name")
    private String transfereeDeptName;

    @Column(name = "transfer_reason", columnDefinition = "TEXT")
    private String transferReason;

    @Column(name = "original_application_id")
    private Long originalApplicationId;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "specification")
    private String specification;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit")
    private String unit;

    @Column(name = "estimated_unit_price")
    private BigDecimal estimatedUnitPrice;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
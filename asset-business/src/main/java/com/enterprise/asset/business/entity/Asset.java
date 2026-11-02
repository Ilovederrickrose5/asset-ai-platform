package com.enterprise.asset.business.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset")
@Data
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_no", unique = true, nullable = false)
    private String assetNo;

    @Column(name = "asset_name", nullable = false)
    private String assetName;

    @Column(name = "category_id")
    private Long categoryId;

    private String model;

    private String unit;

    @Column(name = "purchase_price")
    private BigDecimal purchasePrice;

    @Column(name = "net_value")
    private BigDecimal netValue;

    @Column(name = "original_value")
    private BigDecimal originalValue;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "warranty_period")
    private Integer warrantyPeriod;

    @Column(name = "useful_life")
    private Integer usefulLife;

    @Column(name = "depreciation_method")
    private String depreciationMethod;

    @Column(name = "status")
    private String status;

    @Column(name = "use_status")
    private String useStatus;

    @Column(name = "custodian_id")
    private Long custodianId;

    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "location")
    private String location;

    @Column(name = "remark")
    private String remark;

    @Column(name = "user_id")
    private Long userId;

    // 借出相关字段
    @Column(name = "borrow_status")
    private String borrowStatus; // not_borrowed: 未借出, borrowed: 已借出

    @Column(name = "current_location")
    private String currentLocation; // 当前实际位置（借出后的位置）

    @Column(name = "borrower_id")
    private Long borrowerId; // 借用人ID

    @Column(name = "borrow_time")
    private LocalDateTime borrowTime; // 借出时间

    @Column(name = "expected_return_time")
    private LocalDateTime expectedReturnTime; // 预计归还时间

    /** 乐观锁版本号: 防止并发审批时资产状态被相互覆盖 */
    @Version
    @Column(name = "version")
    private Long version;

    /** 当前未决申请ID: 原子占用,保证同一资产同时只挂一个未决申请 */
    @Column(name = "current_application_id")
    private Long currentApplicationId;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
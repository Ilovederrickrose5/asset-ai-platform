package com.enterprise.asset.business.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "asset_inventory_detail")
public class AssetInventoryDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "asset_no", nullable = false)
    private String assetNo;

    @Column(name = "asset_name")
    private String assetName;

    @Column(name = "expected_status")
    private String expectedStatus;

    @Column(name = "actual_status")
    private String actualStatus;

    @Column(name = "inventory_result")
    private String inventoryResult;

    @Column(name = "remark")
    private String remark;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "actual_quantity", nullable = false)
    private Integer actualQuantity;

    @Column(name = "difference_quantity", nullable = false)
    private Integer differenceQuantity;

    @Column(name = "inventory_person_id", nullable = false)
    private Long inventoryPersonId;

    @Column(name = "inventory_person_name")
    private String inventoryPersonName;

    @Column(name = "status")
    private String status;

    @Column(name = "system_quantity", nullable = false)
    private Integer systemQuantity;

    @Column(name = "difference_value")
    private Double differenceValue;
}
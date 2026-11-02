package com.enterprise.asset.business.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "asset_inventory")
public class AssetInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_no", nullable = false, unique = true)
    private String inventoryNo;

    @Column(name = "inventory_name", nullable = false)
    private String inventoryName;

    @Column(name = "inventory_date")
    private LocalDate inventoryDate;

    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "status")
    private String status; // pending, in_progress, completed, cancelled

    @Column(name = "total_count")
    private Integer totalCount;

    @Column(name = "actual_count")
    private Integer actualCount;

    @Column(name = "surplus_count")
    private Integer surplusCount;

    @Column(name = "shortage_count")
    private Integer shortageCount;

    @Column(name = "remark")
    private String remark;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "actual_completion_time")
    private LocalDateTime actualCompletionTime;
    // 被分配人ID
    @Column(name = "assignee_id")
    private Long assigneeId;
    // 被分配人姓名
    @Column(name = "assignee_name")
    private String assigneeName;

    @Column(name = "complete_time")
    private LocalDateTime completeTime;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "creator_name")
    private String creatorName;

    @Column(name = "expected_completion_time")
    private LocalDateTime expectedCompletionTime;

    @Column(name = "inventory_area")
    private String inventoryArea;

    @Column(name = "inventory_scope", nullable = false)
    private String inventoryScope;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        if (this.status == null) {
            this.status = "pending";
        }
    }
}
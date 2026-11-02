package com.enterprise.asset.business.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_request")
@Data
public class PurchaseRequest {

    public static final String APPLICATION_DATE = "applicationDate";

    @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "item_name", nullable = false)
  private String itemName;

  @Column(name = "specification")
  private String specification;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @Column(name = "unit")
  private String unit;

  @Column(name = "estimated_unit_price", precision = 10, scale = 2)
  private BigDecimal estimatedUnitPrice;

  @Column(name = "total_amount", precision = 12, scale = 2)
  private BigDecimal totalAmount;

  @Column(name = "purchase_reason", columnDefinition = "TEXT")
  private String purchaseReason;

  @Column(name = "department_id")
  private Long departmentId;

  @Column(name = "department_name")
  private String departmentName;

  @Column(name = "applicant_id", nullable = false)
  private Long applicantId;

  @Column(name = "applicant_name", nullable = false)
  private String applicantName;

  @Column(name = "application_date", nullable = false)
  private LocalDateTime applicationDate;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "approval_date")
  private LocalDateTime approvalDate;

  @Column(name = "approver_id")
  private Long approverId;

  @Column(name = "approver_name")
  private String approverName;

  @Column(name = "approval_remark", columnDefinition = "TEXT")
  private String approvalRemark;

  @CreationTimestamp
  @Column(name = "create_time", updatable = false)
  private LocalDateTime createTime;

  @UpdateTimestamp
  @Column(name = "update_time")
  private LocalDateTime updateTime;
}
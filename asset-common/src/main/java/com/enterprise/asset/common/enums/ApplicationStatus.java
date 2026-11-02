package com.enterprise.asset.common.enums;

/**
 * 申请状态枚举
 */
public enum ApplicationStatus {

  PENDING("pending", "待审批"),
  PENDING_LEADER("pending_leader", "待领导审批"),
  LEADER_APPROVED("leader_approved", "领导已批准"),
  APPROVED("approved", "已批准"),
  REJECTED("rejected", "已拒绝"),
  IN_PROGRESS("in_progress", "进行中"),
  COMPLETED("completed", "已完成");

  private final String code;
  private final String name;

  ApplicationStatus(String code, String name) {
    this.code = code;
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public static ApplicationStatus fromCode(String code) {
    if (code == null) {
      return null;
    }
    for (ApplicationStatus status : values()) {
      if (status.code.equals(code)) {
        return status;
      }
    }
    return null;
  }

  public boolean isPending() {
    return this == PENDING || this == PENDING_LEADER;
  }

  public boolean isFinalized() {
    return this == APPROVED || this == REJECTED;
  }
}
package com.enterprise.asset.common.enums;

/**
 * 资产状态枚举
 */
public enum AssetStatus {

  IN_STOCK("in_stock", "在库"),
  USING("using", "使用中"),
  MAINTENANCE("maintenance", "维修中"),
  SCRAPPED("scrapped", "已报废"),
  DISPOSED("disposed", "已报废"),
  IDLE("idle", "闲置");

  private final String code;
  private final String name;

  AssetStatus(String code, String name) {
    this.code = code;
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public static AssetStatus fromCode(String code) {
    if (code == null) {
      return null;
    }
    for (AssetStatus status : values()) {
      if (status.code.equals(code)) {
        return status;
      }
    }
    return null;
  }

  public boolean isActive() {
    return this == IN_STOCK || this == USING || this == MAINTENANCE || this == IDLE;
  }

  public boolean isScrapped() {
    return this == SCRAPPED || this == DISPOSED;
  }
}
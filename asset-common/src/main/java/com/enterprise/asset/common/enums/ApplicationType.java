package com.enterprise.asset.common.enums;

/**
 * 申请类型枚举
 */
public enum ApplicationType {

    RECEIVE("RECEIVE", "领用"),
    TRANSFER("TRANSFER", "调拨"),
    DISPOSAL("DISPOSAL", "报废"),
    MAINTENANCE("MAINTENANCE", "维修"),
    REPAIR("REPAIR", "修理");

    private final String code;
    private final String name;

    ApplicationType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static ApplicationType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ApplicationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
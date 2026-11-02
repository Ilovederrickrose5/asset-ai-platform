package com.enterprise.asset.common.enums;

public enum LogType {
    SYSTEM("SYSTEM", "系统"),
    ASSET("ASSET", "资产"),
    INVENTORY("INVENTORY", "盘点"),
    PURCHASE("PURCHASE", "采购"),
    DEPRECIATION("DEPRECIATION", "折旧"),
    APPLICATION("APPLICATION", "申请");

    private final String code;
    private final String name;

    LogType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static LogType fromCode(String code) {
        if (code == null) {
            return SYSTEM;
        }
        for (LogType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return SYSTEM;
    }
}
package com.enterprise.asset.common.enums;

public enum InventoryStatus {
    PENDING("pending", "待处理"),
    IN_PROGRESS("in_progress", "进行中"),
    COMPLETED("completed", "已完成"),
    SURPLUS("surplus", "盘盈"),
    SHORTAGE("shortage", "盘亏"),
    NORMAL("normal", "正常");

    private final String code;
    private final String name;

    InventoryStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static InventoryStatus fromCode(String code) {
        if (code == null) {
            return PENDING;
        }
        for (InventoryStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return PENDING;
    }
}
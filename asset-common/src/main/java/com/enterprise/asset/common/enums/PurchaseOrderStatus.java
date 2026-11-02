package com.enterprise.asset.common.enums;

public enum PurchaseOrderStatus {

    PENDING("pending", "待处理"),
    ORDERED("ordered", "已下单"),
    DELIVERED("delivered", "已到货"),
    COMPLETED("completed", "已完成");

    private final String code;
    private final String name;

    PurchaseOrderStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static PurchaseOrderStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (PurchaseOrderStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }
}
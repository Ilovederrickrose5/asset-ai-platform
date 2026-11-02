package com.enterprise.asset.common.enums;

public enum PurchaseRequestStatus {

    PENDING("pending", "待审批"),
    APPROVED("approved", "已批准"),
    REJECTED("rejected", "已拒绝"),
    ORDERED("ordered", "已下单");

    private final String code;
    private final String name;

    PurchaseRequestStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static PurchaseRequestStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (PurchaseRequestStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }
}
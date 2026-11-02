package com.enterprise.asset.common.enums;

public enum DepreciationMethod {

    STRAIGHT_LINE("STRAIGHT_LINE", "直线法"),
    DOUBLE_DECLINING_BALANCE("DOUBLE_DECLINING_BALANCE", "双倍余额递减法"),
    WORK_UNIT("WORK_UNIT", "工作量法"),
    SUM_OF_YEARS("SUM_OF_YEARS", "年数总和法");

    private final String code;
    private final String name;

    DepreciationMethod(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static DepreciationMethod fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DepreciationMethod method : values()) {
            if (method.code.equalsIgnoreCase(code)) {
                return method;
            }
        }
        return STRAIGHT_LINE;
    }
}
package com.enterprise.asset.common.enums;

public enum UserRole {

    ADMIN("admin", "管理员"),
    LEADER("leader", "部门领导"),
    MANAGER("manager", "资产管理员"),
    ASSET_MANAGER("asset_manager", "部门资产管理员"),
    DEPT_MANAGER("dept_manager", "部门管理员"),
    USER("user", "普通用户");

    private final String code;
    private final String name;

    UserRole(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static UserRole fromCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.toUpperCase().replace("ROLE_", "");
        for (UserRole role : values()) {
            if (role.code.equalsIgnoreCase(code) || role.name().equals(normalizedCode)) {
                return role;
            }
        }
        return null;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isLeader() {
        return this == LEADER;
    }

    public boolean isManager() {
        return this == MANAGER || this == ASSET_MANAGER || this == DEPT_MANAGER;
    }

    public boolean isUser() {
        return this == USER;
    }
}
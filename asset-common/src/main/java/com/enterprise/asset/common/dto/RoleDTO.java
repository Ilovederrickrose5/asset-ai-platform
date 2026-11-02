package com.enterprise.asset.common.dto;

import lombok.Data;

/**
 * 角色信息DTO - 用于Feign调用传输角色数据
 */
@Data
public class RoleDTO {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer status;
}
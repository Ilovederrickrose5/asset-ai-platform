package com.enterprise.asset.common.dto;

import lombok.Data;

/**
 * 部门信息DTO - 用于Feign调用传输部门数据
 */
@Data
public class DepartmentDTO {
    private Long id;
    private String deptName;
    private String deptCode;
    private Long parentId;
    private Long leader;
    private String description;
    private Integer status;
}
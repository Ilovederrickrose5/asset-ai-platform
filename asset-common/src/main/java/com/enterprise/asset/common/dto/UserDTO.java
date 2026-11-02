package com.enterprise.asset.common.dto;

import lombok.Data;
import java.util.List;

/**
 * 用户信息DTO - 用于Feign调用传输用户数据
 */
@Data
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String realName;
    private String employeeNo;
    private Long deptId;
    private String position;
    private Integer status;
    private List<String> roleCodes;
    private List<String> roleNames;
}
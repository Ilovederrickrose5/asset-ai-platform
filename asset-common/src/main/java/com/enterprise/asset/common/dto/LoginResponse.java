package com.enterprise.asset.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LoginResponse {
    
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String realName;
    private Long departmentId;
    private String departmentName;
    private List<String> roles;
    
    public LoginResponse(String token, Long id, String username, String email, String realName, Long departmentId, String departmentName, List<String> roles) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.realName = realName;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.roles = roles;
    }
}
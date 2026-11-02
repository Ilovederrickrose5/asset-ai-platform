package com.enterprise.asset.business.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "user")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String phone;

    @Column(name = "real_name")
    private String realName;

    @Column(name = "employee_no")
    private String employeeNo;

    @Column(name = "dept_id")
    private Long deptId;

    private String position;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /** 用户角色关联(一对多) */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<UserRole> userRoles = new ArrayList<>();

    /** 获取用户所有角色编码 */
    public List<String> getRoleCodes() {
        if (userRoles == null || userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        return userRoles.stream()
                .map(userRole -> userRole.getRole().getCode())
                .collect(Collectors.toList());
    }

    /** 获取用户所有角色名称 */
    public List<String> getRoleNames() {
        if (userRoles == null || userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        return userRoles.stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toList());
    }

    /**
     * 兼容旧代码的角色获取方法
     * 返回第一个角色的小写形式,如果没有角色返回null
     * 注意:此方法仅用于兼容旧代码,新代码应使用getRoleCodes()
     */
    public String getRole() {
        List<String> roleCodes = getRoleCodes();
        if (roleCodes == null || roleCodes.isEmpty()) {
            return null;
        }
        return roleCodes.get(0).toLowerCase();
    }

    /**
     * 兼容旧代码的角色设置方法
     * 注意:此方法仅用于兼容旧代码,不推荐使用
     */
    public void setRole(String role) {
        // 保留空实现以兼容旧代码
        // 实际角色应通过UserRoleRepository管理
    }
}
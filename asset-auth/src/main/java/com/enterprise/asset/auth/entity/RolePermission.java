package com.enterprise.asset.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 角色权限关联实体
 * 关联表: role_permission(角色权限关联表)
 * 用于实现角色与权限的多对多关系
 */
@Entity
@Table(name = "role_permission")
@Data
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /** 关联角色实体 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private Role role;

    /** 关联权限实体 */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "permission_id", insertable = false, updatable = false)
    private Permission permission;
}
package com.enterprise.asset.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 权限实体
 * 关联表: permission(权限表)
 * 用于实现细粒度权限控制
 */
@Entity
@Table(name = "permission")
@Data
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 权限编码（如：asset:view） */
    @Column(unique = true, nullable = false, length = 100)
    private String code;

    /** 权限名称（如：查看资产） */
    @Column(nullable = false, length = 100)
    private String name;

    /** 权限描述 */
    @Column(length = 500)
    private String description;

    /** 父权限ID（用于权限层级） */
    @Column(name = "parent_id")
    private Long parentId = 0L;

    /** 排序号 */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /** 状态：1启用，0禁用 */
    @Column(nullable = false)
    private Integer status = 1;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
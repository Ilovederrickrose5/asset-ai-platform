package com.enterprise.asset.auth.repository;

import com.enterprise.asset.auth.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 角色权限关联数据访问接口
 * 关联表: role_permission(角色权限关联表)
 * 主要操作: 角色权限关系的增删改查
 */
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    /** 根据角色ID查询权限列表 */
    List<RolePermission> findByRoleId(Long roleId);

    /** 根据权限ID查询角色列表 */
    List<RolePermission> findByPermissionId(Long permissionId);

    /** 根据角色ID删除所有权限关联 */
    void deleteByRoleId(Long roleId);

    /** 根据权限ID删除所有角色关联 */
    void deleteByPermissionId(Long permissionId);

    /** 判断角色是否拥有某个权限 */
    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);
}
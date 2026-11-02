package com.enterprise.asset.auth.service;

import com.enterprise.asset.auth.entity.RolePermission;
import com.enterprise.asset.auth.repository.RolePermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色权限服务
 * 提供角色与权限关联管理的业务逻辑
 */
@Service
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;

    public RolePermissionService(RolePermissionRepository rolePermissionRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
    }

    /** 为角色分配权限 */
    @Transactional
    public void assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        // 先删除该角色的所有权限
        rolePermissionRepository.deleteByRoleId(roleId);
        
        // 再添加新的权限关联
        for (Long permissionId : permissionIds) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permissionId);
            rolePermissionRepository.save(rolePermission);
        }
    }

    /** 为角色添加单个权限 */
    @Transactional
    public void addPermissionToRole(Long roleId, Long permissionId) {
        if (!rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permissionId);
            rolePermissionRepository.save(rolePermission);
        }
    }

    /** 从角色移除权限 */
    @Transactional
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(roleId);
        rolePermissions.stream()
                .filter(rp -> rp.getPermissionId().equals(permissionId))
                .findFirst()
                .ifPresent(rp -> rolePermissionRepository.delete(rp));
    }

    /** 检查角色是否拥有权限 */
    public boolean hasPermission(Long roleId, Long permissionId) {
        return rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId);
    }

    /** 根据角色ID获取所有权限关联 */
    public List<RolePermission> getRolePermissions(Long roleId) {
        return rolePermissionRepository.findByRoleId(roleId);
    }
}
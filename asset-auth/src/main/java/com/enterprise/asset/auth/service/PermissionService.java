package com.enterprise.asset.auth.service;

import com.enterprise.asset.auth.entity.Permission;
import com.enterprise.asset.auth.entity.RolePermission;
import com.enterprise.asset.auth.repository.PermissionRepository;
import com.enterprise.asset.auth.repository.RolePermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限服务
 * 提供权限管理相关业务逻辑
 */
@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public PermissionService(PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository) {
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    /** 获取所有权限 */
    public List<Permission> getAllPermissions() {
        return permissionRepository.findByStatus(1);
    }

    /** 根据权限编码获取权限 */
    public Permission getPermissionByCode(String code) {
        return permissionRepository.findByCode(code).orElse(null);
    }

    /** 根据角色ID获取权限列表 */
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(roleId);
        return rolePermissions.stream()
                .map(RolePermission::getPermission)
                .collect(Collectors.toList());
    }

    /** 创建权限 */
    @Transactional
    public Permission createPermission(Permission permission) {
        return permissionRepository.save(permission);
    }

    /** 更新权限 */
    @Transactional
    public Permission updatePermission(Long id, Permission permission) {
        Permission existing = permissionRepository.findById(id).orElseThrow();
        existing.setCode(permission.getCode());
        existing.setName(permission.getName());
        existing.setDescription(permission.getDescription());
        existing.setParentId(permission.getParentId());
        existing.setSortOrder(permission.getSortOrder());
        existing.setStatus(permission.getStatus());
        return permissionRepository.save(existing);
    }

    /** 删除权限 */
    @Transactional
    public void deletePermission(Long id) {
        permissionRepository.deleteById(id);
    }

    /** 批量创建权限 */
    @Transactional
    public List<Permission> createPermissions(List<Permission> permissions) {
        return permissionRepository.saveAll(permissions);
    }
}
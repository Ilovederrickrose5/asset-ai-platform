package com.enterprise.asset.auth.service;

import com.enterprise.asset.auth.entity.UserRole;
import com.enterprise.asset.auth.repository.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户角色服务
 * 提供用户与角色关联管理的业务逻辑
 */
@Service
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;

    public UserRoleService(UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }

    /** 为用户分配角色 */
    @Transactional
    public void assignRolesToUser(Long userId, List<Long> roleIds) {
        // 先删除该用户的所有角色
        userRoleRepository.deleteByUserId(userId);
        
        // 再添加新的角色关联
        for (Long roleId : roleIds) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleRepository.save(userRole);
        }
    }

    /** 为用户添加单个角色 */
    @Transactional
    public void addRoleToUser(Long userId, Long roleId) {
        if (!userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleRepository.save(userRole);
        }
    }

    /** 从用户移除角色 */
    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        userRoles.stream()
                .filter(ur -> ur.getRoleId().equals(roleId))
                .findFirst()
                .ifPresent(ur -> userRoleRepository.delete(ur));
    }

    /** 检查用户是否拥有角色 */
    public boolean hasRole(Long userId, Long roleId) {
        return userRoleRepository.existsByUserIdAndRoleId(userId, roleId);
    }

    /** 根据用户ID获取所有角色关联 */
    public List<UserRole> getUserRoles(Long userId) {
        return userRoleRepository.findByUserId(userId);
    }
}
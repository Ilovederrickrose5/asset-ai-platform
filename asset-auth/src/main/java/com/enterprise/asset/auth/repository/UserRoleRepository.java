package com.enterprise.asset.auth.repository;

import com.enterprise.asset.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 用户角色关联数据访问接口
 * 关联表: user_role(用户角色关联表)
 * 主要操作: 用户角色关系的增删改查
 */
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    /** 根据用户ID查询角色列表 */
    List<UserRole> findByUserId(Long userId);

    /** 根据角色ID查询用户列表 */
    List<UserRole> findByRoleId(Long roleId);

    /** 根据用户ID删除所有角色关联 */
    void deleteByUserId(Long userId);

    /** 根据角色ID删除所有用户关联 */
    void deleteByRoleId(Long roleId);

    /** 判断用户是否拥有某个角色 */
    boolean existsByUserIdAndRoleId(Long userId, Long roleId);
}
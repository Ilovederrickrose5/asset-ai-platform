package com.enterprise.asset.auth.repository;

import com.enterprise.asset.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 角色数据访问接口
 * 关联表: role(角色表)
 * 主要操作: 角色的增删改查、角色编码查询
 */
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /** 根据角色编码查询角色 */
    Optional<Role> findByCode(String code);
    
    /** 判断角色编码是否存在 */
    boolean existsByCode(String code);
}
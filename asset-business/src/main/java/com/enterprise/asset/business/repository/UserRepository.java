package com.enterprise.asset.business.repository;

import com.enterprise.asset.business.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * 用户数据访问接口
 * 关联表: user(用户表)
 * 主要操作: 用户的增删改查、登录认证查询
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** 根据用户名查询用户(登录认证用) */
    Optional<User> findByUsername(String username);

    /** 判断用户名是否存在 */
    boolean existsByUsername(String username);

    /** 根据邮箱查询用户 */
    Optional<User> findByEmail(String email);

    /** 判断邮箱是否存在 */
    boolean existsByEmail(String email);

    /** 根据手机号查询用户 */
    Optional<User> findByPhone(String phone);

    /** 判断手机号是否存在 */
    boolean existsByPhone(String phone);

    /** 根据部门ID统计用户数量 */
    long countByDeptId(Long deptId);

    /** 统计活跃用户数量 */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 1")
    long countActiveUsers();
}
package com.enterprise.asset.auth.service;

import com.enterprise.asset.auth.entity.Role;
import com.enterprise.asset.auth.entity.User;
import com.enterprise.asset.auth.entity.UserRole;
import com.enterprise.asset.auth.repository.RoleRepository;
import com.enterprise.asset.auth.repository.UserRepository;
import com.enterprise.asset.auth.repository.UserRoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 用户服务 - 处理用户CRUD操作与权限管理 */
@Service
public class UserService {

    private static final String DEFAULT_ROLE_CODE = "user";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
            UserRoleRepository userRoleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** 获取所有用户列表 */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /** 获取活跃用户列表（status=1） */
    public List<User> getActiveUsers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getStatus() == 1)
                .toList();
    }

    /** 根据ID获取用户 */
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    /** 根据用户名获取用户 */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    /** 根据部门ID获取用户列表 */
    public List<User> getUsersByDepartment(Long departmentId) {
        return userRepository.findByDeptId(departmentId);
    }

    /**
     * 创建用户
     * 业务规则：用户名/邮箱/手机号唯一校验 → 角色默认user → 密码加密存储
     * 事务处理：@Transactional保证数据一致性
     */
    @Transactional
    public User createUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }

        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                throw new RuntimeException("邮箱已被使用");
            }
        }

        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            if (userRepository.findByPhone(user.getPhone()).isPresent()) {
                throw new RuntimeException("手机号已被使用");
            }
        }

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        String requestedRole = user.getRole();
        User savedUser = userRepository.save(user);
        applyRole(savedUser, requestedRole);
        return savedUser;
    }

    /**
     * 更新用户信息
     * 业务规则：更新基本信息，密码需重新加密，角色转换为小写
     * 事务处理：@Transactional保证数据一致性
     */
    @Transactional
    public User updateUser(Long id, User user) {
        User existingUser = userRepository.findById(id).orElse(null);
        if (existingUser == null) {
            return null;
        }

        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setRealName(user.getRealName());
        existingUser.setEmployeeNo(user.getEmployeeNo());
        existingUser.setDeptId(user.getDeptId());
        existingUser.setPosition(user.getPosition());
        existingUser.setStatus(user.getStatus());

        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        User savedUser = userRepository.save(existingUser);

        String requestedRole = user.getRole();
        if (requestedRole != null && !requestedRole.isBlank()) {
            applyRole(savedUser, requestedRole);
        }

        return savedUser;
    }

    /**
     * 删除用户
     * 事务处理：@Transactional保证数据一致性
     */
    @Transactional
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * 更新用户角色
     * 业务规则：支持角色ID（数字）或角色代码（字符串），自动转换为小写角色代码
     * 事务处理：@Transactional保证数据一致性
     */
    @Transactional
    public User updateRole(Long userId, String roleIdStr) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        if (roleIdStr == null || roleIdStr.isBlank()) {
            throw new RuntimeException("角色不能为空");
        }

        Role role;
        try {
            Long roleId = Long.parseLong(roleIdStr.trim());
            role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("角色不存在: " + roleIdStr));
        } catch (NumberFormatException e) {
            role = resolveRole(roleIdStr);
        }

        applyRole(user, role.getCode());
        return user;
    }

    /**
     * 将角色写入 user_role 关联表（先清空旧角色），并同步内存中的角色列表供响应序列化。
     */
    private void applyRole(User user, String roleCode) {
        Role role = resolveRole(roleCode == null || roleCode.isBlank() ? DEFAULT_ROLE_CODE : roleCode);

        userRoleRepository.deleteByUserId(user.getId());

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRole.setRole(role);
        userRoleRepository.save(userRole);

        user.getUserRoles().clear();
        user.getUserRoles().add(userRole);
        user.setRole(role.getCode());
    }

    private Role resolveRole(String roleCode) {
        String normalized = roleCode.trim().toLowerCase();
        return roleRepository.findByCode(normalized)
                .orElseThrow(() -> new RuntimeException("角色不存在: " + roleCode));
    }

    /** 获取活跃用户数量 */
    public long getActiveUserCount() {
        return userRepository.countActiveUsers();
    }

    /** 获取指定部门的用户数量 */
    public long getUserCountByDepartment(Long departmentId) {
        return userRepository.countByDeptId(departmentId);
    }
}
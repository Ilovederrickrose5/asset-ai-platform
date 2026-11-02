package com.enterprise.asset.auth.service.impl;

import com.enterprise.asset.auth.entity.Role;
import com.enterprise.asset.auth.repository.RoleRepository;
import com.enterprise.asset.auth.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 角色服务实现 - 处理角色CRUD操作 */
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public Role getRoleById(Long id) {
        return roleRepository.findById(id).orElse(null);
    }

    @Override
    public Role getRoleByCode(String code) {
        return roleRepository.findByCode(code).orElse(null);
    }

    /**
     * 创建角色
     * 事务处理：@Transactional保证数据一致性
     */
    @Override
    @Transactional
    public Role createRole(Role role) {
        return roleRepository.save(role);
    }

    /**
     * 更新角色信息
     * 业务流程：查询角色→更新字段→保存
     * 事务处理：@Transactional保证数据一致性
     */
    @Override
    @Transactional
    public Role updateRole(Long id, Role role) {
        Role existingRole = roleRepository.findById(id).orElse(null);
        if (existingRole == null) {
            return null;
        }
        existingRole.setName(role.getName());
        existingRole.setCode(role.getCode());
        existingRole.setDescription(role.getDescription());
        existingRole.setStatus(role.getStatus());
        return roleRepository.save(existingRole);
    }

    /**
     * 删除角色
     * 事务处理：@Transactional保证数据一致性
     */
    @Override
    @Transactional
    public boolean deleteRole(Long id) {
        if (roleRepository.existsById(id)) {
            roleRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
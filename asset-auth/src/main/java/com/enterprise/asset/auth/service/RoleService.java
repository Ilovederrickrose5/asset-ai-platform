/** 角色服务接口 - 处理角色CRUD操作 */
package com.enterprise.asset.auth.service;

import com.enterprise.asset.auth.entity.Role;

import java.util.List;

public interface RoleService {
    
    /** 获取所有角色列表 */
    List<Role> getAllRoles();
    
    /** 根据ID获取角色 */
    Role getRoleById(Long id);
    
    /** 根据角色代码获取角色（如admin、manager、leader、user） */
    Role getRoleByCode(String code);
    
    /** 创建角色 */
    Role createRole(Role role);
    
    /** 更新角色信息 */
    Role updateRole(Long id, Role role);
    
    /** 删除角色 */
    boolean deleteRole(Long id);
}
package com.enterprise.asset.auth.config;

import com.enterprise.asset.auth.entity.Role;
import com.enterprise.asset.auth.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

  private final RoleRepository roleRepository;

  public DataInitializer(RoleRepository roleRepository) {
    this.roleRepository = roleRepository;
  }

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    initRoles();
  }

  private void initRoles() {
    createRoleIfNotExists("admin", "系统管理员", "系统管理员，拥有所有权限");
    createRoleIfNotExists("leader", "领导", "领导，可查看统计报表和审批重大资产处置");
    createRoleIfNotExists("manager", "部门资产管理员", "部门资产管理员，管理本部门资产");
    createRoleIfNotExists("user", "普通员工", "普通员工，可发起各类申请");
  }

  private void createRoleIfNotExists(String code, String name, String description) {
    if (!roleRepository.existsByCode(code)) {
      Role role = new Role();
      role.setCode(code);
      role.setName(name);
      role.setDescription(description);
      roleRepository.save(role);
    }
  }
}
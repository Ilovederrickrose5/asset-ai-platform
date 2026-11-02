package com.enterprise.asset.auth.service;

import com.enterprise.asset.common.dto.LoginRequest;
import com.enterprise.asset.common.dto.LoginResponse;
import com.enterprise.asset.auth.entity.Department;
import com.enterprise.asset.auth.entity.User;
import com.enterprise.asset.auth.repository.DepartmentRepository;
import com.enterprise.asset.auth.repository.UserRepository;
import com.enterprise.asset.common.security.JwtUtil;
import com.enterprise.asset.auth.security.UserDetailsImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 认证服务 - 处理用户登录认证与JWT生成 */
@Service
public class AuthService {

        private final AuthenticationManager authenticationManager;
        private final JwtUtil jwtUtil;
        private final DepartmentRepository departmentRepository;
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;

        public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                DepartmentRepository departmentRepository, UserRepository userRepository,
                PasswordEncoder passwordEncoder) {
            this.authenticationManager = authenticationManager;
            this.jwtUtil = jwtUtil;
            this.departmentRepository = departmentRepository;
            this.userRepository = userRepository;
            this.passwordEncoder = passwordEncoder;
        }

        /**
         * 用户登录认证
         * 业务流程：用户名密码验证 → 获取用户详情 → 生成JWT令牌 → 查询部门名称 → 返回登录响应
         * 外部调用：AuthenticationManager认证、JwtUtil生成令牌、DepartmentRepository查询部门
         * @param loginRequest 登录请求（用户名、密码）
         * @return 登录响应（包含JWT、用户信息、角色列表）
         */
        @Transactional
        public LoginResponse login(LoginRequest loginRequest) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
                                                loginRequest.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

                upgradeStoredPassword(loginRequest.getUsername(), loginRequest.getPassword(), userDetails);

                Map<String, Object> claims = new HashMap<>();
                claims.put("uid", userDetails.getId());
                if (userDetails.getDepartmentId() != null) {
                        claims.put("deptId", userDetails.getDepartmentId());
                }
                if (userDetails.getRealName() != null) {
                        claims.put("realName", userDetails.getRealName());
                }
                String jwt = jwtUtil.generateToken(userDetails, claims);

                List<String> roles = userDetails.getAuthorities().stream()
                                .map(item -> item.getAuthority())
                                .collect(Collectors.toList());

                String departmentName = null;
                if (userDetails.getDepartmentId() != null) {
                        Department department = departmentRepository.findById(userDetails.getDepartmentId())
                                        .orElse(null);
                        if (department != null) {
                                departmentName = department.getDeptName();
                        }
                }

                return new LoginResponse(
                                jwt,
                                userDetails.getId(),
                                userDetails.getUsername(),
                                userDetails.getEmail(),
                                userDetails.getRealName(),
                                userDetails.getDepartmentId(),
                                departmentName,
                                roles);
        }

        /** 存量明文/弱哈希密码在登录成功后重新以 BCrypt 存储 */
        private void upgradeStoredPassword(String username, String rawPassword, UserDetailsImpl userDetails) {
                User user = userRepository.findByUsername(username).orElse(null);
                if (user == null || !passwordEncoder.upgradeEncoding(user.getPassword())) {
                        return;
                }
                user.setPassword(passwordEncoder.encode(rawPassword));
                userRepository.save(user);
                userDetails.setPassword(user.getPassword());
        }
}
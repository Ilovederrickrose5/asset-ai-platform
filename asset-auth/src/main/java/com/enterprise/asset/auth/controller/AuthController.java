package com.enterprise.asset.auth.controller;

import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.common.dto.LoginRequest;
import com.enterprise.asset.common.dto.LoginResponse;
import com.enterprise.asset.common.dto.UserDTO;
import com.enterprise.asset.common.dto.DepartmentDTO;
import com.enterprise.asset.common.dto.ValidateTokenRequest;
import com.enterprise.asset.common.security.JwtUtil;
import com.enterprise.asset.auth.service.AuthService;
import com.enterprise.asset.auth.service.UserService;
import com.enterprise.asset.auth.service.DepartmentService;
import com.enterprise.asset.auth.entity.User;
import com.enterprise.asset.auth.entity.Department;
import com.enterprise.asset.auth.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 认证控制器 - 处理用户登录/登出及用户信息查询 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final DepartmentService departmentService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, UserService userService,
            DepartmentService departmentService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.userService = userService;
        this.departmentService = departmentService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * POST /api/auth/login - 用户登录
     * 
     * @param loginRequest 登录请求体 {"username": "...", "password": "..."}
     * @return 登录响应（包含用户信息和权限）
     */
    @PostMapping("/login")
    public ResponseEntity<Result<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = authService.login(loginRequest);
        return ResponseEntity.ok(Result.success(loginResponse));
    }

    /**
     * POST /api/auth/logout - 用户登出
     *
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ResponseEntity<Result<String>> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Result.success("登出成功"));
    }

    /**
     * POST /api/auth/validate-token - 验证Token有效性
     * 业务服务JWT过滤器调用此接口统一校验token
     *
     * @param request Token验证请求
     * @return 用户信息（如果token有效）
     */
    @PostMapping("/validate-token")
    public ResponseEntity<Result<UserDTO>> validateToken(@RequestBody ValidateTokenRequest request) {
        try {
            String token = request.getToken();
            String username = jwtUtil.extractUsername(token);

            User user = userService.getUserByUsername(username);
            if (user == null) {
                return ResponseEntity.ok(Result.error(401, "用户不存在"));
            }

            UserDTO userDTO = convertToUserDTO(user);
            return ResponseEntity.ok(Result.success(userDTO));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(401, "Token无效或已过期"));
        }
    }

    /**
     * GET /api/auth/users/{id} - 根据ID获取用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<Result<UserDTO>> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.ok(Result.error(404, "用户不存在"));
        }
        UserDTO userDTO = convertToUserDTO(user);
        return ResponseEntity.ok(Result.success(userDTO));
    }

    /**
     * GET /api/auth/users/username/{username} - 根据用户名获取用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/users/username/{username}")
    public ResponseEntity<Result<UserDTO>> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return ResponseEntity.ok(Result.error(404, "用户不存在"));
        }
        UserDTO userDTO = convertToUserDTO(user);
        return ResponseEntity.ok(Result.success(userDTO));
    }

    /**
     * GET /api/auth/users/current - 获取当前用户信息
     *
     * @return 当前用户信息
     */
    @GetMapping("/users/current")
    public ResponseEntity<Result<UserDTO>> getCurrentUser() {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();

            User user = userService.getUserById(userDetails.getId());
            if (user == null) {
                return ResponseEntity.ok(Result.error(404, "用户不存在"));
            }
            UserDTO userDTO = convertToUserDTO(user);
            return ResponseEntity.ok(Result.success(userDTO));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(401, "未登录或登录已过期"));
        }
    }

    /**
     * GET /api/auth/departments/{id} - 根据ID获取部门信息
     *
     * @param id 部门ID
     * @return 部门信息
     */
    @GetMapping("/departments/{id}")
    public ResponseEntity<Result<DepartmentDTO>> getDepartmentById(@PathVariable Long id) {
        Department department = departmentService.getDepartmentById(id);
        if (department == null) {
            return ResponseEntity.ok(Result.error(404, "部门不存在"));
        }
        DepartmentDTO departmentDTO = convertToDepartmentDTO(department);
        return ResponseEntity.ok(Result.success(departmentDTO));
    }

    /**
     * GET /api/auth/departments/stats - 获取部门用户数量统计
     *
     * @param deptId 部门ID
     * @return 用户数量
     */
    @GetMapping("/departments/stats")
    public ResponseEntity<Result<Long>> getDepartmentUserCount(@RequestParam Long deptId) {
        long count = userService.getUserCountByDepartment(deptId);
        return ResponseEntity.ok(Result.success(count));
    }

    /**
     * GET /api/auth/users/count - 获取用户总数统计
     *
     * @return 用户总数
     */
    @GetMapping("/users/count")
    public ResponseEntity<Result<Long>> getUserCount() {
        long count = userService.getActiveUserCount();
        return ResponseEntity.ok(Result.success(count));
    }

    /**
     * GET /api/auth/users - 获取所有用户列表（供业务服务调用）
     *
     * @return 用户列表
     */
    @GetMapping("/users")
    public ResponseEntity<Result<List<UserDTO>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserDTO> userDTOs = users.stream()
                .map(this::convertToUserDTO)
                .toList();
        return ResponseEntity.ok(Result.success(userDTOs));
    }

    /**
     * GET /api/auth/users/department/{deptId} - 根据部门ID获取用户列表
     *
     * @param deptId 部门ID
     * @return 用户列表
     */
    @GetMapping("/users/department/{deptId}")
    public ResponseEntity<Result<List<UserDTO>>> getUsersByDepartment(@PathVariable Long deptId) {
        List<User> users = userService.getUsersByDepartment(deptId);
        List<UserDTO> userDTOs = users.stream()
                .map(this::convertToUserDTO)
                .toList();
        return ResponseEntity.ok(Result.success(userDTOs));
    }

    /**
     * GET /api/auth/users/count/dept/{deptId} - 统计指定部门的用户数量
     *
     * @param deptId 部门ID
     * @return 用户数量
     */
    @GetMapping("/users/count/dept/{deptId}")
    public ResponseEntity<Result<Long>> countUsersByDepartment(@PathVariable Long deptId) {
        long count = userService.getUserCountByDepartment(deptId);
        return ResponseEntity.ok(Result.success(count));
    }

    /**
     * GET /api/auth/departments - 获取所有部门列表（供业务服务调用）
     *
     * @return 部门列表
     */
    @GetMapping("/departments")
    public ResponseEntity<Result<List<DepartmentDTO>>> getAllDepartments() {
        List<Department> departments = departmentService.getAllDepartments();
        List<DepartmentDTO> departmentDTOs = departments.stream()
                .map(this::convertToDepartmentDTO)
                .toList();
        return ResponseEntity.ok(Result.success(departmentDTOs));
    }

    /**
     * GET /api/auth/departments/count - 统计启用状态的部门数量
     *
     * @return 部门数量
     */
    @GetMapping("/departments/count")
    public ResponseEntity<Result<Long>> getDepartmentCount() {
        long count = departmentService.getActiveDepartmentCount();
        return ResponseEntity.ok(Result.success(count));
    }

    /**
     * 将User实体转换为UserDTO
     */
    private UserDTO convertToUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRealName(user.getRealName());
        dto.setEmployeeNo(user.getEmployeeNo());
        dto.setDeptId(user.getDeptId());
        dto.setPosition(user.getPosition());
        dto.setStatus(user.getStatus());
        dto.setRoleCodes(user.getRoleCodes());
        dto.setRoleNames(user.getRoleNames());
        return dto;
    }

    /**
     * 将Department实体转换为DepartmentDTO
     */
    private DepartmentDTO convertToDepartmentDTO(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setDeptName(department.getDeptName());
        dto.setDeptCode(department.getDeptCode());
        dto.setParentId(department.getParentId());
        dto.setLeader(department.getLeader());
        dto.setDescription(department.getDescription());
        dto.setStatus(department.getStatus());
        return dto;
    }
}
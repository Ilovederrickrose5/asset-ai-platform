package com.enterprise.asset.business.controller;

import com.enterprise.asset.common.dto.DashboardOperationsDTO;
import com.enterprise.asset.common.dto.DashboardStatsDTO;
import com.enterprise.asset.common.dto.RecentOperationDTO;
import com.enterprise.asset.common.dto.UserDTO;
import com.enterprise.asset.common.enums.UserRole;
import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.business.service.DashboardService;
import com.enterprise.asset.business.service.RecentOperationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 仪表盘控制器 - 处理首页统计数据和操作记录
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;
    private final RecentOperationService recentOperationService;

    public DashboardController(DashboardService dashboardService,
            RecentOperationService recentOperationService) {
        this.dashboardService = dashboardService;
        this.recentOperationService = recentOperationService;
    }

    /**
     * GET /api/dashboard/stats - 获取仪表盘统计数据
     * 权限：admin查看全部统计，manager/leader查看部门统计，普通用户查看个人资产统计
     */
    @GetMapping("/stats")
    public ResponseEntity<Result<DashboardStatsDTO>> getStats() {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.ok(Result.error(401, "用户未登录"));
        }

        DashboardStatsDTO stats;
        if (hasRole(user, UserRole.ADMIN)) {
            stats = dashboardService.getStatsForAdmin();
        } else if (hasRole(user, UserRole.LEADER, UserRole.MANAGER, UserRole.ASSET_MANAGER, UserRole.DEPT_MANAGER)) {
            stats = dashboardService.getStatsForDepartment(user.getDeptId());
        } else {
            stats = dashboardService.getStatsForUser(user.getId());
        }

        return ResponseEntity.ok(Result.success(stats));
    }

    /**
     * GET /api/dashboard/recent-operations - 获取最近操作记录
     */
    @GetMapping("/recent-operations")
    public ResponseEntity<Result<List<RecentOperationDTO>>> getRecentOperations(
            @RequestParam(defaultValue = "10") int limit) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.ok(Result.error(401, "用户未登录"));
        }
        try {
            List<RecentOperationDTO> operations = recentOperationService.getRecentOperations(limit, user);
            return ResponseEntity.ok(Result.success(operations));
        } catch (Exception e) {
            logger.error("获取最近操作失败", e);
            return ResponseEntity.ok(Result.error(500, "获取最近操作失败"));
        }
    }

    /**
     * GET /api/dashboard/operations - 获取仪表盘操作数据（待处理任务和最近动态）
     */
    @GetMapping("/operations")
    public ResponseEntity<Result<DashboardOperationsDTO>> getDashboardOperations(
            @RequestParam(defaultValue = "10") int limit) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.ok(Result.error(401, "用户未登录"));
        }
        try {
            DashboardOperationsDTO operations = recentOperationService.getDashboardOperations(limit, user);
            return ResponseEntity.ok(Result.success(operations));
        } catch (Exception e) {
            logger.error("获取仪表盘操作数据失败", e);
            return ResponseEntity.ok(Result.error(500, "获取仪表盘操作数据失败"));
        }
    }

    private UserDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getPrincipal() instanceof UserDTO user ? user : null;
    }

    private boolean hasRole(UserDTO user, UserRole... roles) {
        if (user.getRoleCodes() == null) {
            return false;
        }
        for (String code : user.getRoleCodes()) {
            UserRole parsed = UserRole.fromCode(code);
            if (parsed == null) {
                continue;
            }
            for (UserRole target : roles) {
                if (parsed == target) {
                    return true;
                }
            }
        }
        return false;
    }
}

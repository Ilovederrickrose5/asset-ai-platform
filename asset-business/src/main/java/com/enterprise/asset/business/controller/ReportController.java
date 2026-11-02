package com.enterprise.asset.business.controller;

import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.common.dto.AssetStatusDistributionDTO;
import com.enterprise.asset.common.dto.DepartmentAssetStatsDTO;
import com.enterprise.asset.common.dto.UserDTO;
import com.enterprise.asset.business.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 报表控制器 - 处理资产统计报表查询
 * 
 * Feign改造说明：
 * - 删除了UserRepository本地DAO依赖
 * - 通过AuthFeignClient远程调用asset-auth服务获取用户信息
 * - 使用UserDTO替代User实体进行跨服务数据传输
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * GET /api/reports/department-stats - 获取部门资产统计
     * 权限：admin查看全部，leader/manager查看本部门，普通用户无权限
     */
    @GetMapping("/department-stats")
    public ResponseEntity<Result<List<DepartmentAssetStatsDTO>>> getDepartmentAssetStats() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof UserDTO) {
                UserDTO user = (UserDTO) authentication.getPrincipal();
                List<String> roles = user.getRoleCodes();

                boolean isAdmin = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("ADMIN"));
                boolean isLeader = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("LEADER"));
                boolean isManager = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("MANAGER")
                        || r.equalsIgnoreCase("ASSET_MANAGER") || r.equalsIgnoreCase("DEPT_MANAGER"));

                if (!isAdmin && !isLeader && !isManager) {
                    return ResponseEntity.ok(Result.error(403, "无权限访问部门资产统计"));
                }

                if ((isManager || isLeader) && user.getDeptId() != null) {
                    List<DepartmentAssetStatsDTO> stats = reportService
                            .getDepartmentAssetStatsByDepartment(user.getDeptId());
                    return ResponseEntity.ok(Result.success(stats));
                }
            }

            List<DepartmentAssetStatsDTO> stats = reportService.getDepartmentAssetStats();
            return ResponseEntity.ok(Result.success(stats));
        } catch (Exception e) {
            logger.error("获取部门资产统计失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error(500, "获取部门资产统计失败"));
        }
    }

    /**
     * GET /api/reports/department-stats/{departmentId} - 获取指定部门资产统计
     * 权限：admin可查看任意部门，leader/manager只能查看本部门
     */
    @GetMapping("/department-stats/{departmentId}")
    public ResponseEntity<Result<List<DepartmentAssetStatsDTO>>> getDepartmentAssetStatsByDepartment(
            @PathVariable Long departmentId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof UserDTO) {
                UserDTO user = (UserDTO) authentication.getPrincipal();
                List<String> roles = user.getRoleCodes();
                boolean isAdmin = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("ADMIN"));

                if (!isAdmin && (user.getDeptId() == null || !user.getDeptId().equals(departmentId))) {
                    return ResponseEntity.ok(Result.error(403, "无权限访问该部门资产统计"));
                }
            }

            List<DepartmentAssetStatsDTO> stats = reportService.getDepartmentAssetStatsByDepartment(departmentId);
            return ResponseEntity.ok(Result.success(stats));
        } catch (Exception e) {
            logger.error("获取部门资产统计失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error(500, "获取部门资产统计失败"));
        }
    }

    /**
     * GET /api/reports/status-distribution - 获取资产状态分布
     * 权限：admin查看全部，leader/manager查看本部门，普通用户无权限
     */
    @GetMapping("/status-distribution")
    public ResponseEntity<Result<List<AssetStatusDistributionDTO>>> getAssetStatusDistribution() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof UserDTO) {
                UserDTO user = (UserDTO) authentication.getPrincipal();
                List<String> roles = user.getRoleCodes();

                boolean isAdmin = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("ADMIN"));
                boolean isLeader = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("LEADER"));
                boolean isManager = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("MANAGER")
                        || r.equalsIgnoreCase("ASSET_MANAGER") || r.equalsIgnoreCase("DEPT_MANAGER"));

                if (!isAdmin && !isLeader && !isManager) {
                    return ResponseEntity.ok(Result.error(403, "无权限访问资产状态分布"));
                }

                if ((isManager || isLeader) && user.getDeptId() != null) {
                    List<AssetStatusDistributionDTO> distribution = reportService
                            .getAssetStatusDistributionByDepartment(user.getDeptId());
                    return ResponseEntity.ok(Result.success(distribution));
                }
            }

            List<AssetStatusDistributionDTO> distribution = reportService.getAssetStatusDistribution();
            return ResponseEntity.ok(Result.success(distribution));
        } catch (Exception e) {
            logger.error("获取资产状态分布失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(Result.error(500, "获取资产状态分布失败"));
        }
    }
}
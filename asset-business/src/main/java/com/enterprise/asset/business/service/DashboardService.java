package com.enterprise.asset.business.service;

import com.enterprise.asset.common.enums.ApplicationStatus;
import com.enterprise.asset.common.dto.DashboardStatsDTO;
import com.enterprise.asset.common.dto.DepartmentDTO;
import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.business.client.AuthFeignClient;
import com.enterprise.asset.business.repository.AssetApplicationRepository;
import com.enterprise.asset.business.repository.AssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 仪表盘服务 - 处理首页统计数据查询
 * 
 * Feign改造说明：
 * - 删除了UserRepository、DepartmentRepository本地DAO依赖
 * - 通过AuthFeignClient远程调用asset-auth服务获取用户、部门数据
 * - 实现了标准微服务跨服务通信模式
 */
@Service
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    private final AssetRepository assetRepository;
    private final AssetApplicationRepository assetApplicationRepository;
    private final AuthFeignClient authFeignClient;

    public DashboardService(AssetRepository assetRepository,
            AssetApplicationRepository assetApplicationRepository,
            AuthFeignClient authFeignClient) {
        this.assetRepository = assetRepository;
        this.assetApplicationRepository = assetApplicationRepository;
        this.authFeignClient = authFeignClient;
    }

    /**
     * 获取管理员仪表盘统计数据
     * 统计内容：资产总数、待审批数量、部门数、用户数
     */
    public DashboardStatsDTO getStatsForAdmin() {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        // 查询资产总数（本地Repository）
        long assetCount = assetRepository.countAllAssets();
        stats.setAssetCount(assetCount);

        // 查询待审批数量（本地Repository）
        long totalPending = assetApplicationRepository.countByStatus(ApplicationStatus.PENDING.getCode());
        stats.setPendingCount(totalPending);

        // Feign远程调用：获取部门数量
        Result<Long> deptCountResult = authFeignClient.getDepartmentCount();
        stats.setDepartmentCount(deptCountResult.getData() != null ? deptCountResult.getData() : 0L);

        // Feign远程调用：获取用户总数
        Result<Long> userCountResult = authFeignClient.getUserCount();
        stats.setUserCount(userCountResult.getData() != null ? userCountResult.getData() : 0L);

        stats.setDepartmentName("公司");

        logger.info("系统管理员统计数据 - 资产总数: {}, 待审批总数: {}, 部门数: {}, 用户数: {}",
                assetCount, totalPending, stats.getDepartmentCount(), stats.getUserCount());

        return stats;
    }

    /**
     * 获取部门仪表盘统计数据
     * 统计内容：部门资产数、部门用户数
     */
    public DashboardStatsDTO getStatsForDepartment(Long deptId) {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        // 查询部门资产数（本地Repository）
        long assetCount = assetRepository.countAllAssetsByDepartment(deptId);
        stats.setAssetCount(assetCount);

        stats.setPendingCount(0L);
        stats.setDepartmentCount(null);

        // Feign远程调用：统计部门用户数量
        Result<Long> userCountResult = authFeignClient.countUsersByDepartment(deptId);
        stats.setUserCount(userCountResult.getData() != null ? userCountResult.getData() : 0L);

        // Feign远程调用：获取部门名称
        Result<DepartmentDTO> deptResult = authFeignClient.getDepartmentById(deptId);
        stats.setDepartmentName(deptResult.getData() != null
                ? deptResult.getData().getDeptName()
                : "部门");

        logger.info("部门统计数据 - 部门ID: {}, 资产数: {}, 用户数: {}",
                deptId, assetCount, stats.getUserCount());

        return stats;
    }

    /**
     * 获取个人仪表盘统计数据
     * 统计内容：个人资产数
     */
    public DashboardStatsDTO getStatsForUser(Long userId) {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        // 查询个人资产数（本地Repository）
        long assetCount = assetRepository.countAllAssetsByUser(userId);
        stats.setAssetCount(assetCount);

        stats.setPendingCount(0L);
        stats.setDepartmentCount(null);
        stats.setUserCount(1L);
        stats.setDepartmentName("个人");

        logger.info("个人统计数据 - 用户ID: {}, 资产数: {}", userId, assetCount);

        return stats;
    }
}
package com.enterprise.asset.business.service.impl;

import com.enterprise.asset.common.enums.AssetStatus;
import com.enterprise.asset.common.dto.AssetStatusDistributionDTO;
import com.enterprise.asset.common.dto.DepartmentAssetStatsDTO;
import com.enterprise.asset.common.dto.UserDTO;
import com.enterprise.asset.common.dto.DepartmentDTO;
import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.business.client.AuthFeignClient;
import com.enterprise.asset.business.entity.Asset;
import com.enterprise.asset.business.repository.AssetRepository;
import com.enterprise.asset.business.service.ReportService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表服务实现 - 处理资产统计报表查询
 * 
 * Feign改造说明：
 * - 删除了UserRepository、DepartmentRepository本地DAO依赖
 * - 通过AuthFeignClient远程调用asset-auth服务获取用户、部门数据
 * - 使用UserDTO、DepartmentDTO替代User、Department实体进行跨服务数据传输
 */
@Service
public class ReportServiceImpl implements ReportService {

    private final AssetRepository assetRepository;
    private final AuthFeignClient authFeignClient;

    public ReportServiceImpl(AssetRepository assetRepository, AuthFeignClient authFeignClient) {
        this.assetRepository = assetRepository;
        this.authFeignClient = authFeignClient;
    }

    private static final List<AssetStatus> STATUS_ORDER = List.of(
            AssetStatus.SCRAPPED, AssetStatus.USING, AssetStatus.IN_STOCK, AssetStatus.MAINTENANCE);

    /**
     * 获取所有部门资产统计
     * Feign改造：通过AuthFeignClient远程获取用户、部门列表
     */
    @Override
    public List<DepartmentAssetStatsDTO> getDepartmentAssetStats() {
        List<Asset> assets = assetRepository.findAll();

        // Feign远程调用：获取所有用户列表
        Result<List<UserDTO>> usersResult = authFeignClient.getAllUsers();
        List<UserDTO> users = usersResult.getData() != null ? usersResult.getData() : List.of();

        // Feign远程调用：获取所有部门列表
        Result<List<DepartmentDTO>> deptsResult = authFeignClient.getAllDepartments();
        List<DepartmentDTO> departments = deptsResult.getData() != null ? deptsResult.getData() : List.of();

        Map<Long, Long> userToDeptMap = users.stream()
                .collect(Collectors.toMap(UserDTO::getId, UserDTO::getDeptId));
        Map<Long, String> deptIdToNameMap = departments.stream()
                .collect(Collectors.toMap(DepartmentDTO::getId, DepartmentDTO::getDeptName));

        Map<Long, List<Asset>> assetsByDepartment = assets.stream()
                .collect(Collectors.groupingBy(asset -> getAssetDepartmentId(asset, userToDeptMap)));

        List<DepartmentAssetStatsDTO> statsList = new ArrayList<>();
        for (Map.Entry<Long, List<Asset>> entry : assetsByDepartment.entrySet()) {
            statsList.add(buildDepartmentStats(entry.getKey(), entry.getValue(), deptIdToNameMap));
        }

        return statsList;
    }

    @Override
    public List<AssetStatusDistributionDTO> getAssetStatusDistribution() {
        List<Asset> assets = assetRepository.findAll();
        return buildStatusDistribution(assets);
    }

    /**
     * 获取指定部门的资产统计
     * Feign改造：通过AuthFeignClient远程获取用户、部门列表
     */
    @Override
    public List<DepartmentAssetStatsDTO> getDepartmentAssetStatsByDepartment(Long departmentId) {
        List<Asset> assets = assetRepository.findAll();

        // Feign远程调用：获取所有用户列表
        Result<List<UserDTO>> usersResult = authFeignClient.getAllUsers();
        List<UserDTO> users = usersResult.getData() != null ? usersResult.getData() : List.of();

        // Feign远程调用：获取所有部门列表
        Result<List<DepartmentDTO>> deptsResult = authFeignClient.getAllDepartments();
        List<DepartmentDTO> departments = deptsResult.getData() != null ? deptsResult.getData() : List.of();

        Map<Long, Long> userToDeptMap = users.stream()
                .collect(Collectors.toMap(UserDTO::getId, UserDTO::getDeptId));
        Map<Long, String> deptIdToNameMap = departments.stream()
                .collect(Collectors.toMap(DepartmentDTO::getId, DepartmentDTO::getDeptName));

        List<Asset> departmentAssets = assets.stream()
                .filter(asset -> getAssetDepartmentId(asset, userToDeptMap).equals(departmentId))
                .collect(Collectors.toList());

        List<DepartmentAssetStatsDTO> statsList = new ArrayList<>();
        if (!departmentAssets.isEmpty()) {
            statsList.add(buildDepartmentStats(departmentId, departmentAssets, deptIdToNameMap));
        } else {
            DepartmentAssetStatsDTO stats = new DepartmentAssetStatsDTO();
            stats.setDepartmentId(departmentId);
            stats.setDepartmentName(deptIdToNameMap.getOrDefault(departmentId, "部门" + departmentId));
            stats.setAssetCount(0);
            stats.setTotalValue(0.0);
            stats.setAverageValue(0.0);
            stats.setInUseCount(0);
            stats.setIdleCount(0);
            stats.setMaintenanceCount(0);
            stats.setScrappedCount(0);
            statsList.add(stats);
        }

        return statsList;
    }

    /**
     * 获取指定部门的资产状态分布
     * Feign改造：通过AuthFeignClient远程获取用户列表
     */
    @Override
    public List<AssetStatusDistributionDTO> getAssetStatusDistributionByDepartment(Long departmentId) {
        List<Asset> assets = assetRepository.findAll();

        // Feign远程调用：获取所有用户列表
        Result<List<UserDTO>> usersResult = authFeignClient.getAllUsers();
        List<UserDTO> users = usersResult.getData() != null ? usersResult.getData() : List.of();

        Map<Long, Long> userToDeptMap = users.stream()
                .collect(Collectors.toMap(UserDTO::getId, UserDTO::getDeptId));

        List<Asset> departmentAssets = assets.stream()
                .filter(asset -> getAssetDepartmentId(asset, userToDeptMap).equals(departmentId))
                .collect(Collectors.toList());

        return buildStatusDistribution(departmentAssets);
    }

    private Long getAssetDepartmentId(Asset asset, Map<Long, Long> userToDeptMap) {
        if (asset.getDeptId() != null) {
            return asset.getDeptId();
        } else if (asset.getUserId() != null && userToDeptMap.containsKey(asset.getUserId())) {
            return userToDeptMap.get(asset.getUserId());
        } else {
            return 0L;
        }
    }

    private double calculateTotalValue(List<Asset> assets) {
        return assets.stream()
                .map(Asset::getPurchasePrice)
                .filter(price -> price != null)
                .mapToDouble(java.math.BigDecimal::doubleValue)
                .sum();
    }

    private Map<String, Long> countAssetsByStatus(List<Asset> assets) {
        return assets.stream()
                .collect(Collectors.groupingBy(Asset::getStatus, Collectors.counting()));
    }

    private DepartmentAssetStatsDTO buildDepartmentStats(Long departmentId, List<Asset> departmentAssets,
            Map<Long, String> deptIdToNameMap) {
        DepartmentAssetStatsDTO stats = new DepartmentAssetStatsDTO();
        stats.setDepartmentId(departmentId);
        stats.setDepartmentName(deptIdToNameMap.getOrDefault(departmentId,
                departmentId != 0L ? "部门" + departmentId : "公共资产"));
        stats.setAssetCount(departmentAssets.size());

        double totalValue = calculateTotalValue(departmentAssets);
        stats.setTotalValue(totalValue);

        if (!departmentAssets.isEmpty()) {
            stats.setAverageValue(totalValue / departmentAssets.size());
        }

        Map<String, Long> statusCounts = countAssetsByStatus(departmentAssets);
        stats.setInUseCount(statusCounts.getOrDefault(AssetStatus.USING.getCode(), 0L));
        stats.setIdleCount(statusCounts.getOrDefault(AssetStatus.IN_STOCK.getCode(), 0L));
        stats.setMaintenanceCount(statusCounts.getOrDefault(AssetStatus.MAINTENANCE.getCode(), 0L));
        stats.setScrappedCount(statusCounts.getOrDefault(AssetStatus.SCRAPPED.getCode(), 0L)
                + statusCounts.getOrDefault(AssetStatus.DISPOSED.getCode(), 0L));

        return stats;
    }

    private List<AssetStatusDistributionDTO> buildStatusDistribution(List<Asset> assets) {
        long totalCount = assets.size();
        Map<String, List<Asset>> assetsByStatus = assets.stream()
                .collect(Collectors.groupingBy(Asset::getStatus));

        List<AssetStatusDistributionDTO> distributionList = new ArrayList<>();

        List<Asset> scrappedAssets = assetsByStatus.getOrDefault(AssetStatus.SCRAPPED.getCode(), new ArrayList<>());
        List<Asset> disposedAssets = assetsByStatus.getOrDefault(AssetStatus.DISPOSED.getCode(), new ArrayList<>());

        int scrappedCount = scrappedAssets.size() + disposedAssets.size();
        double scrappedValue = calculateTotalValue(scrappedAssets) + calculateTotalValue(disposedAssets);

        AssetStatusDistributionDTO scrappedDistribution = new AssetStatusDistributionDTO();
        scrappedDistribution.setStatus(AssetStatus.SCRAPPED.getCode());
        scrappedDistribution.setStatusName(AssetStatus.SCRAPPED.getName());
        scrappedDistribution.setCount(scrappedCount);
        scrappedDistribution.setPercentage(totalCount > 0 ? (double) scrappedCount / totalCount * 100 : 0.0);
        scrappedDistribution.setTotalValue(scrappedValue);
        distributionList.add(scrappedDistribution);

        for (AssetStatus status : STATUS_ORDER) {
            if (status == AssetStatus.SCRAPPED) {
                continue;
            }

            List<Asset> statusAssets = assetsByStatus.getOrDefault(status.getCode(), new ArrayList<>());
            AssetStatusDistributionDTO distribution = new AssetStatusDistributionDTO();
            distribution.setStatus(status.getCode());
            distribution.setStatusName(status.getName());
            distribution.setCount(statusAssets.size());
            distribution.setPercentage(totalCount > 0 ? (double) statusAssets.size() / totalCount * 100 : 0.0);
            distribution.setTotalValue(calculateTotalValue(statusAssets));
            distributionList.add(distribution);
        }

        return distributionList;
    }
}
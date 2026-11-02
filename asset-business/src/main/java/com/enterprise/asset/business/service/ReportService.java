/** 报表服务接口 - 处理资产统计报表查询（权限控制：admin可查看全部，leader/manager查看本部门） */
package com.enterprise.asset.business.service;

import com.enterprise.asset.common.dto.AssetStatusDistributionDTO;
import com.enterprise.asset.common.dto.DepartmentAssetStatsDTO;
import java.util.List;

public interface ReportService {
    /** 获取所有部门资产统计 */
    List<DepartmentAssetStatsDTO> getDepartmentAssetStats();
    /** 获取资产状态分布 */
    List<AssetStatusDistributionDTO> getAssetStatusDistribution();
    /** 根据部门ID获取部门资产统计 */
    List<DepartmentAssetStatsDTO> getDepartmentAssetStatsByDepartment(Long departmentId);
    /** 根据部门ID获取资产状态分布 */
    List<AssetStatusDistributionDTO> getAssetStatusDistributionByDepartment(Long departmentId);
}
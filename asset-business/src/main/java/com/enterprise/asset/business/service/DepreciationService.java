/** 折旧服务接口 - 处理资产折旧计算与记录管理 */
package com.enterprise.asset.business.service;

import com.enterprise.asset.business.entity.Asset;
import com.enterprise.asset.business.entity.DepreciationRecord;

import java.time.LocalDate;
import java.util.List;

public interface DepreciationService {

        /** 计算单个资产折旧（使用资产默认折旧方法） */
        DepreciationRecord calculateAssetDepreciation(Long assetId, LocalDate startDate, LocalDate endDate);

        /** 计算单个资产折旧（工作量法） */
        DepreciationRecord calculateAssetDepreciation(Long assetId, LocalDate startDate, LocalDate endDate,
                        java.math.BigDecimal actualWorkUnits);

        /** 计算单个资产折旧（指定折旧方法） */
        DepreciationRecord calculateAssetDepreciation(Long assetId, LocalDate startDate, LocalDate endDate,
                        String depreciationMethod, Integer actualWorkUnits);

        /** 批量计算资产折旧（日期范围） */
        List<DepreciationRecord> calculateBatchDepreciation(List<Long> assetIds, LocalDate startDate,
                        LocalDate endDate);

        /** 批量计算资产折旧（日期范围+工作量） */
        List<DepreciationRecord> calculateBatchDepreciation(List<Long> assetIds, LocalDate startDate, LocalDate endDate,
                        java.math.BigDecimal actualWorkUnits);

        /** 批量计算资产折旧（指定月份） */
        List<DepreciationRecord> calculateBatchDepreciation(List<Long> assetIds, String depreciationMonth);

        /** 批量计算资产折旧（指定月份和方法） */
        List<DepreciationRecord> calculateBatchDepreciation(List<Long> assetIds, String depreciationMonth,
                        String depreciationMethod);

        /** 计算所有资产折旧 */
        List<DepreciationRecord> calculateAllAssetsDepreciation(LocalDate startDate, LocalDate endDate);

        /** 获取资产最新折旧记录 */
        DepreciationRecord getLatestDepreciationRecord(Long assetId);

        /** 根据资产ID获取折旧记录列表 */
        List<DepreciationRecord> getDepreciationRecordsByAssetId(Long assetId);

        /** 根据日期范围获取折旧记录 */
        List<DepreciationRecord> getDepreciationRecordsByDateRange(LocalDate startDate, LocalDate endDate);

        /** 根据分类获取折旧记录 */
        List<DepreciationRecord> getDepreciationRecordsByCategory(Long categoryId);

        /** 根据部门获取折旧记录 */
        List<DepreciationRecord> getDepreciationRecordsByDepartment(Long departmentId);

        /** 验证折旧计算结果 */
        boolean validateDepreciationCalculation(Asset asset, DepreciationRecord record);

        /** 回滚折旧记录 */
        void rollbackDepreciationRecord(Long recordId);

        /** 根据ID获取折旧记录 */
        DepreciationRecord getDepreciationRecordById(Long recordId);
}
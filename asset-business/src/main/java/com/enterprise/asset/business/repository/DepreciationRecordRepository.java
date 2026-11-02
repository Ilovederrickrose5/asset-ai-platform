package com.enterprise.asset.business.repository;

import com.enterprise.asset.business.entity.DepreciationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 折旧记录数据访问接口
 * 关联表: depreciation_record(折旧记录表)
 * 主要操作: 折旧记录的增删改查、按资产/部门/分类查询、统计计算
 */
public interface DepreciationRecordRepository extends JpaRepository<DepreciationRecord, Long> {

        /** 根据资产ID查询折旧记录 */
        List<DepreciationRecord> findByAssetId(Long assetId);

        /** 根据资产ID查询折旧记录并按创建时间倒序 */
        List<DepreciationRecord> findByAssetIdOrderByCreateTimeDesc(Long assetId);

        /** 获取资产最新折旧记录 */
        Optional<DepreciationRecord> findTopByAssetIdOrderByCreateTimeDesc(Long assetId);

        /** 查询资产指定月份的折旧记录 */
        Optional<DepreciationRecord> findTopByAssetIdAndDepreciationMonth(Long assetId, String depreciationMonth);

        /** 根据折旧期间和创建时间范围查询 */
        List<DepreciationRecord> findByDepreciationPeriodAndCreateTimeBetween(
                        String depreciationPeriod,
                        LocalDateTime startDate,
                        LocalDateTime endDate);

        /** 根据创建时间范围查询 */
        List<DepreciationRecord> findByCreateTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

        /** 根据资产分类ID查询折旧记录 */
        List<DepreciationRecord> findByCategoryId(Long categoryId);

        /** 根据部门ID查询折旧记录 */
        List<DepreciationRecord> findByDepartmentId(Long departmentId);

        /** 根据创建时间和状态查询 */
        @Query("SELECT dr FROM DepreciationRecord dr WHERE dr.createTime = :date AND dr.status = :status")
        List<DepreciationRecord> findByCreateTimeAndStatus(
                        @Param("date") LocalDateTime date,
                        @Param("status") String status);

        /** 统计资产累计折旧金额 */
        @Query("SELECT SUM(dr.depreciationAmount) FROM DepreciationRecord dr WHERE dr.assetId = :assetId")
        java.math.BigDecimal getTotalDepreciationByAssetId(@Param("assetId") Long assetId);

        /** 统计资产折旧记录数量 */
        @Query("SELECT COUNT(dr) FROM DepreciationRecord dr WHERE dr.assetId = :assetId")
        Long countByAssetId(@Param("assetId") Long assetId);

        /** 根据折旧期间查询 */
        @Query("SELECT dr FROM DepreciationRecord dr WHERE dr.depreciationPeriod = :period ORDER BY dr.createTime DESC")
        List<DepreciationRecord> findByDepreciationPeriod(@Param("period") String period);

        /** 根据资产ID列表和时间范围查询 */
        @Query("SELECT dr FROM DepreciationRecord dr WHERE dr.assetId IN :assetIds AND dr.createTime BETWEEN :startDate AND :endDate")
        List<DepreciationRecord> findByAssetIdsAndCreateTimeBetween(
                        @Param("assetIds") List<Long> assetIds,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /** 获取多个资产的最新折旧记录 */
        @Query("SELECT dr FROM DepreciationRecord dr WHERE dr.id IN (SELECT MAX(dr2.id) FROM DepreciationRecord dr2 WHERE dr2.assetId IN :assetIds GROUP BY dr2.assetId)")
        List<DepreciationRecord> findLatestByAssetIds(@Param("assetIds") List<Long> assetIds);

        /** 删除指定资产指定月份的折旧记录 */
        void deleteByAssetIdAndDepreciationMonth(Long assetId, String depreciationMonth);
}
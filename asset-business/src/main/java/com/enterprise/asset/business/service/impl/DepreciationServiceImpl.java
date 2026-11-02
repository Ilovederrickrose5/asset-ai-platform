package com.enterprise.asset.business.service.impl;

import com.enterprise.asset.common.enums.AssetStatus;
import com.enterprise.asset.common.enums.DepreciationMethod;
import com.enterprise.asset.business.entity.Asset;
import com.enterprise.asset.business.entity.DepreciationRecord;
import com.enterprise.asset.business.repository.AssetRepository;
import com.enterprise.asset.business.repository.DepreciationRecordRepository;
import com.enterprise.asset.business.service.DepreciationCalculator;
import com.enterprise.asset.business.service.DepreciationCalculatorFactory;
import com.enterprise.asset.business.service.DepreciationService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 折旧计算服务实现类
 * 
 * 【职责】：处理折旧计算业务逻辑 → 调用计算器 → 保存结果 → 返回给控制器
 * 
 * 【数据流向】：
 * Controller传入参数 → 查询资产信息 → 查询累计折旧 → 选择计算器 → 计算 → 保存记录 → 返回
 * 
 * 【依赖组件】：
 * - AssetRepository：查询资产信息
 * - DepreciationRecordRepository：查询和保存折旧记录
 * - DepreciationCalculatorFactory：根据方法类型获取对应的计算器
 */
@Service
public class DepreciationServiceImpl implements DepreciationService {

    private static final Logger logger = LoggerFactory.getLogger(DepreciationServiceImpl.class);

    private final AssetRepository assetRepository;
    private final DepreciationRecordRepository depreciationRecordRepository;
    private final DepreciationCalculatorFactory calculatorFactory;

    public DepreciationServiceImpl(AssetRepository assetRepository,
            DepreciationRecordRepository depreciationRecordRepository,
            DepreciationCalculatorFactory calculatorFactory) {
        this.assetRepository = assetRepository;
        this.depreciationRecordRepository = depreciationRecordRepository;
        this.calculatorFactory = calculatorFactory;
    }

    @Override
    @Transactional
    public DepreciationRecord calculateAssetDepreciation(Long assetId, LocalDate startDate, LocalDate endDate) {
        return calculateAssetDepreciation(assetId, startDate, endDate, BigDecimal.ONE);
    }

    @Override
    @Transactional
    public DepreciationRecord calculateAssetDepreciation(Long assetId, LocalDate startDate, LocalDate endDate,
            BigDecimal actualWorkUnits) {
        Optional<Asset> assetOpt = assetRepository.findById(assetId);
        if (assetOpt.isEmpty()) {
            throw new RuntimeException("资产不存在: " + assetId);
        }

        Asset asset = assetOpt.get();

        AssetStatus assetStatus = AssetStatus.fromCode(asset.getStatus());
        if (assetStatus != AssetStatus.IN_STOCK && assetStatus != AssetStatus.USING
                && assetStatus != AssetStatus.MAINTENANCE) {
            throw new RuntimeException("资产状态不允许计提折旧: " + asset.getStatus());
        }

        if (asset.getOriginalValue() == null || asset.getOriginalValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("资产原值必须大于0");
        }

        if (asset.getUsefulLife() == null || asset.getUsefulLife() <= 0) {
            throw new RuntimeException("资产使用年限必须大于0");
        }

        Optional<DepreciationRecord> lastRecordOpt = depreciationRecordRepository
                .findTopByAssetIdOrderByCreateTimeDesc(assetId);
        BigDecimal accumulatedDepreciation = lastRecordOpt
                .map(record -> record.getAccumulatedDepreciation() != null
                        ? record.getAccumulatedDepreciation()
                        : BigDecimal.ZERO)
                .orElse(BigDecimal.ZERO);
        Integer usedMonths = lastRecordOpt
                .map(record -> record.getUsedMonths() != null ? record.getUsedMonths() : 0)
                .orElse(0);

        if (usedMonths >= asset.getUsefulLife()) {
            logger.info("资产已达到使用年限，不再计提折旧: {}", asset.getAssetNo());
            return null;
        }

        DepreciationMethod method = DepreciationMethod.fromCode(asset.getDepreciationMethod());
        if (method == null) {
            method = DepreciationMethod.STRAIGHT_LINE;
        }
        DepreciationCalculator calculator = calculatorFactory.getCalculator(method.getCode());

        DepreciationRecord record;
        if (method == DepreciationMethod.WORK_UNIT) {
            // 对于工作量法，传入实际工作量
            record = calculator.calculateDepreciation(asset, startDate, endDate, accumulatedDepreciation, usedMonths,
                    actualWorkUnits);
        } else {
            // 其他折旧方法使用默认实现
            record = calculator.calculateDepreciation(asset, startDate, endDate, accumulatedDepreciation, usedMonths);
        }

        if (!validateDepreciationCalculation(asset, record)) {
            throw new RuntimeException("折旧计算结果验证失败");
        }

        record = depreciationRecordRepository.save(record);

        asset.setNetValue(record.getCurrentNetValue());
        assetRepository.save(asset);

        logger.info("资产折旧计算完成: {} 折旧金额: {} 净值: {}",
                asset.getAssetNo(), record.getDepreciationAmount(), record.getCurrentNetValue());

        return record;
    }

    /**
     * 计算单个资产折旧（支持自定义折旧方法）
     * 流程：查资产→校验→查累计折旧→选方法→调计算器→保存记录→更新净值
     * 参数：assetId, startDate, endDate, depreciationMethod(可选),
     * actualWorkUnits(工作量法用)
     */
    @Override
    @Transactional
    public DepreciationRecord calculateAssetDepreciation(Long assetId, LocalDate startDate, LocalDate endDate,
            String depreciationMethod, Integer actualWorkUnits) {
        // 查资产
        Optional<Asset> assetOpt = assetRepository.findById(assetId);
        if (assetOpt.isEmpty())
            throw new RuntimeException("资产不存在: " + assetId);
        Asset asset = assetOpt.get();

        // 校验状态和字段
        AssetStatus assetStatus = AssetStatus.fromCode(asset.getStatus());
        if (assetStatus != AssetStatus.IN_STOCK && assetStatus != AssetStatus.USING
                && assetStatus != AssetStatus.MAINTENANCE) {
            throw new RuntimeException("资产状态不允许计提折旧: " + asset.getStatus());
        }
        if (asset.getOriginalValue() == null || asset.getOriginalValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("资产原值必须大于0");
        }
        if (asset.getUsefulLife() == null || asset.getUsefulLife() <= 0) {
            throw new RuntimeException("资产使用年限必须大于0");
        }

        // 当月已有记录时先撤销旧记录，避免基于"已包含本月"的累计值重复计提
        String depreciationMonth = startDate.getYear() + "-" + String.format("%02d", startDate.getMonthValue());
        depreciationRecordRepository.findTopByAssetIdAndDepreciationMonth(assetId, depreciationMonth)
                .ifPresent(depreciationRecordRepository::delete);
        depreciationRecordRepository.flush();

        // 查累计折旧和已使用月数
        Optional<DepreciationRecord> lastRecordOpt = depreciationRecordRepository
                .findTopByAssetIdOrderByCreateTimeDesc(assetId);
        BigDecimal accumulatedDepreciation = lastRecordOpt
                .map(existing -> existing.getAccumulatedDepreciation() != null
                        ? existing.getAccumulatedDepreciation()
                        : BigDecimal.ZERO)
                .orElse(BigDecimal.ZERO);
        Integer usedMonths = lastRecordOpt
                .map(existing -> existing.getUsedMonths() != null ? existing.getUsedMonths() : 0)
                .orElse(0);

        // 检查使用年限
        if (usedMonths >= asset.getUsefulLife()) {
            logger.info("资产已达到使用年限，不再计提折旧: {}", asset.getAssetNo());
            return null;
        }

        // 确定折旧方法（优先级：传入 > 资产预设 > 默认直线法）
        DepreciationMethod method = (depreciationMethod != null && !depreciationMethod.isEmpty())
                ? DepreciationMethod.fromCode(depreciationMethod)
                : DepreciationMethod.fromCode(asset.getDepreciationMethod());
        if (method == null) {
            method = DepreciationMethod.STRAIGHT_LINE;
        }

        // 调计算器计算
        DepreciationCalculator calculator = calculatorFactory.getCalculator(method.getCode());
        DepreciationRecord record = method == DepreciationMethod.WORK_UNIT
                ? calculator.calculateDepreciation(asset, startDate, endDate, accumulatedDepreciation, usedMonths,
                        BigDecimal.valueOf(actualWorkUnits != null ? actualWorkUnits : 1))
                : calculator.calculateDepreciation(asset, startDate, endDate, accumulatedDepreciation, usedMonths);

        record.setDepreciationMethod(method.getCode());
        if (!validateDepreciationCalculation(asset, record)) {
            throw new RuntimeException("折旧计算结果验证失败");
        }

        record = depreciationRecordRepository.save(record);

        // 更新资产净值
        asset.setNetValue(record.getCurrentNetValue());
        assetRepository.save(asset);

        logger.info("资产折旧计算完成(自定义方法): {} 折旧方法: {} 折旧金额: {} 净值: {}",
                asset.getAssetNo(), method, record.getDepreciationAmount(), record.getCurrentNetValue());
        return record;
    }

    @Override
    @Transactional
    public List<DepreciationRecord> calculateBatchDepreciation(List<Long> assetIds, LocalDate startDate,
            LocalDate endDate) {
        List<DepreciationRecord> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (Long assetId : assetIds) {
            try {
                DepreciationRecord record = calculateAssetDepreciation(assetId, startDate, endDate);
                if (record != null) {
                    results.add(record);
                    successCount++;
                }
            } catch (Exception e) {
                logger.error("资产折旧计算失败: {} 错误: {}", assetId, e.getMessage());
                failureCount++;
            }
        }

        logger.info("批量折旧计算完成: 成功: {} 失败: {}", successCount, failureCount);
        return results;
    }

    @Override
    @Transactional
    public List<DepreciationRecord> calculateBatchDepreciation(List<Long> assetIds, LocalDate startDate,
            LocalDate endDate, BigDecimal actualWorkUnits) {
        List<DepreciationRecord> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (Long assetId : assetIds) {
            try {
                DepreciationRecord record = calculateAssetDepreciation(assetId, startDate, endDate, actualWorkUnits);
                if (record != null) {
                    results.add(record);
                    successCount++;
                }
            } catch (Exception e) {
                logger.error("资产折旧计算失败: {} 错误: {}", assetId, e.getMessage());
                failureCount++;
            }
        }

        logger.info("批量折旧计算完成: 成功: {} 失败: {}", successCount, failureCount);
        return results;
    }

    @Override
    @Transactional
    public List<DepreciationRecord> calculateBatchDepreciation(List<Long> assetIds, String depreciationMonth) {
        return calculateBatchDepreciation(assetIds, depreciationMonth, null);
    }

    @Override
    @Transactional
    public List<DepreciationRecord> calculateBatchDepreciation(List<Long> assetIds, String depreciationMonth,
            String depreciationMethod) {
        // 解析折旧月份，格式为 "yyyy-MM"
        LocalDate startDate = LocalDate.parse(depreciationMonth + "-01");
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<DepreciationRecord> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (Long assetId : assetIds) {
            try {
                DepreciationRecord record = calculateAssetDepreciation(assetId, startDate, endDate, depreciationMethod,
                        null);
                if (record != null) {
                    results.add(record);
                    successCount++;
                }
            } catch (Exception e) {
                logger.error("资产折旧计算失败: {} 错误: {}", assetId, e.getMessage());
                failureCount++;
            }
        }

        logger.info("批量折旧计算完成: 成功: {} 失败: {}", successCount, failureCount);
        return results;
    }

    @Override
    @Transactional
    public List<DepreciationRecord> calculateAllAssetsDepreciation(LocalDate startDate, LocalDate endDate) {
        List<Asset> allAssets = assetRepository.findAll();
        List<Long> assetIds = allAssets.stream().map(Asset::getId).toList();
        return calculateBatchDepreciation(assetIds, startDate, endDate);
    }

    @Override
    public DepreciationRecord getLatestDepreciationRecord(Long assetId) {
        return depreciationRecordRepository.findTopByAssetIdOrderByCreateTimeDesc(assetId).orElse(null);
    }

    @Override
    public List<DepreciationRecord> getDepreciationRecordsByAssetId(Long assetId) {
        return depreciationRecordRepository.findByAssetIdOrderByCreateTimeDesc(assetId);
    }

    @Override
    public List<DepreciationRecord> getDepreciationRecordsByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        return depreciationRecordRepository.findByCreateTimeBetween(startDateTime, endDateTime);
    }

    @Override
    public List<DepreciationRecord> getDepreciationRecordsByCategory(Long categoryId) {
        return depreciationRecordRepository.findByCategoryId(categoryId);
    }

    @Override
    public List<DepreciationRecord> getDepreciationRecordsByDepartment(Long departmentId) {
        return depreciationRecordRepository.findByDepartmentId(departmentId);
    }

    @Override
    public boolean validateDepreciationCalculation(Asset asset, DepreciationRecord record) {
        if (record.getDepreciationAmount() == null || record.getDepreciationAmount().compareTo(BigDecimal.ZERO) < 0) {
            logger.error("折旧金额不能为负数: {}", record.getDepreciationAmount());
            return false;
        }

        if (record.getCurrentNetValue() == null || record.getCurrentNetValue().compareTo(BigDecimal.ZERO) < 0) {
            logger.error("净值不能为负数: {}", record.getCurrentNetValue());
            return false;
        }

        // accumulatedDepreciation 已包含本期折旧，无需再次相加
        if (record.getAccumulatedDepreciation() == null
                || record.getAccumulatedDepreciation().compareTo(asset.getOriginalValue()) > 0) {
            logger.error("累计折旧不能超过原值: {} > {}",
                    record.getAccumulatedDepreciation(), asset.getOriginalValue());
            return false;
        }

        return true;
    }

    @Override
    @Transactional
    public void rollbackDepreciationRecord(Long recordId) {
        Optional<DepreciationRecord> recordOpt = depreciationRecordRepository.findById(recordId);
        if (recordOpt.isEmpty()) {
            throw new RuntimeException("折旧记录不存在: " + recordId);
        }

        DepreciationRecord record = recordOpt.get();

        Optional<Asset> assetOpt = assetRepository.findById(record.getAssetId());
        if (assetOpt.isEmpty()) {
            throw new RuntimeException("资产不存在: " + record.getAssetId());
        }

        Asset asset = assetOpt.get();

        BigDecimal newNetValue = record.getCurrentNetValue().add(record.getDepreciationAmount());
        asset.setNetValue(newNetValue);
        assetRepository.save(asset);

        depreciationRecordRepository.deleteById(recordId);

        logger.info("折旧记录回滚完成: 资产: {} 记录ID: {}", asset.getAssetNo(), recordId);
    }

    @Override
    public DepreciationRecord getDepreciationRecordById(Long recordId) {
        return depreciationRecordRepository.findById(recordId).orElse(null);
    }
}
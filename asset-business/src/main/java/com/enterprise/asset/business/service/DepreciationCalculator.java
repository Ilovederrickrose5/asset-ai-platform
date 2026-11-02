package com.enterprise.asset.business.service;

import com.enterprise.asset.business.entity.Asset;
import com.enterprise.asset.business.entity.DepreciationRecord;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 折旧计算器接口 - 定义不同折旧方法的计算逻辑（直线法/工作量法/双倍余额递减法/年数总和法） */
public interface DepreciationCalculator {

    /**
     * 计算折旧
     * @param asset 资产实体
     * @param periodStartDate 计算周期开始日期
     * @param periodEndDate 计算周期结束日期
     * @param accumulatedDepreciation 累计折旧额
     * @param usedMonths 已使用月份
     * @return 折旧记录
     */
    DepreciationRecord calculateDepreciation(
            Asset asset,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            BigDecimal accumulatedDepreciation,
            Integer usedMonths);

    /** 计算折旧（支持工作量法） */
    default DepreciationRecord calculateDepreciation(
            Asset asset,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            BigDecimal accumulatedDepreciation,
            Integer usedMonths,
            BigDecimal actualWorkUnits) {
        return calculateDepreciation(asset, periodStartDate, periodEndDate, accumulatedDepreciation, usedMonths);
    }

    /** 获取折旧方法标识 */
    String getDepreciationMethod();

    /** 计算资产净值 */
    BigDecimal calculateNetValue(BigDecimal originalValue, BigDecimal accumulatedDepreciation);
}
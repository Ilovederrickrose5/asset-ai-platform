package com.enterprise.asset.business.service.impl;

import com.enterprise.asset.business.entity.Asset;
import com.enterprise.asset.business.entity.DepreciationRecord;
import com.enterprise.asset.business.service.DepreciationCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class WorkUnitDepreciationCalculator implements DepreciationCalculator {

    private static final String DEPRECIATION_METHOD = "WORK_UNIT";
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final int DECIMAL_PLACES = 2;

    @Override
    public DepreciationRecord calculateDepreciation(
            Asset asset,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            BigDecimal accumulatedDepreciation,
            Integer usedMonths) {
        // 默认使用1个工作量单位
        return calculateDepreciation(asset, periodStartDate, periodEndDate, accumulatedDepreciation, usedMonths,
                BigDecimal.ONE);
    }

    @Override
    public DepreciationRecord calculateDepreciation(
            Asset asset,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            BigDecimal accumulatedDepreciation,
            Integer usedMonths,
            BigDecimal actualWorkUnits) {

        DepreciationRecord record = new DepreciationRecord();

        BigDecimal originalValue = asset.getOriginalValue() != null ? asset.getOriginalValue() : BigDecimal.ZERO;
        Integer usefulLife = asset.getUsefulLife() != null ? asset.getUsefulLife() : 0;
        BigDecimal accumulated = accumulatedDepreciation != null ? accumulatedDepreciation : BigDecimal.ZERO;

        if (usefulLife <= 0 || originalValue.compareTo(BigDecimal.ZERO) <= 0) {
            record.setDepreciationAmount(BigDecimal.ZERO);
            record.setCurrentNetValue(originalValue);
            record.setAccumulatedDepreciation(accumulated);
            return record;
        }

        // 使用资产的使用年限作为总工作量
        BigDecimal totalExpectedWorkUnits = new BigDecimal(usefulLife);

        // 确保实际工作量不为负数
        BigDecimal periodWorkUnits = actualWorkUnits != null && actualWorkUnits.compareTo(BigDecimal.ZERO) > 0
                ? actualWorkUnits
                : BigDecimal.ZERO;

        BigDecimal depreciationPerUnit = originalValue.divide(
                totalExpectedWorkUnits,
                DECIMAL_PLACES,
                ROUNDING_MODE);

        BigDecimal periodDepreciation = depreciationPerUnit.multiply(periodWorkUnits)
                .setScale(DECIMAL_PLACES, ROUNDING_MODE);

        BigDecimal newAccumulatedDepreciation = accumulated.add(periodDepreciation);
        BigDecimal currentNetValue = originalValue.subtract(newAccumulatedDepreciation);

        if (currentNetValue.compareTo(BigDecimal.ZERO) < 0) {
            currentNetValue = BigDecimal.ZERO;
            periodDepreciation = originalValue.subtract(accumulated);
            newAccumulatedDepreciation = originalValue;
        }

        record.setAssetId(asset.getId());
        record.setAssetNo(asset.getAssetNo());
        record.setAssetName(asset.getAssetName());
        record.setCategoryId(asset.getCategoryId());
        record.setDepartmentId(asset.getDeptId());
        record.setDepreciationMethod(DEPRECIATION_METHOD);
        record.setOriginalValue(originalValue);
        record.setCurrentNetValue(currentNetValue);
        record.setDepreciationAmount(periodDepreciation);
        record.setAccumulatedDepreciation(newAccumulatedDepreciation);
        record.setUsefulLife(usefulLife);
        int safeUsedMonths = usedMonths != null ? usedMonths : 0;
        record.setUsedMonths(safeUsedMonths + periodWorkUnits.intValue());
        record.setDepreciationPeriod(periodStartDate.toString() + "至" + periodEndDate.toString());
        record.setDepreciationMonth(periodStartDate.getYear() + "-" + String.format("%02d", periodStartDate.getMonthValue()));
        record.setNetValue(currentNetValue);
        record.setWorkUnits(periodWorkUnits);
        record.setTotalWorkUnits(totalExpectedWorkUnits);
        record.setStatus("COMPLETED");

        return record;
    }

    @Override
    public String getDepreciationMethod() {
        return DEPRECIATION_METHOD;
    }

    @Override
    public BigDecimal calculateNetValue(BigDecimal originalValue, BigDecimal accumulatedDepreciation) {
        BigDecimal netValue = originalValue.subtract(accumulatedDepreciation);
        return netValue.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : netValue;
    }
}
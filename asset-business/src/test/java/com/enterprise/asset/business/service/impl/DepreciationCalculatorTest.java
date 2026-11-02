package com.enterprise.asset.business.service.impl;

import com.enterprise.asset.business.entity.Asset;
import com.enterprise.asset.business.entity.DepreciationRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DepreciationCalculatorTest {

    private static final LocalDate JANUARY_START = LocalDate.of(2025, 1, 1);
    private static final LocalDate JANUARY_END = LocalDate.of(2025, 1, 31);

    private Asset asset(String originalValue, int usefulLifeMonths) {
        Asset asset = new Asset();
        asset.setId(1L);
        asset.setAssetNo("AST-TEST-001");
        asset.setAssetName("测试资产");
        asset.setOriginalValue(new BigDecimal(originalValue));
        asset.setUsefulLife(usefulLifeMonths);
        return asset;
    }

    @Test
    void straightLineUsesMonthsAsUsefulLifeUnit() {
        StraightLineDepreciationCalculator calculator = new StraightLineDepreciationCalculator();

        DepreciationRecord record = calculator.calculateDepreciation(
                asset("10000", 60), JANUARY_START, JANUARY_END, BigDecimal.ZERO, 0);

        assertEquals(new BigDecimal("166.67"), record.getDepreciationAmount());
        assertEquals(new BigDecimal("166.67"), record.getAccumulatedDepreciation());
        assertEquals(new BigDecimal("9833.33"), record.getCurrentNetValue());
        assertEquals(1, record.getUsedMonths());
        assertEquals("2025-01", record.getDepreciationMonth());
    }

    @Test
    void straightLineAccumulatesOnTopOfPreviousDepreciation() {
        StraightLineDepreciationCalculator calculator = new StraightLineDepreciationCalculator();

        DepreciationRecord record = calculator.calculateDepreciation(
                asset("10000", 60), JANUARY_START, JANUARY_END, new BigDecimal("5000.00"), 30);

        assertEquals(new BigDecimal("166.67"), record.getDepreciationAmount());
        assertEquals(new BigDecimal("5166.67"), record.getAccumulatedDepreciation());
        assertEquals(new BigDecimal("4833.33"), record.getCurrentNetValue());
        assertEquals(31, record.getUsedMonths());
    }

    @Test
    void straightLineNeverDepreciatesBelowZeroNetValue() {
        StraightLineDepreciationCalculator calculator = new StraightLineDepreciationCalculator();

        DepreciationRecord record = calculator.calculateDepreciation(
                asset("10000", 60), JANUARY_START, JANUARY_END, new BigDecimal("9833.33"), 59);

        assertEquals(new BigDecimal("166.67"), record.getDepreciationAmount());
        assertEquals(new BigDecimal("10000.00"), record.getAccumulatedDepreciation());
        assertEquals(BigDecimal.ZERO.setScale(2), record.getCurrentNetValue());
    }

    @Test
    void doubleDecliningBalanceUsesMonthlyRate() {
        DoubleDecliningBalanceDepreciationCalculator calculator =
                new DoubleDecliningBalanceDepreciationCalculator();

        DepreciationRecord record = calculator.calculateDepreciation(
                asset("12000", 60), JANUARY_START, JANUARY_END, BigDecimal.ZERO, 0);

        assertEquals(new BigDecimal("400.00"), record.getDepreciationAmount());
        assertEquals(new BigDecimal("400.00"), record.getAccumulatedDepreciation());
        assertEquals(new BigDecimal("11600.00"), record.getCurrentNetValue());
    }

    @Test
    void workUnitUsesActualWorkUnitsOfPeriod() {
        WorkUnitDepreciationCalculator calculator = new WorkUnitDepreciationCalculator();

        DepreciationRecord record = calculator.calculateDepreciation(
                asset("1000", 10), JANUARY_START, JANUARY_END, BigDecimal.ZERO, 0,
                new BigDecimal("2"));

        assertEquals(new BigDecimal("200.00"), record.getDepreciationAmount());
        assertEquals(new BigDecimal("200.00"), record.getAccumulatedDepreciation());
        assertEquals(new BigDecimal("800.00"), record.getCurrentNetValue());
        assertEquals(2, record.getUsedMonths());
    }

    @Test
    void calculatorsTolerateNullAccumulatedDepreciation() {
        StraightLineDepreciationCalculator calculator = new StraightLineDepreciationCalculator();

        DepreciationRecord record = calculator.calculateDepreciation(
                asset("10000", 60), JANUARY_START, JANUARY_END, null, null);

        assertEquals(new BigDecimal("166.67"), record.getDepreciationAmount());
        assertEquals(1, record.getUsedMonths());
    }
}

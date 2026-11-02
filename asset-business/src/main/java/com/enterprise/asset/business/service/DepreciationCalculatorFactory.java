package com.enterprise.asset.business.service;

import com.enterprise.asset.business.service.impl.DoubleDecliningBalanceDepreciationCalculator;
import com.enterprise.asset.business.service.impl.StraightLineDepreciationCalculator;
import com.enterprise.asset.business.service.impl.WorkUnitDepreciationCalculator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DepreciationCalculatorFactory {
    
    private final Map<String, DepreciationCalculator> calculators;
    
    public DepreciationCalculatorFactory() {
        this.calculators = new HashMap<>();
        this.calculators.put("STRAIGHT_LINE", new StraightLineDepreciationCalculator());
        this.calculators.put("WORK_UNIT", new WorkUnitDepreciationCalculator());
        this.calculators.put("DOUBLE_DECLINING_BALANCE", new DoubleDecliningBalanceDepreciationCalculator());
        this.calculators.put("DOUBLE_DECLINING", new DoubleDecliningBalanceDepreciationCalculator());
    }
    
    public DepreciationCalculator getCalculator(String method) {
        DepreciationCalculator calculator = calculators.get(method);
        if (calculator == null) {
            return new StraightLineDepreciationCalculator();
        }
        return calculator;
    }
    
    public boolean isValidMethod(String method) {
        return calculators.containsKey(method);
    }
}
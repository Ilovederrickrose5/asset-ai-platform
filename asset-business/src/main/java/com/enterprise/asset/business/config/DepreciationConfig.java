package com.enterprise.asset.business.config;

import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DepreciationConfig {

    public static class DepreciationMethodConfig {
        private String name;
        private String code;
        private Integer defaultUsefulLife;
        private BigDecimal defaultResidualRate;
        private String description;

        public DepreciationMethodConfig(String name, String code, Integer defaultUsefulLife, 
                                         BigDecimal defaultResidualRate, String description) {
            this.name = name;
            this.code = code;
            this.defaultUsefulLife = defaultUsefulLife;
            this.defaultResidualRate = defaultResidualRate;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }

        public Integer getDefaultUsefulLife() {
            return defaultUsefulLife;
        }

        public BigDecimal getDefaultResidualRate() {
            return defaultResidualRate;
        }

        public String getDescription() {
            return description;
        }
    }

    private static final Map<String, DepreciationMethodConfig> DEPRECIATION_METHODS = new HashMap<>();

    static {
        DEPRECIATION_METHODS.put("STRAIGHT_LINE", new DepreciationMethodConfig(
            "直线法",
            "STRAIGHT_LINE",
            60,
            new BigDecimal("0.05"),
            "按月平均折旧，折旧额 = (原值 - 残值) / 使用月数"
        ));

        DEPRECIATION_METHODS.put("DOUBLE_DECLINING", new DepreciationMethodConfig(
            "双倍余额递减法",
            "DOUBLE_DECLINING",
            60,
            new BigDecimal("0.05"),
            "双倍直线折旧率计算，最后两年改为直线法"
        ));

        DEPRECIATION_METHODS.put("SUM_OF_YEARS", new DepreciationMethodConfig(
            "年数总和法",
            "SUM_OF_YEARS",
            60,
            new BigDecimal("0.05"),
            "按使用年数比例计算折旧"
        ));

        DEPRECIATION_METHODS.put("WORK_UNIT", new DepreciationMethodConfig(
            "工作量法",
            "WORK_UNIT",
            60,
            new BigDecimal("0.05"),
            "按实际工作量计算折旧"
        ));
    }

    public static DepreciationMethodConfig getDepreciationMethodConfig(String methodCode) {
        return DEPRECIATION_METHODS.get(methodCode);
    }

    public static Map<String, DepreciationMethodConfig> getAllDepreciationMethods() {
        return new HashMap<>(DEPRECIATION_METHODS);
    }

    public static String getDefaultDepreciationMethod() {
        return "STRAIGHT_LINE";
    }

    public static Integer getDefaultUsefulLife(String methodCode) {
        DepreciationMethodConfig config = DEPRECIATION_METHODS.get(methodCode);
        return config != null ? config.getDefaultUsefulLife() : 60;
    }

    public static BigDecimal getDefaultResidualRate(String methodCode) {
        DepreciationMethodConfig config = DEPRECIATION_METHODS.get(methodCode);
        return config != null ? config.getDefaultResidualRate() : new BigDecimal("0.05");
    }
}
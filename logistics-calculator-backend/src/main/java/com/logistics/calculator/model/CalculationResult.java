package com.logistics.calculator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

/**
 * 物流计算结果实体类
 * 用于返回物流费用计算的详细结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculationResult {
    
    /**
     * 总费用（单位：人民币元）
     */
    private BigDecimal totalCost;
    
    /**
     * 单件商品分摊的物流成本（单位：人民币元）
     */
    private BigDecimal costPerItem;
    
    /**
     * 费用明细列表（包含各项费用的详细说明）
     */
    private List<FeeDetail> feeDetails;
    
    /**
     * 警告信息列表（如体积重量提示、超大件警告等）
     */
    private List<String> warnings;
    
    /**
     * 计算记录唯一标识
     */
    private String calculationId;
    
    /**
     * 费用明细内部类
     * 用于描述每一项费用的详细信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeeDetail {
        /**
         * 费用名称（如：基础运费、燃油附加费等）
         */
        private String feeName;
        
        /**
         * 费用描述（说明该费用的计算依据）
         */
        private String description;
        
        /**
         * 费用金额（单位：人民币元）
         */
        private BigDecimal amount;
        
        /**
         * 费用单位（如：CNY、kg、CBM等）
         */
        private String unit;
    }
}

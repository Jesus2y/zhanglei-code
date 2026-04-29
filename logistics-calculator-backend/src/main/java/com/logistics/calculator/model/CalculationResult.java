package com.logistics.calculator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculationResult {
    
    private BigDecimal totalCost;
    
    private BigDecimal costPerItem;
    
    private List<FeeDetail> feeDetails;
    
    private List<String> warnings;
    
    private String calculationId;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeeDetail {
        private String feeName;
        private String description;
        private BigDecimal amount;
        private String unit;
    }
}

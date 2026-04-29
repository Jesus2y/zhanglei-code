package com.logistics.calculator.service;

import com.logistics.calculator.model.CalculationRequest;
import com.logistics.calculator.model.CalculationResult;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class LogisticsCalculationService {
    
    private static final BigDecimal AIR_VOLUME_DIVISOR = new BigDecimal("6000");
    private static final BigDecimal SEA_VOLUME_DIVISOR = new BigDecimal("1000000");
    private static final BigDecimal MIN_AIR_WEIGHT = new BigDecimal("0.5");
    
    public CalculationResult calculate(CalculationRequest request) {
        List<CalculationResult.FeeDetail> feeDetails = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        BigDecimal volumeWeight = calculateVolumeWeight(request);
        BigDecimal chargeableWeight = determineChargeableWeight(request, volumeWeight);
        BigDecimal volumeCBM = calculateVolumeCBM(request);
        
        BigDecimal baseFreight = calculateBaseFreight(request, chargeableWeight, volumeCBM);
        feeDetails.add(createFeeDetail("基础运费", "按计费重量计算", baseFreight, "CNY"));
        
        BigDecimal fuelSurcharge = calculateFuelSurcharge(baseFreight, request.getCountry());
        feeDetails.add(createFeeDetail("燃油附加费", "基础运费的百分比", fuelSurcharge, "CNY"));
        
        BigDecimal destinationFee = calculateDestinationFee(request, chargeableWeight, volumeCBM);
        feeDetails.add(createFeeDetail("目的港杂费", "目的地相关费用", destinationFee, "CNY"));
        
        BigDecimal customsFee = BigDecimal.ZERO;
        if ("SEA".equals(request.getShippingMethod())) {
            customsFee = calculateCustomsFee(request.getCountry());
            feeDetails.add(createFeeDetail("清关费用", "海关手续费", customsFee, "CNY"));
        }
        
        BigDecimal warehouseFee = calculateWarehouseFee(request, chargeableWeight);
        feeDetails.add(createFeeDetail("海外仓操作费", "仓库处理费用", warehouseFee, "CNY"));
        
        BigDecimal residentialFee = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(request.getResidentialDelivery())) {
            residentialFee = calculateResidentialFee(request.getCountry());
            feeDetails.add(createFeeDetail("住宅配送费", "住宅地址配送", residentialFee, "CNY"));
        }
        
        BigDecimal insuranceFee = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(request.getInsurance())) {
            insuranceFee = calculateInsuranceFee(baseFreight);
            feeDetails.add(createFeeDetail("保险费", "货物保险", insuranceFee, "CNY"));
        }
        
        BigDecimal totalCost = baseFreight.add(fuelSurcharge).add(destinationFee)
                .add(customsFee).add(warehouseFee).add(residentialFee).add(insuranceFee);
        
        BigDecimal costPerItem = totalCost.divide(
                new BigDecimal(request.getQuantity()), 2, RoundingMode.HALF_UP);
        
        generateWarnings(request, chargeableWeight, volumeWeight, warnings);
        
        return CalculationResult.builder()
                .totalCost(totalCost.setScale(2, RoundingMode.HALF_UP))
                .costPerItem(costPerItem)
                .feeDetails(feeDetails)
                .warnings(warnings)
                .build();
    }
    
    private BigDecimal calculateVolumeWeight(CalculationRequest request) {
        BigDecimal volume = request.getLength()
                .multiply(request.getWidth())
                .multiply(request.getHeight());
        
        if ("AIR".equals(request.getShippingMethod())) {
            return volume.divide(AIR_VOLUME_DIVISOR, 2, RoundingMode.HALF_UP);
        } else {
            return volume.divide(SEA_VOLUME_DIVISOR, 4, RoundingMode.HALF_UP);
        }
    }
    
    private BigDecimal determineChargeableWeight(CalculationRequest request, BigDecimal volumeWeight) {
        if ("AIR".equals(request.getShippingMethod())) {
            BigDecimal actualWeight = request.getWeight()
                    .multiply(new BigDecimal(request.getQuantity()));
            BigDecimal chargeable = actualWeight.max(volumeWeight);
            return chargeable.max(MIN_AIR_WEIGHT);
        } else {
            return calculateVolumeCBM(request);
        }
    }
    
    private BigDecimal calculateVolumeCBM(CalculationRequest request) {
        BigDecimal volume = request.getLength()
                .multiply(request.getWidth())
                .multiply(request.getHeight());
        return volume.divide(SEA_VOLUME_DIVISOR, 4, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateBaseFreight(CalculationRequest request, 
                                           BigDecimal chargeableWeight, 
                                           BigDecimal volumeCBM) {
        BigDecimal rate = getFreightRate(request);
        
        if ("AIR".equals(request.getShippingMethod())) {
            return chargeableWeight.multiply(rate);
        } else {
            return volumeCBM.multiply(rate);
        }
    }
    
    private BigDecimal getFreightRate(CalculationRequest request) {
        String key = request.getCountry() + "_" + request.getShippingMethod();
        
        return switch (key) {
            case "US_AIR" -> new BigDecimal("45.00");
            case "US_SEA" -> new BigDecimal("1200.00");
            case "DE_AIR" -> new BigDecimal("52.00");
            case "DE_SEA" -> new BigDecimal("1400.00");
            case "JP_AIR" -> new BigDecimal("38.00");
            case "JP_SEA" -> new BigDecimal("1000.00");
            default -> new BigDecimal("50.00");
        };
    }
    
    private BigDecimal calculateFuelSurcharge(BigDecimal baseFreight, String country) {
        BigDecimal rate = switch (country) {
            case "US" -> new BigDecimal("0.15");
            case "DE" -> new BigDecimal("0.18");
            case "JP" -> new BigDecimal("0.12");
            default -> new BigDecimal("0.15");
        };
        return baseFreight.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateDestinationFee(CalculationRequest request, 
                                              BigDecimal chargeableWeight, 
                                              BigDecimal volumeCBM) {
        BigDecimal rate = switch (request.getCountry()) {
            case "US" -> new BigDecimal("8.00");
            case "DE" -> new BigDecimal("12.00");
            case "JP" -> new BigDecimal("10.00");
            default -> new BigDecimal("10.00");
        };
        
        if ("AIR".equals(request.getShippingMethod())) {
            return chargeableWeight.multiply(rate);
        } else {
            return volumeCBM.multiply(rate.multiply(new BigDecimal("100")));
        }
    }
    
    private BigDecimal calculateCustomsFee(String country) {
        return switch (country) {
            case "US" -> new BigDecimal("50.00");
            case "DE" -> new BigDecimal("75.00");
            case "JP" -> new BigDecimal("60.00");
            default -> new BigDecimal("60.00");
        };
    }
    
    private BigDecimal calculateWarehouseFee(CalculationRequest request, BigDecimal chargeableWeight) {
        BigDecimal rate = new BigDecimal("2.50");
        return chargeableWeight.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateResidentialFee(String country) {
        return switch (country) {
            case "US" -> new BigDecimal("15.00");
            case "DE" -> new BigDecimal("20.00");
            case "JP" -> new BigDecimal("18.00");
            default -> new BigDecimal("15.00");
        };
    }
    
    private BigDecimal calculateInsuranceFee(BigDecimal baseFreight) {
        return baseFreight.multiply(new BigDecimal("0.03")).setScale(2, RoundingMode.HALF_UP);
    }
    
    private void generateWarnings(CalculationRequest request, 
                                 BigDecimal chargeableWeight, 
                                 BigDecimal volumeWeight,
                                 List<String> warnings) {
        if (volumeWeight.compareTo(request.getWeight()) > 0) {
            warnings.add("注意：体积重量(" + volumeWeight + "kg)大于实际重量，将按体积重量计费");
        }
        
        if ("AIR".equals(request.getShippingMethod()) && 
            chargeableWeight.compareTo(new BigDecimal("100")) > 0) {
            warnings.add("建议：货物超过100kg，考虑使用海运可能更经济");
        }
        
        if (request.getLength().compareTo(new BigDecimal("120")) > 0 ||
            request.getWidth().compareTo(new BigDecimal("80")) > 0 ||
            request.getHeight().compareTo(new BigDecimal("80")) > 0) {
            warnings.add("警告：尺寸超出标准，可能产生超大件附加费");
        }
    }
    
    private CalculationResult.FeeDetail createFeeDetail(String name, String desc, 
                                                        BigDecimal amount, String unit) {
        return CalculationResult.FeeDetail.builder()
                .feeName(name)
                .description(desc)
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .unit(unit)
                .build();
    }
}

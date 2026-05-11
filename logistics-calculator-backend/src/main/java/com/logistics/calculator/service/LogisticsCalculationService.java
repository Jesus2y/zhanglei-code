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
        boolean isAir = "AIR".equals(request.getShippingMethod());
        boolean isSensitive = "SENSITIVE".equals(request.getGoodsType());
        boolean needQuote = !isAir && isSensitive; // 海运敏感货需单独询价
        
        BigDecimal volumeWeight = calculateVolumeWeight(request);
        BigDecimal chargeableWeight = determineChargeableWeight(request, volumeWeight);
        BigDecimal volumeCBM = calculateVolumeCBM(request);
        
        // 获取基础运费费率区间
        RateRange baseRateRange = getFreightRateRange(request);
        
        // 基础运费区间计算
        BigDecimal baseFreightMin, baseFreightMax;
        if (isAir) {
            baseFreightMin = chargeableWeight.multiply(baseRateRange.min);
            baseFreightMax = chargeableWeight.multiply(baseRateRange.max);
        } else {
            baseFreightMin = volumeCBM.multiply(baseRateRange.min);
            baseFreightMax = volumeCBM.multiply(baseRateRange.max);
        }
        feeDetails.add(createFeeDetail("基础运费", getBaseFreightDesc(request), baseFreightMin, baseFreightMax, "CNY"));
        
        // 燃油附加费区间（按基础运费的百分比）
        BigDecimal fuelMin = calculateFuelSurcharge(baseFreightMin, request.getCountry());
        BigDecimal fuelMax = calculateFuelSurcharge(baseFreightMax, request.getCountry());
        feeDetails.add(createFeeDetail("燃油附加费", "基础运费的百分比", fuelMin, fuelMax, "CNY"));
        
        // 目的港杂费
        BigDecimal destMin = calculateDestinationFee(request, chargeableWeight, volumeCBM, true);
        BigDecimal destMax = calculateDestinationFee(request, chargeableWeight, volumeCBM, false);
        feeDetails.add(createFeeDetail("目的港杂费", "目的地相关费用", destMin, destMax, "CNY"));
        
        // 清关费用（仅海运，固定费用无区间）
        BigDecimal customsMin = BigDecimal.ZERO, customsMax = BigDecimal.ZERO;
        if (!isAir) {
            customsMin = customsMax = calculateCustomsFee(request.getCountry(), isSensitive);
            feeDetails.add(createFeeDetail("清关费用", "海关手续费", customsMin, customsMax, "CNY"));
        }
        
        // 海外仓操作费（按计费重量）
        BigDecimal warehouseMin = chargeableWeight.multiply(new BigDecimal("2.00")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal warehouseMax = chargeableWeight.multiply(new BigDecimal("3.50")).setScale(2, RoundingMode.HALF_UP);
        feeDetails.add(createFeeDetail("海外仓操作费", "仓库处理费用", warehouseMin, warehouseMax, "CNY"));
        
        // 住宅配送费（固定费用）
        BigDecimal residentialMin = BigDecimal.ZERO, residentialMax = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(request.getResidentialDelivery())) {
            BigDecimal resFee = calculateResidentialFee(request.getCountry());
            residentialMin = residentialMax = resFee;
            feeDetails.add(createFeeDetail("住宅配送费", "住宅地址配送", resFee, resFee, "CNY"));
        }
        
        // 保险费（按基础运费百分比）
        BigDecimal insuranceMin = BigDecimal.ZERO, insuranceMax = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(request.getInsurance())) {
            insuranceMin = calculateInsuranceFee(baseFreightMin);
            insuranceMax = calculateInsuranceFee(baseFreightMax);
            feeDetails.add(createFeeDetail("保险费", "货物保险(保额的3%)", insuranceMin, insuranceMax, "CNY"));
        }
        
        // 总费用区间汇总
        BigDecimal totalCostMin = baseFreightMin.add(fuelMin).add(destMin)
                .add(customsMin).add(warehouseMin).add(residentialMin).add(insuranceMin)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCostMax = baseFreightMax.add(fuelMax).add(destMax)
                .add(customsMax).add(warehouseMax).add(residentialMax).add(insuranceMax)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCostMid = totalCostMin.add(totalCostMax)
                .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        
        // 单件分摊成本
        int qty = request.getQuantity();
        BigDecimal costPerItemMin = totalCostMin.divide(new BigDecimal(qty), 2, RoundingMode.HALF_UP);
        BigDecimal costPerItemMax = totalCostMax.divide(new BigDecimal(qty), 2, RoundingMode.HALF_UP);
        
        // 预估时效
        String deliveryTime = getDeliveryTime(isAir, isSensitive);
        
        generateWarnings(request, chargeableWeight, volumeWeight, warnings, needQuote);
        
        return CalculationResult.builder()
                .totalCostMin(totalCostMin)
                .totalCostMax(totalCostMax)
                .totalCost(totalCostMid)
                .costPerItemMin(costPerItemMin)
                .costPerItemMax(costPerItemMax)
                .feeDetails(feeDetails)
                .warnings(warnings)
                .deliveryTime(deliveryTime)
                .goodsType(request.getGoodsType())
                .needQuote(needQuote)
                .build();
    }
    
    /**
     * 费率区间内部类
     */
    private static class RateRange {
        BigDecimal min;
        BigDecimal max;
        RateRange(BigDecimal min, BigDecimal max) { this.min = min; this.max = max; }
    }
    
    /**
     * 根据运输方式和货物类型获取费率区间（元/kg 或 元/CBM）
     * 参考：2026年中国至美国物流价格参考表
     * 
     * 空运普货:   30-45元/kg    空运敏感货: 40-65元/kg
     * 海运普货:   5-20元/kg     海运敏感货: 需单独询价(上浮30%-50%)
     */
    private RateRange getFreightRateRange(CalculationRequest request) {
        boolean isAir = "AIR".equals(request.getShippingMethod());
        boolean isSensitive = "SENSITIVE".equals(request.getGoodsType());
        
        if (isAir) {
            if (isSensitive) {
                return new RateRange(new BigDecimal("40"), new BigDecimal("65")); // 空运敏感货 40-65元/kg
            } else {
                return new RateRange(new BigDecimal("30"), new BigDecimal("45")); // 空运普货 30-45元/kg
            }
        } else {
            if (isSensitive) {
                // 海运敏感货：在普货基础上上浮30%-50%，按7.5-30元/kg估算
                return new RateRange(new BigDecimal("7.50"), new BigDecimal("30.00"));
            } else {
                return new RateRange(new BigDecimal("5"), new BigDecimal("20")); // 海运普货 5-20元/kg
            }
        }
    }
    
    private String getBaseFreightDesc(CalculationRequest request) {
        String method = "AIR".equals(request.getShippingMethod()) ? "空运" : "海运";
        String goodsType = "SENSITIVE".equals(request.getGoodsType()) ? "敏感货" : "普货";
        String unit = "AIR".equals(request.getShippingMethod()) ? "/kg" : "/kg";
        RateRange range = getFreightRateRange(request);
        return method + goodsType + "费率" + range.min + "-" + range.max + unit;
    }
    
    private String getDeliveryTime(boolean isAir, boolean isSensitive) {
        if (isAir) {
            return isSensitive ? "6-18个工作日" : "5-15个工作日";
        } else {
            return isSensitive ? "30-50个工作日（需单独询价）" : "25-45个工作日";
        }
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
            // 海运按体积重计费
            return request.getWeight().multiply(new BigDecimal(request.getQuantity())).max(volumeWeight);
        }
    }
    
    private BigDecimal calculateVolumeCBM(CalculationRequest request) {
        BigDecimal volume = request.getLength()
                .multiply(request.getWidth())
                .multiply(request.getHeight());
        return volume.divide(SEA_VOLUME_DIVISOR, 4, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateFuelSurcharge(BigDecimal baseFreight, String country) {
        BigDecimal rate = switch (country) {
            case "US" -> new BigDecimal("0.15");   // 15%
            case "DE" -> new BigDecimal("0.18");   // 18%
            case "JP" -> new BigDecimal("0.12");   // 12%
            default -> new BigDecimal("0.15");
        };
        return baseFreight.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateDestinationFee(CalculationRequest request, 
                                              BigDecimal chargeableWeight, 
                                              BigDecimal volumeCBM,
                                              boolean useMinRate) {
        BigDecimal rate = switch (request.getCountry()) {
            case "US" -> new BigDecimal("8.00");
            case "DE" -> new BigDecimal("12.00");
            case "JP" -> new BigDecimal("10.00");
            default -> new BigDecimal("10.00");
        };
        
        if ("AIR".equals(request.getShippingMethod())) {
            // 空运目的港费也有一定浮动范围
            BigDecimal adjustedRate = useMinRate ? rate.multiply(new BigDecimal("0.9")) : rate.multiply(new BigDecimal("1.1"));
            return chargeableWeight.multiply(adjustedRate).setScale(2, RoundingMode.HALF_UP);
        } else {
            BigDecimal cbmRate = rate.multiply(new BigDecimal("100"));
            BigDecimal adjustedCbmRate = useMinRate ? cbmRate.multiply(new BigDecimal("0.85")) : cbmRate.multiply(new BigDecimal("1.15"));
            return volumeCBM.multiply(adjustedCbmRate).setScale(2, RoundingMode.HALF_UP);
        }
    }
    
    private BigDecimal calculateCustomsFee(String country, boolean isSensitive) {
        BigDecimal baseFee = switch (country) {
            case "US" -> new BigDecimal("50.00");
            case "DE" -> new BigDecimal("75.00");
            case "JP" -> new BigDecimal("60.00");
            default -> new BigDecimal("60.00");
        };
        // 敏感货清关费略高
        return isSensitive ? baseFee.multiply(new BigDecimal("1.3")) : baseFee;
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
                                 List<String> warnings,
                                 boolean needQuote) {
        if (volumeWeight.compareTo(request.getWeight().multiply(new BigDecimal(request.getQuantity()))) > 0) {
            warnings.add("注意：体积重量(" + volumeWeight.setScale(2, RoundingMode.HALF_UP) + "kg)大于实际重量，将按体积重量计费");
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
        
        if ("SENSITIVE".equals(request.getGoodsType())) {
            warnings.add("提示：敏感货（带电、化妆品等）可能需要额外提供MSDS/鉴定报告");
        }
        
        if (needQuote) {
            warnings.add("重要：海运敏感货价格为预估值，实际价格需单独询价确认（通常上浮30%-50%）");
        }
    }
    
    private CalculationResult.FeeDetail createFeeDetail(String name, String desc, 
                                                        BigDecimal amountMin, 
                                                        BigDecimal amountMax,
                                                        String unit) {
        BigDecimal mid = amountMin.add(amountMax)
                .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        return CalculationResult.FeeDetail.builder()
                .feeName(name)
                .description(desc)
                .amountMin(amountMin.setScale(2, RoundingMode.HALF_UP))
                .amountMax(amountMax.setScale(2, RoundingMode.HALF_UP))
                .amount(mid)
                .unit(unit)
                .build();
    }
}

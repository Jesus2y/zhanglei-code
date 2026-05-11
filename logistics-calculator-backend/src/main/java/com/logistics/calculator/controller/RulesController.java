package com.logistics.calculator.controller;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 物流规则查询控制器
 * 提供各国物流计费规则和参数的查询接口（支持普货/敏感货价格区间）
 */
@RestController
@RequestMapping("/api/v1")
public class RulesController {

    /**
     * 获取指定国家的物流计费规则
     * 返回空运和海运的费率区间、体积重量除数等参数
     * 
     * @param country 国家代码（US/DE/JP）
     * @return 物流规则信息（包含空运/海运普货/敏感货费率区间、体积除数、支持的国家列表等）
     */
    @GetMapping("/rules/{country}")
    public Map<String, Object> getRules(@PathVariable String country) {
        // 空运费率区间 (元/kg)
        Map<String, Object> airNormalRate = new HashMap<>();
        airNormalRate.put("min", 30);
        airNormalRate.put("max", 45);
        airNormalRate.put("unit", "元/kg");
        airNormalRate.put("deliveryTime", "5-15个工作日");
        
        Map<String, Object> airSensitiveRate = new HashMap<>();
        airSensitiveRate.put("min", 40);
        airSensitiveRate.put("max", 65);
        airSensitiveRate.put("unit", "元/kg");
        airSensitiveRate.put("deliveryTime", "6-18个工作日");

        // 海运费率区间 (元/kg)
        Map<String, Object> seaNormalRate = new HashMap<>();
        seaNormalRate.put("min", 5);
        seaNormalRate.put("max", 20);
        seaNormalRate.put("unit", "元/kg");
        seaNormalRate.put("deliveryTime", "25-45个工作日");
        
        Map<String, Object> seaSensitiveRate = new HashMap<>();
        seaSensitiveRate.put("desc", "需单独询价（通常在普货基础上上浮30%-50%）");
        seaSensitiveRate.put("deliveryTime", "30-50个工作日");
        seaSensitiveRate.put("needQuote", true);

        Map<String, Object> airRates = new HashMap<>();
        airRates.put("NORMAL", airNormalRate);
        airRates.put("SENSITIVE", airSensitiveRate);

        Map<String, Object> seaRates = new HashMap<>();
        seaRates.put("NORMAL", seaNormalRate);
        seaRates.put("SENSITIVE", seaSensitiveRate);

        // 货物类型说明
        Map<String, String> goodsTypes = new HashMap<>();
        goodsTypes.put("NORMAL", "普货（服饰、家居等常规物品）");
        goodsTypes.put("SENSITIVE", "敏感货（带电产品、化妆品、液体等特殊物品）");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("country", country);
        result.put("airVolumeDivisor", 6000);       // 空运体积除数
        result.put("seaVolumeDivisor", 1000000);     // 海运体积除数
        result.put("minAirWeight", 0.5);             // 最小计费重量(kg)
        result.put("airRates", airRates);
        result.put("seaRates", seaRates);
        result.put("goodsTypes", goodsTypes);
        result.put("supportedCountries", List.of("US", "DE", "JP"));
        return result;
    }
}

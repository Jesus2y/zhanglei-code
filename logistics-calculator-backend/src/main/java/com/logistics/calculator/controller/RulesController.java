package com.logistics.calculator.controller;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 物流规则查询控制器
 * 提供各国物流计费规则和参数的查询接口
 */
@RestController
@RequestMapping("/api/v1")
public class RulesController {

    /**
     * 获取指定国家的物流计费规则
     * 返回空运和海运的费率、体积重量除数等参数
     * 
     * @param country 国家代码（US/DE/JP）
     * @return 物流规则信息（包含空运/海运费率、体积除数、支持的国家列表等）
     */
    @GetMapping("/rules/{country}")
    public Map<String, Object> getRules(@PathVariable String country) {
        Map<String, BigDecimal> airRate = new HashMap<>();
        airRate.put("US", new BigDecimal("45.00"));
        airRate.put("DE", new BigDecimal("52.00"));
        airRate.put("JP", new BigDecimal("38.00"));

        Map<String, BigDecimal> seaRate = new HashMap<>();
        seaRate.put("US", new BigDecimal("1200.00"));
        seaRate.put("DE", new BigDecimal("1400.00"));
        seaRate.put("JP", new BigDecimal("1000.00"));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("country", country);
        result.put("airVolumeDivisor", 6000);
        result.put("seaVolumeDivisor", 1000000);
        result.put("minAirWeight", 0.5);
        result.put("airRate", airRate.getOrDefault(country, new BigDecimal("50.00")));
        result.put("seaRate", seaRate.getOrDefault(country, new BigDecimal("1200.00")));
        result.put("supportedCountries", new String[]{"US", "DE", "JP"});
        return result;
    }
}


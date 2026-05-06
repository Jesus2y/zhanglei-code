package com.logistics.calculator.model;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 物流计算请求实体类
 * 用于接收前端传来的货物信息和物流参数
 */
@Data
public class CalculationRequest {
    
    /**
     * 货物长度（单位：厘米）
     * 最小值：0.1
     */
    @NotNull(message = "长度不能为空")
    @DecimalMin(value = "0.1", message = "长度必须大于0")
    private BigDecimal length;
    
    /**
     * 货物宽度（单位：厘米）
     * 最小值：0.1
     */
    @NotNull(message = "宽度不能为空")
    @DecimalMin(value = "0.1", message = "宽度必须大于0")
    private BigDecimal width;
    
    /**
     * 货物高度（单位：厘米）
     * 最小值：0.1
     */
    @NotNull(message = "高度不能为空")
    @DecimalMin(value = "0.1", message = "高度必须大于0")
    private BigDecimal height;
    
    /**
     * 货物单件重量（单位：千克）
     * 最小值：0.1
     */
    @NotNull(message = "重量不能为空")
    @DecimalMin(value = "0.1", message = "重量必须大于0")
    private BigDecimal weight;
    
    /**
     * 货物数量
     * 最小值：1
     */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;
    
    /**
     * 目的国家代码（US-美国，DE-德国，JP-日本）
     */
    @NotBlank(message = "国家不能为空")
    @Pattern(regexp = "^(US|DE|JP)$", message = "只支持美国(US)、德国(DE)、日本(JP)")
    private String country;
    
    /**
     * 物流方式（AIR-空运，SEA-海运）
     */
    @NotBlank(message = "物流方式不能为空")
    @Pattern(regexp = "^(AIR|SEA)$", message = "物流方式只能为空派(AIR)或海运(SEA)")
    private String shippingMethod;
    
    /**
     * 是否需要住宅配送（默认不需要）
     */
    private Boolean residentialDelivery = false;
    
    /**
     * 是否需要购买保险（默认不需要）
     */
    private Boolean insurance = false;
}

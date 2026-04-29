package com.logistics.calculator.model;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CalculationRequest {
    
    @NotNull(message = "长度不能为空")
    @DecimalMin(value = "0.1", message = "长度必须大于0")
    private BigDecimal length;
    
    @NotNull(message = "宽度不能为空")
    @DecimalMin(value = "0.1", message = "宽度必须大于0")
    private BigDecimal width;
    
    @NotNull(message = "高度不能为空")
    @DecimalMin(value = "0.1", message = "高度必须大于0")
    private BigDecimal height;
    
    @NotNull(message = "重量不能为空")
    @DecimalMin(value = "0.1", message = "重量必须大于0")
    private BigDecimal weight;
    
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;
    
    @NotBlank(message = "国家不能为空")
    @Pattern(regexp = "^(US|DE|JP)$", message = "只支持美国(US)、德国(DE)、日本(JP)")
    private String country;
    
    @NotBlank(message = "物流方式不能为空")
    @Pattern(regexp = "^(AIR|SEA)$", message = "物流方式只能为空派(AIR)或海运(SEA)")
    private String shippingMethod;
    
    private Boolean residentialDelivery = false;
    
    private Boolean insurance = false;
}

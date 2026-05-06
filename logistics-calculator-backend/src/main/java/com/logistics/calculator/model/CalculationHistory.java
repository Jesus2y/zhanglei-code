package com.logistics.calculator.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 计算历史记录实体类
 * 用于保存用户的物流计算历史记录
 */
@Entity
@Table(name = "calculation_history")
@Data
public class CalculationHistory {
    
    /**
     * 历史记录唯一标识（自增主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID（关联users表）
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * 计算请求数据（JSON格式字符串，保存完整的计算参数）
     */
    @Column(nullable = false)
    private String calculationData;
    
    /**
     * 计算得出的总费用（单位：人民币元）
     */
    @Column(nullable = false)
    private BigDecimal totalCost;
    
    /**
     * 目的国家代码（US/DE/JP）
     */
    @Column(nullable = false, length = 10)
    private String country;
    
    /**
     * 物流方式（AIR/SEA）
     */
    @Column(nullable = false, length = 10)
    private String shippingMethod;
    
    /**
     * 记录创建时间（自动填充）
     */
    @CreationTimestamp
    private LocalDateTime createdAt;
}

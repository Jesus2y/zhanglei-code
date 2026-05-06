package com.logistics.calculator.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体类
 * 用于保存会员购买的订单和支付信息
 */
@Entity
@Table(name = "payment_records")
@Data
public class PaymentRecord {
    
    /**
     * 支付记录唯一标识（自增主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 订单号（唯一标识，格式：M+时间戳）
     */
    @Column(nullable = false, length = 50)
    private String orderNo;
    
    /**
     * 用户ID（关联users表）
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * 支付金额（单位：人民币元）
     */
    @Column(nullable = false)
    private BigDecimal amount;
    
    /**
     * 货币类型（默认：CNY-人民币）
     */
    @Column(nullable = false, length = 20)
    private String currency = "CNY";
    
    /**
     * 订单状态（CREATED-已创建，SUCCESS-支付成功，FAILED-支付失败）
     */
    @Column(nullable = false, length = 20)
    private String status;
    
    /**
     * 微信支付交易号（支付成功后由微信返回）
     */
    @Column(length = 100)
    private String transactionId;
    
    /**
     * 订单创建时间（自动填充）
     */
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    /**
     * 支付完成时间
     */
    private LocalDateTime paidAt;
}

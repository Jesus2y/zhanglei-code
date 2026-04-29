package com.logistics.calculator.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_records")
@Data
public class PaymentRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String orderNo;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(nullable = false, length = 20)
    private String currency = "CNY";
    
    @Column(nullable = false, length = 20)
    private String status;
    
    @Column(length = 100)
    private String transactionId;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    private LocalDateTime paidAt;
}

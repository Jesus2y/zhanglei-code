package com.logistics.calculator.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "calculation_history")
@Data
public class CalculationHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String calculationData;
    
    @Column(nullable = false)
    private BigDecimal totalCost;
    
    @Column(nullable = false, length = 10)
    private String country;
    
    @Column(nullable = false, length = 10)
    private String shippingMethod;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}

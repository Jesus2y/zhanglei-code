package com.logistics.calculator.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String openid;
    
    @Column(length = 50)
    private String nickname;
    
    @Column(length = 200)
    private String avatarUrl;
    
    @Column(nullable = false)
    private Boolean isMember = false;
    
    @Column(name = "membership_expire_date")
    private LocalDateTime membershipExpireDate;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "last_calculation_date")
    private LocalDateTime lastCalculationDate;
    
    @Column(name = "calculation_count_today")
    private Integer calculationCountToday = 0;
}

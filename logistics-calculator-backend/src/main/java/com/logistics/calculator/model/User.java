package com.logistics.calculator.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 用于存储微信小程序用户信息和会员状态
 */
@Entity
@Table(name = "users")
@Data
public class User {
    
    /**
     * 用户唯一标识（自增主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 微信用户OpenID（唯一标识）
     */
    @Column(unique = true, nullable = false)
    private String openid;
    
    /**
     * 用户昵称
     */
    @Column(length = 50)
    private String nickname;
    
    /**
     * 用户头像URL
     */
    @Column(length = 200)
    private String avatarUrl;
    
    /**
     * 用户手机号码（可选，用于会员联系和订单通知）
     */
    @Column(length = 20)
    private String phoneNumber;
    
    /**
     * 是否为会员（true-会员，false-普通用户）
     */
    @Column(nullable = false)
    private Boolean isMember = false;
    
    /**
     * 会员过期时间
     */
    @Column(name = "membership_expire_date")
    private LocalDateTime membershipExpireDate;
    
    /**
     * 用户创建时间（自动填充）
     */
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    /**
     * 最后一次计算日期（用于判断每日免费次数重置）
     */
    @Column(name = "last_calculation_date")
    private LocalDateTime lastCalculationDate;
    
    /**
     * 今日计算次数（普通用户每日限制3次）
     */
    @Column(name = "calculation_count_today")
    private Integer calculationCountToday = 0;
}

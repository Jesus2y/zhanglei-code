package com.logistics.calculator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 限流服务（已简化，仅保留接口兼容性）
 * 实际限流逻辑由 UserService 基于数据库实现
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    /**
     * 检查是否可以计算（始终返回true，实际限流由UserService处理）
     * 
     * @param ip 客户端IP地址（未使用）
     * @param openid 用户OpenID（未使用）
     * @return 始终返回true
     */
    public boolean canCalculate(String ip, String openid) {
        // IP限流已移除，统一使用基于用户的数据库限流
        return true;
    }

    /**
     * 增加计数（空实现，实际计数由UserService处理）
     * 
     * @param ip 客户端IP地址（未使用）
     */
    public void incrementCount(String ip) {
        // IP计数已移除，统一使用基于用户的数据库计数
    }
}

package com.logistics.calculator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RateLimitService {
    
    private final StringRedisTemplate redisTemplate;
    private final UserService userService;
    
    public boolean canCalculate(String ip, String openid) {
        String key = "rate_limit:" + ip + ":" + LocalDate.now();
        String countStr = redisTemplate.opsForValue().get(key);
        
        if (countStr == null) {
            return true;
        }
        
        int count = Integer.parseInt(countStr);
        return count < 3;
    }
    
    public void incrementCount(String ip) {
        String key = "rate_limit:" + ip + ":" + LocalDate.now();
        Long count = redisTemplate.opsForValue().increment(key);
        
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofDays(1));
        }
    }
}

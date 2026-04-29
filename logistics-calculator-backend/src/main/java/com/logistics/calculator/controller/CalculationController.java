package com.logistics.calculator.controller;

import com.logistics.calculator.model.CalculationRequest;
import com.logistics.calculator.model.CalculationResult;
import com.logistics.calculator.model.User;
import com.logistics.calculator.service.LogisticsCalculationService;
import com.logistics.calculator.service.RateLimitService;
import com.logistics.calculator.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CalculationController {
    
    private final LogisticsCalculationService calculationService;
    private final UserService userService;
    private final RateLimitService rateLimitService;
    
    @PostMapping("/calculate")
    public CalculationResult calculate(@Valid @RequestBody CalculationRequest request,
                                      @RequestHeader("X-User-OpenID") String openid,
                                      HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        
        if (!rateLimitService.canCalculate(ip, openid)) {
            throw new RuntimeException("今日免费次数已用完，请升级为会员");
        }
        
        User user = userService.getUserByOpenid(openid);
        
        if (!userService.canCalculate(user)) {
            throw new RuntimeException("今日免费次数已用完，请升级为会员");
        }
        
        CalculationResult result = calculationService.calculate(request);
        
        rateLimitService.incrementCount(ip);
        userService.incrementCalculationCount(user);
        
        return result;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}

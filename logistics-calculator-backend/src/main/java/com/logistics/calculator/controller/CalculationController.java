package com.logistics.calculator.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.calculator.model.CalculationHistory;
import com.logistics.calculator.model.CalculationRequest;
import com.logistics.calculator.model.CalculationResult;
import com.logistics.calculator.model.User;
import com.logistics.calculator.repository.CalculationHistoryRepository;
import com.logistics.calculator.service.LogisticsCalculationService;
import com.logistics.calculator.service.RateLimitService;
import com.logistics.calculator.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 物流计算控制器
 * 提供物流费用计算相关的API接口
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CalculationController {

    private final LogisticsCalculationService calculationService;
    private final UserService userService;
    private final RateLimitService rateLimitService;
    private final CalculationHistoryRepository calculationHistoryRepository;
    private final ObjectMapper objectMapper;

    /**
     * 执行物流费用计算
     * 
     * @param request 计算请求参数（包含货物尺寸、重量、目的地等信息）
     * @param openid 微信用户OpenID（从请求头获取）
     * @param httpRequest HTTP请求对象（用于获取客户端IP）
     * @return 计算结果（包含总费用、费用明细、警告信息等）
     * @throws RuntimeException 当用户免费次数用完时抛出异常
     */
    @PostMapping("/calculate")
    public CalculationResult calculate(@Valid @RequestBody CalculationRequest request,
                                      @RequestHeader("X-User-OpenID") String openid,
                                      HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        User user = userService.getUserByOpenid(openid);

        // 会员不限次，普通用户同时受 IP 和用户级限制。
        if (!Boolean.TRUE.equals(user.getIsMember())) {
            if (!rateLimitService.canCalculate(ip, openid)) {
                throw new RuntimeException("今日免费次数已用完，请升级为会员");
            }
            if (!userService.canCalculate(user)) {
                throw new RuntimeException("今日免费次数已用完，请升级为会员");
            }
        }

        CalculationResult result = calculationService.calculate(request);

        if (!Boolean.TRUE.equals(user.getIsMember())) {
            rateLimitService.incrementCount(ip);
            userService.incrementCalculationCount(user);
        }

        saveHistory(user, request, result);
        return result;
    }

    /**
     * 保存计算历史记录到数据库
     * 
     * @param user 执行计算的用户
     * @param request 计算请求参数
     * @param result 计算结果
     * @throws RuntimeException 当保存失败时抛出异常
     */
    private void saveHistory(User user, CalculationRequest request, CalculationResult result) {
        try {
            CalculationHistory history = new CalculationHistory();
            history.setUserId(user.getId());
            history.setCountry(request.getCountry());
            history.setShippingMethod(request.getShippingMethod());
            history.setTotalCost(result.getTotalCost());
            history.setCalculationData(objectMapper.writeValueAsString(request));
            calculationHistoryRepository.save(history);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("计算结果保存失败", e);
        }
    }

    /**
     * 获取客户端真实IP地址
     * 依次尝试从多个请求头中获取，以处理代理情况
     * 
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
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

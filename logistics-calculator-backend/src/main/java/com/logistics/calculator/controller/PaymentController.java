package com.logistics.calculator.controller;

import com.logistics.calculator.model.PaymentRecord;
import com.logistics.calculator.model.User;
import com.logistics.calculator.repository.PaymentRecordRepository;
import com.logistics.calculator.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 支付管理控制器
 * 提供会员购买订单创建和支付回调处理等API接口
 */
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRecordRepository paymentRecordRepository;
    private final UserService userService;

    /**
     * 创建会员购买订单
     * 生成订单记录并返回订单信息，待接入微信支付JSAPI
     * 
     * @param openid 微信用户OpenID（从请求头获取）
     * @param payload 请求体（可选，预留扩展参数）
     * @return 订单信息（包含订单号、金额、货币类型、状态等）
     */
    @PostMapping("/create")
    public Map<String, Object> createOrder(@RequestHeader("X-User-OpenID") String openid,
                                           @RequestBody(required = false) Map<String, Object> payload) {
        User user = userService.getUserByOpenid(openid);

        PaymentRecord record = new PaymentRecord();
        record.setOrderNo("M" + System.currentTimeMillis());
        record.setUserId(user.getId());
        record.setAmount(new BigDecimal("9.90"));
        record.setStatus("CREATED");
        paymentRecordRepository.save(record);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("orderNo", record.getOrderNo());
        result.put("amount", record.getAmount());
        result.put("currency", record.getCurrency());
        result.put("status", record.getStatus());
        result.put("message", "订单创建成功，待接入微信支付JSAPI");
        return result;
    }

    /**
     * 支付回调接口
     * 处理微信支付成功后的回调通知，更新订单状态并升级用户为会员
     * 
     * @param payload 回调参数（包含orderNo、status、transactionId等）
     * @return 处理结果（包含订单号和状态）
     * @throws RuntimeException 当订单不存在时抛出异常
     */
    @PostMapping("/callback")
    public Map<String, Object> callback(@RequestBody Map<String, String> payload) {
        String orderNo = payload.get("orderNo");
        String status = payload.getOrDefault("status", "SUCCESS");

        PaymentRecord record = paymentRecordRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        record.setStatus(status);
        record.setTransactionId(payload.getOrDefault("transactionId", UUID.randomUUID().toString()));
        record.setPaidAt(LocalDateTime.now());
        paymentRecordRepository.save(record);

        if ("SUCCESS".equalsIgnoreCase(status)) {
            userService.upgradeToMember(record.getUserId(), LocalDateTime.now().plusMonths(1));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("orderNo", record.getOrderNo());
        result.put("status", record.getStatus());
        return result;
    }
}


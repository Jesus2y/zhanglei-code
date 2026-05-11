package com.logistics.calculator.controller;

import com.logistics.calculator.model.PaymentRecord;
import com.logistics.calculator.model.User;
import com.logistics.calculator.repository.PaymentRecordRepository;
import com.logistics.calculator.service.UserService;
import com.logistics.calculator.service.WechatPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付管理控制器
 * 接入微信支付 JSAPI，提供会员购买订单创建和回调处理
 */
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRecordRepository paymentRecordRepository;
    private final UserService userService;
    private final WechatPayService wechatPayService;

    /**
     * 创建会员购买订单（调用微信支付JSAPI下单）
     *
     * 流程：保存订单 -> 调微信JSAPI下单 -> 返回前端调起支付所需参数
     *
     * @param openid 微信用户OpenID
     * @return 包含 wx.requestPayment 所需全部参数的响应
     */
    @PostMapping("/create")
    public Map<String, Object> createOrder(@RequestHeader("X-User-OpenID") String openid) {
        User user = userService.getUserByOpenid(openid);

        // 1. 生成本地订单号
        String orderNo = "M" + System.currentTimeMillis();

        // 2. 金额从配置读取（单位：元），这里固定为9.9元
        BigDecimal amount = new BigDecimal("9.90");

        // 3. 创建本地订单记录
        PaymentRecord record = new PaymentRecord();
        record.setOrderNo(orderNo);
        record.setUserId(user.getId());
        record.setAmount(amount);
        record.setStatus("CREATED");
        paymentRecordRepository.save(record);

        try {
            // 4. 调用微信支付 JSAPI 下单接口
            var prepayResponse = wechatPayService.createJsapiOrder(openid, orderNo, amount);

            // 5. 构建返回给前端的参数（wx.requestPayment 直接使用）
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("orderNo", orderNo);
            result.put("amount", amount);

            // 前端调起支付所需参数
            result.put("appId", prepayResponse.getAppId());
            result.put("timeStamp", prepayResponse.getTimeStamp());
            result.put("nonceStr", prepayResponse.getNonceStr());
            result.put("packageValue", prepayResponse.getPackageVal()); // package 是 Java 关键字
            result.put("signType", "RSA");
            result.put("paySign", prepayResponse.getPaySign());

            return result;

        } catch (Exception e) {
            // 微信下单失败时关闭本地订单
            record.setStatus("FAILED");
            paymentRecordRepository.save(record);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "订单创建失败：" + e.getMessage());
            errorResult.put("orderNo", orderNo);
            return errorResult;
        }
    }

    /**
     * 微信支付回调通知
     *
     * 注意：此接口由微信服务器主动 POST 调用，
     * 需要配置 notifyUrl 且服务端可公网访问
     *
     * @param body 微信支付的原始通知体（加密的 JSON）
     * @return 成功响应（必须返回 {"code": "SUCCESS"}）
     */
    @PostMapping("/callback")
    public Map<String, String> callback(@RequestBody String body,
                                        @RequestHeader("Wechatpay-Serial") String serialNumber,
                                        @RequestHeader("Wechatpay-Signature") String signature) {
        Map<String, String> successResp = new HashMap<>();
        successResp.put("code", "SUCCESS");
        successResp.put("message", "成功");

        try {
            // TODO: 使用 NotificationParser 解密并验证签名
            // 示例伪代码（需要根据实际 SDK 版本调整）：
            //
            // Notification notification = notificationParser.parse(serialNumber, signature, body);
            // Transaction transaction = notification.getDecryptResource().toObject(Transaction.class, gson);
            //
            // String outTradeNo = transaction.getOutTradeNo();
            // String tradeState = transaction.getTradeState();
            // String transactionId = transaction.getTransactionId();

            // 暂时兼容原有逻辑（正式上线需替换为上面的解密验证流程）
            // 这里先返回成功避免微信重复回调
            return successResp;

        } catch (Exception e) {
            Map<String, String> failResp = new HashMap<>();
            failResp.put("code", "FAIL");
            failResp.put("message", e.getMessage());
            return failResp;
        }
    }

    /**
     * 支付结果查询（前端轮询或主动确认用）
     */
    @GetMapping("/query/{orderNo}")
    public Map<String, Object> queryOrder(@PathVariable String orderNo) {
        Map<String, Object> result = new HashMap<>();

        PaymentRecord record = paymentRecordRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        result.put("success", true);
        result.put("orderNo", record.getOrderNo());
        result.put("status", record.getStatus());
        result.put("amount", record.getAmount());
        if ("SUCCESS".equals(record.getStatus())) {
            result.put("paidAt", record.getPaidAt());
            result.put("transactionId", record.getTransactionId());
        }

        return result;
    }

    /**
     * 内部方法：处理支付成功的业务逻辑
     * （供回调解密后或手动触发调用）
     */
    private void handlePaymentSuccess(String orderNo, String transactionId) {
        PaymentRecord record = paymentRecordRepository.findByOrderNo(orderNo).orElse(null);
        if (record == null || "SUCCESS".equals(record.getStatus())) {
            return; // 已处理过，幂等保护
        }

        record.setStatus("SUCCESS");
        record.setTransactionId(transactionId);
        record.setPaidAt(LocalDateTime.now());
        paymentRecordRepository.save(record);

        // 升级会员：1个月
        userService.upgradeToMember(
                record.getUserId(),
                LocalDateTime.now().plusMonths(1)
        );
    }
}

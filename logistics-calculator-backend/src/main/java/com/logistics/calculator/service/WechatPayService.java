package com.logistics.calculator.service;

import com.logistics.calculator.config.WechatPayConfig;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class WechatPayService {

    private final WechatPayConfig wechatPayConfig;
    private volatile JsapiServiceExtension jsapiService;

    private JsapiServiceExtension getJsapiService() throws Exception {
        if (jsapiService == null) {
            synchronized (this) {
                if (jsapiService == null) {
                    Config config = new RSAAutoCertificateConfig.Builder()
                            .merchantId(wechatPayConfig.getMchId())
                            .privateKeyFromPath(wechatPayConfig.getPrivateKeyPath())
                            .merchantSerialNumber(wechatPayConfig.getMerchantSerialNumber())
                            .apiV3Key(wechatPayConfig.getApiV3Key())
                            .build();
                    jsapiService = new JsapiServiceExtension.Builder().config(config).build();
                }
            }
        }
        return jsapiService;
    }

    /**
     * JSAPI 下单 - 创建预支付订单，返回前端调起支付所需参数
     */
    public PrepayWithRequestPaymentResponse createJsapiOrder(String openid, String outTradeNo, BigDecimal amount) throws Exception {
        // 金额单位：分
        int totalFee = amount.multiply(new BigDecimal("100")).intValue();

        PrepayRequest request = new PrepayRequest();
        request.setAppid(wechatPayConfig.getAppId());
        request.setMchid(wechatPayConfig.getMchId());
        request.setDescription("跨境物流计算器-会员购买");
        request.setOutTradeNo(outTradeNo);
        request.setNotifyUrl(wechatPayConfig.getNotifyUrl());

        Amount payAmount = new Amount();
        payAmount.setTotal(totalFee);
        payAmount.setCurrency("CNY");
        request.setAmount(payAmount);

        Payer payer = new Payer();
        payer.setOpenid(openid);
        request.setPayer(payer);

        // 调用微信支付下单接口
        return getJsapiService().prepayWithRequestPayment(request);
    }

    /**
     * 关闭订单
     */
    public void closeOrder(String outTradeNo) throws Exception {
        CloseOrderRequest request = new CloseOrderRequest();
        request.setMchid(wechatPayConfig.getMchId());
        request.setOutTradeNo(outTradeNo);
        getJsapiService().closeOrder(request);
    }

    /**
     * 查询订单状态
     */
    public Transaction queryOrderByOutTradeNo(String outTradeNo) throws Exception {
        GetOrderByOutTradeNoRequest request = new GetOrderByOutTradeNoRequest();
        request.setMchid(wechatPayConfig.getMchId());
        request.setOutTradeNo(outTradeNo);
        return getJsapiService().queryOrderByOutTradeNo(request);
    }
}

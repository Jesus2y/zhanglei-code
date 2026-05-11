package com.logistics.calculator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat.pay")
public class WechatPayConfig {

    private String appId;

    private String mchId;

    private String apiV3Key;

    private String privateKeyPath;

    private String merchantSerialNumber;

    private String notifyUrl;
}

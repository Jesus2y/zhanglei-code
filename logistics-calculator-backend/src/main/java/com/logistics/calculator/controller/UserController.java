package com.logistics.calculator.controller;

import com.logistics.calculator.model.User;
import com.logistics.calculator.repository.CalculationHistoryRepository;
import com.logistics.calculator.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户管理控制器
 * 提供用户登录、信息查询、历史记录等API接口
 */
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CalculationHistoryRepository calculationHistoryRepository;

    /**
     * 用户登录接口
     * 根据openid或code登录，若用户不存在则自动创建
     * 
     * @param params 请求参数（包含openid、code、nickname、avatarUrl、phoneNumber）
     * @return 登录结果（包含用户信息）
     * @throws RuntimeException 当openid和code都为空时抛出异常
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> params) {
        String openid = params.get("openid");
        String code = params.get("code");
        String nickname = params.get("nickname");
        String avatarUrl = params.get("avatarUrl");
        String phoneNumber = params.get("phoneNumber");

        // MVP: 若前端仅传 code，则使用稳定前缀生成 mock openid，后续可替换为微信换取 openid。
        if ((openid == null || openid.isBlank()) && code != null && !code.isBlank()) {
            openid = "wx_" + code;
        }

        if (openid == null || openid.isBlank()) {
            throw new RuntimeException("openid 或 code 不能为空");
        }

        User user = userService.getUserByOpenid(openid);

        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname);
        }
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            user.setAvatarUrl(avatarUrl);
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            user.setPhoneNumber(phoneNumber);
        }

        user = userService.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("user", user);
        return result;
    }

    /**
     * 获取用户个人信息
     * 
     * @param openid 微信用户OpenID（从请求头获取）
     * @return 用户完整信息
     */
    @GetMapping("/profile")
    public User getProfile(@RequestHeader("X-User-OpenID") String openid) {
        return userService.getUserByOpenid(openid);
    }

    /**
     * 获取用户的计算历史记录（分页）
     * 
     * @param openid 微信用户OpenID（从请求头获取）
     * @param page 页码（从0开始，默认为0）
     * @param size 每页大小（默认10，最大50）
     * @return 分页结果（包含历史记录列表、总数、页码等信息）
     */
    @GetMapping("/history")
    public Map<String, Object> getHistory(
            @RequestHeader("X-User-OpenID") String openid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = userService.getUserByOpenid(openid);
        var data = calculationHistoryRepository.findByUserIdOrderByCreatedAtDesc(
                user.getId(),
                PageRequest.of(page, Math.min(size, 50))
        );

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("items", data.getContent());
        result.put("total", data.getTotalElements());
        result.put("page", data.getNumber());
        result.put("size", data.getSize());
        return result;
    }
}

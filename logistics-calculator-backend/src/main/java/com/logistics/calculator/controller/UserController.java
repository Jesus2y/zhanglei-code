package com.logistics.calculator.controller;

import com.logistics.calculator.model.User;
import com.logistics.calculator.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> params) {
        String openid = params.get("openid");
        String nickname = params.get("nickname");
        
        User user = userService.getUserByOpenid(openid);
        
        if (nickname != null && !nickname.isEmpty()) {
            user.setNickname(nickname);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("user", user);
        return result;
    }
    
    @GetMapping("/profile")
    public User getProfile(@RequestHeader("X-User-OpenID") String openid) {
        return userService.getUserByOpenid(openid);
    }
}

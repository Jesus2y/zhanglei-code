package com.logistics.calculator.service;

import com.logistics.calculator.model.User;
import com.logistics.calculator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    public User getUserByOpenid(String openid) {
        return userRepository.findByOpenid(openid)
                .orElseGet(() -> createUser(openid));
    }
    
    @Transactional
    public User createUser(String openid) {
        User user = new User();
        user.setOpenid(openid);
        user.setIsMember(false);
        user.setCalculationCountToday(0);
        return userRepository.save(user);
    }
    
    @Transactional
    public boolean canCalculate(User user) {
        if (Boolean.TRUE.equals(user.getIsMember())) {
            return true;
        }
        
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        if (user.getLastCalculationDate() == null || 
            user.getLastCalculationDate().isBefore(today)) {
            user.setCalculationCountToday(0);
            user.setLastCalculationDate(LocalDateTime.now());
        }
        
        return user.getCalculationCountToday() < 3;
    }
    
    @Transactional
    public void incrementCalculationCount(User user) {
        user.setCalculationCountToday(user.getCalculationCountToday() + 1);
        user.setLastCalculationDate(LocalDateTime.now());
        userRepository.save(user);
    }
    
    @Transactional
    public User upgradeToMember(Long userId, LocalDateTime expireDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setIsMember(true);
        user.setMembershipExpireDate(expireDate);
        return userRepository.save(user);
    }
}

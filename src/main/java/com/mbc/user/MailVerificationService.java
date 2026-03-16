package com.mbc.user;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailVerificationService {
	
	private final StringRedisTemplate redisTemplate;
	
	private final String AUTH_PREFIX = "mail:auth:";
    private final String LIMIT_PREFIX = "mail:limit:";
	
	// 인증번호 저장 (Key: Email, Value: Code, 유효시간 5분)
    public void saveAuthCode(String email, String code) {
        String key = AUTH_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(5));
    }

    // 발송 제한 키 저장 (1분간 유지)
    public void saveSendLimit(String email) {
        redisTemplate.opsForValue().set(LIMIT_PREFIX + email, "sent", Duration.ofMinutes(1));
    }
    
    // 발송 가능한지 확인 (키가 있으면 발송 불가)
    public boolean isLimit(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LIMIT_PREFIX + email));
    }
    
    // 인증번호 가져오기
    public String getData(String email) {
        return redisTemplate.opsForValue().get(AUTH_PREFIX + email);
    }

    // 데이터 삭제
    public void deleteData(String email) {
        redisTemplate.delete(AUTH_PREFIX + email);
    }

}

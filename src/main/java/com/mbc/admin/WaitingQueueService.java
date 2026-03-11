package com.mbc.admin;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


@Service
public class WaitingQueueService {
	private final StringRedisTemplate redisTemplate;
    private static final String QUEUE_KEY = "waiting_queue";
    private static final String QUEUE_STATUS_KEY = "queue_on"; // 대기열 가동 상태 키

    public WaitingQueueService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // --- [추가] 대기열 활성화/비활성화 로직 ---

    // 대기열이 켜져 있는지 확인
    public boolean isQueueEnabled() {
        return Boolean.TRUE.equals(redisTemplate.hasKey(QUEUE_STATUS_KEY));
    }

    // 관리자가 대기열 가동 (Redis에 키 생성)
    public void enableQueue() {
        redisTemplate.opsForValue().set(QUEUE_STATUS_KEY, "true");
    }

    // 관리자가 대기열 종료 (Redis에서 키 삭제)
    public void disableQueue() {
        redisTemplate.delete(QUEUE_STATUS_KEY);
    }

    // --- [기존] 대기열 로직 ---

    // 대기열에 추가 (사용자 ID와 시간)
    public void enterQueue(String userId) {
        // ZSet: score는 시간(currentTimeMillis)으로 사용
        redisTemplate.opsForZSet().add(QUEUE_KEY, userId, System.currentTimeMillis());
    }

    // 내 순번 확인 (0부터 시작하므로 +1)
    public Long getRank(String userId) {
        Long rank = redisTemplate.opsForZSet().rank(QUEUE_KEY, userId);
        return (rank != null) ? rank + 1 : null; // 1등부터 시작
    }
    
    // 대기열에서 나가기 (결제 완료 혹은 취소 시)
    public void leaveQueue(String userId) {
        redisTemplate.opsForZSet().remove(QUEUE_KEY, userId);
    }
    
 // WaitingQueueService.java에 추가
    public Long getQueueSize() {
        return redisTemplate.opsForZSet().zCard(QUEUE_KEY);
    }
    
    public boolean isInQueue(String userId) {
        return redisTemplate.opsForZSet().score(QUEUE_KEY, userId) != null;
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
}
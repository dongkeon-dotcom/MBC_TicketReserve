package com.mbc.admin;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;



@Service
public class WaitingQueueService {
    private final StringRedisTemplate redisTemplate;
    
    // 키를 관리하기 쉽게 접두사로 변경합니다.
    private static final String QUEUE_KEY_PREFIX = "waiting_queue:";
    private static final String STATUS_KEY_PREFIX = "queue_status:";

    public WaitingQueueService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // --- [내부 로직] 공연별 Redis 키 생성 ---
    private String getQueueKey(String showId) {
        return QUEUE_KEY_PREFIX + showId;
    }

    private String getStatusKey(String showId) {
        return STATUS_KEY_PREFIX + showId;
    }

    // --- [추가] 대기열 활성화/비활성화 로직 (공연별) ---

    // 특정 공연의 대기열이 켜져 있는지 확인
    public boolean isQueueEnabled(String showId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getStatusKey(showId)));
    }

    // 특정 공연의 대기열 가동 (관리자용)
    public void enableQueue(String showId) {
        redisTemplate.opsForValue().set(getStatusKey(showId), "true");
    }

    // 특정 공연의 대기열 종료 (관리자용)
    public void disableQueue(String showId) {
        redisTemplate.delete(getStatusKey(showId));
    }

    // --- [기존] 대기열 로직 (공연별 분리) ---

    // 특정 공연 대기열에 추가
    public void enterQueue(String showId, String userId) {
        redisTemplate.opsForZSet().add(getQueueKey(showId), userId, System.currentTimeMillis());
    }

    // 특정 공연에서의 내 순번 확인
    public Long getRank(String showId, String userId) {
        Long rank = redisTemplate.opsForZSet().rank(getQueueKey(showId), userId);
        return (rank != null) ? rank + 1 : null;
    }
    
    // 특정 공연 대기열에서 나가기
    public void leaveQueue(String showId, String userId) {
        redisTemplate.opsForZSet().remove(getQueueKey(showId), userId);
    }
    
    // 특정 공연의 전체 대기열 크기 확인
    public Long getQueueSize(String showId) {
        return redisTemplate.opsForZSet().zCard(getQueueKey(showId));
    }
    
    // 특정 사용자가 해당 공연 대기열에 있는지 확인
    public boolean isInQueue(String showId, String userId) {
        return redisTemplate.opsForZSet().score(getQueueKey(showId), userId) != null;
    }
    
    // 특정 공연 대기열의 1번(가장 오래된 사람) 제거
    public void popFirstFromQueue(String showId) {
        redisTemplate.opsForZSet().removeRange(getQueueKey(showId), 0, 0);
    }
    
    
    
    
}
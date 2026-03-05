package com.mbc.admin;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mbc.admin.repositiry.SeatInventoryRepository;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PerformanceCleanupScheduler {

    private final SeatInventoryRepository seatInventoryRepository;

    /**
     * 매일 새벽 4시에 실행되어 이미 종료된 공연의 좌석 재고 데이터를 삭제합니다.
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void autoDeleteExpiredSeats() {
    	LocalDateTime targetDateTime = LocalDateTime.now().minusDays(3);
    	
    	log.info("--- 지난 공연 좌석 데이터 삭제 스케줄러 시작 (기준시간: {}) ---", targetDateTime);
        
        try {
            seatInventoryRepository.deleteByStartTimeBefore(targetDateTime);
            log.info("--- 지난 공연 좌석 데이터 삭제 완료 ---");
        } catch (Exception e) {
            log.error("--- 좌석 데이터 삭제 중 오류 발생: {} ---", e.getMessage());
        }
    }
}
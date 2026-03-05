package com.mbc.admin.repositiry;

import com.mbc.admin.entity.PerformanceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PerformanceScheduleRepository extends JpaRepository<PerformanceSchedule, Long> {

    /**
     * [1] 티켓 예매 시작 시간(openingTime)을 기준으로 회차 목록 조회
     * 스케줄러가 "오늘로부터 3일 뒤가 오픈인 회차들 다 가져와"라고 할 때 사용합니다.
     */
    List<PerformanceSchedule> findByOpeningTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * [2] 특정 공연(performance_id)에 속한 모든 회차 조회
     * 관리자 페이지에서 특정 공연의 전체 일정을 볼 때 사용합니다.
     */
    List<PerformanceSchedule> findByPerformancePerformanceId(Long performanceId);

    /**
     * [3] 특정 날짜 범위 내의 회차 조회
     * (예: 3월 1일 ~ 3월 15일 사이의 모든 공연 회차)
     */
    List<PerformanceSchedule> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
    
    
        // 특정 공연 ID에 해당하는 모든 데이터를 한 번에 삭제
    @Modifying // 삭제/수정 쿼리에는 필수
    @Query("DELETE FROM PerformanceSchedule s WHERE s.performance.performanceId = :performanceId")
    void deleteByPerformanceId(@Param("performanceId") Long performanceId);
} 
    // (나머지 리포지토리도 동일하게 deleteByPerformanceId 추가 필요)
    
    
    

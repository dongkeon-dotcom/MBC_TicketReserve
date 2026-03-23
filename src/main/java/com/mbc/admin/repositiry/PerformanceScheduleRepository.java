package com.mbc.admin.repositiry;

import com.mbc.admin.Prediction.SalesDataMapping;
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
    
 // 이름을 findByPerformance_PerformanceId로 바꾸거나, 아래처럼 @Query를 씁니다.
    @Query("SELECT s FROM PerformanceSchedule s WHERE s.performance.performanceId = :performanceId")
    List<PerformanceSchedule> findByPerformanceId(@Param("performanceId") Long performanceId);        // 특정 공연 ID에 해당하는 모든 데이터를 한 번에 삭제
    @Modifying // 삭제/수정 쿼리에는 필수
    @Query("DELETE FROM PerformanceSchedule s WHERE s.performance.performanceId = :performanceId")
    void deleteByPerformanceId(@Param("performanceId") Long performanceId);
    
    
 // 가장 최근의, 아직 오픈 전인 데이터 1건만 DB에서 바로 가져옴
    @Query("SELECT s FROM PerformanceSchedule s WHERE s.openingTime > :now ORDER BY s.openingTime ASC LIMIT 1")
    PerformanceSchedule findTopByOpeningTimeAfterOrderByOpeningTimeAsc(@Param("now") LocalDateTime now);
    
    
    //시계열 예
    @Query(value = "SELECT " +
            "DATE_FORMAT(ps.start_time, '%m/%d %H:%i') as startTime, " +
            "COUNT(CASE WHEN si.is_reserved = 1 THEN 1 END) as currentCount, " +
            "ps.schedule_id as scheduleId " +
            "FROM performance_schedule ps " +
            "LEFT JOIN seat_inventory si ON ps.schedule_id = si.schedule_id " +
            "WHERE ps.performance_id = :performanceId " + // 전달받은 공연 ID (예: 211)
            "AND ps.start_time >= CURDATE() " +           // 오늘 00시부터
            "AND ps.start_time < DATE_ADD(CURDATE(), INTERVAL 8 DAY) " + // 7일 후까지
            "GROUP BY ps.schedule_id, ps.start_time " +
            "ORDER BY ps.start_time ASC", nativeQuery = true)
    List<SalesDataMapping> findUpcomingSchedules(@Param("performanceId") Long performanceId);
    
    /**
     * [수정] 관제 센터 전용: 티켓 오픈 전후 데이터를 모두 조회
     * threshold : 기준 시간 (보통 현재 시간에서 몇 시간 전으로 설정)
     */
   
 // 1. 관제용: 지금 이후로 티켓 오픈이 예정된 회차들만 오픈 시간순으로 보기
    // (대기열을 미리 준비해야 하는 '예정된' 것들만 추출)
    List<PerformanceSchedule> findByOpeningTimeAfterOrderByOpeningTimeAsc(LocalDateTime now);

    // 2. 관리용: 이미 오픈된 것들 중 최근 것 보기 (이미 대기열이 돌고 있는 것 확인용)
    List<PerformanceSchedule> findByOpeningTimeBeforeOrderByOpeningTimeDesc(LocalDateTime now);
    
    
    /**
     * [수정] 관제 센터 전용: 티켓 오픈 전후 데이터를 모두 조회
     * threshold : 기준 시간 (보통 현재 시간에서 몇 시간 전으로 설정)
     */
    @Query("SELECT s FROM PerformanceSchedule s " +
    	       "JOIN FETCH s.performance p " +
    	       "WHERE s.scheduleId IN (" +
    	       "    SELECT MIN(s2.scheduleId) " +
    	       "    FROM PerformanceSchedule s2 " +
    	       "    WHERE s2.openingTime >= :threshold " +
    	       "    GROUP BY s2.performance.performanceId" +
    	       ") " +
    	       "ORDER BY s.openingTime ASC")
    	List<PerformanceSchedule> findControlTargetSchedules(@Param("threshold") LocalDateTime threshold);
    
    
    
    
}
    // (나머지 리포지토리도 동일하게 deleteByPerformanceId 추가 필요)
    
    
    

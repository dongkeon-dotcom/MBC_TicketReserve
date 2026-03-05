package com.mbc.admin.repositiry;

import com.mbc.admin.entity.SeatInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeatInventoryRepository extends JpaRepository<SeatInventory, Long> {

    // [1] 특정 회차(schedule_id)의 좌석이 이미 생성되었는지 확인 (중복 생성 방지용)
    boolean existsByScheduleScheduleId(Long scheduleId);

    // [2] 특정 회차의 모든 좌석 리스트 가져오기 (예매 페이지 등에서 사용)
    List<SeatInventory> findByScheduleScheduleId(Long scheduleId);

    // [3] 지난 공연 좌석 데이터 삭제 (공간 확보 및 관리용)
    @Modifying
    @Transactional
    @Query("DELETE FROM SeatInventory si WHERE si.schedule.startTime < :now")
    void deleteByStartTimeBefore(@Param("now") LocalDateTime now);

    // [4] 특정 회차의 잔여 좌석 수 조회 (isReserved = 0)
    long countByScheduleScheduleIdAndIsReserved(Long scheduleId, Integer isReserved);

    @Modifying
    @Transactional
    @Query("DELETE FROM SeatInventory s WHERE s.schedule.performance.performanceId = :performanceId")
    void deleteByPerformanceId(@Param("performanceId") Long performanceId);

}
//실제로 판매될 개별 좌석을 저정하고 확인하기 위해 사용 
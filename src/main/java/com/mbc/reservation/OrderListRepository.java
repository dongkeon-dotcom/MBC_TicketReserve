package com.mbc.reservation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderListRepository  extends JpaRepository<OrderList, Long> {
    // 필요한 경우 추가적인 조회 메서드 작성
	
	// [핵심] 특정 유저가 특정 회차(schedule)에 예매한 내역이 있는지 확인
    // schedule 객체 내부의 scheduleId 필드를 참조합니다.
	boolean existsByUserIdxAndSchedule_ScheduleIdAndStatusNot(Long userIdx, Long scheduleId, String status);    
    // (선택사항) 나중에 마이페이지에서 내 예매 내역을 조회할 때 필요할 수 있습니다.
    // List<OrderList> findByUserIdxOrderByReserveDateDesc(Long userIdx);
	
	@Query("SELECT o FROM OrderList o " +
		       "JOIN FETCH o.schedule s " +
		       "JOIN FETCH s.performance p " +
		       "WHERE o.status = 'CANCELLED'")
		List<OrderList> findAllCancelledWithFetchJoin();
	/**
     * [추가 - 통계용] 공연 제목으로 '확정된' 예매 내역만 조회 (30석 기준 예매율 계산용)
     */
    @Query("SELECT o FROM OrderList o " +
           "JOIN FETCH o.schedule s " +
           "JOIN FETCH s.performance p " +
           "WHERE p.title LIKE %:title% " +
           "AND o.status = 'CONFIRMED'")
    List<OrderList> findAllByPerformanceTitle(@Param("title") String title);

    /**
     * [추가 - 검색용] 공연 제목으로 '취소된' 예매 내역만 조회
     */
    @Query("SELECT o FROM OrderList o " +
           "JOIN FETCH o.schedule s " +
           "JOIN FETCH s.performance p " +
           "WHERE p.title LIKE %:title% " +
           "AND o.status = 'CANCELLED'")
    List<OrderList> findCancelledByPerformanceTitle(@Param("title") String title);
    
    
 // 특정 회차의 특정 상태 주문 개수 조회
    long countBySchedule_ScheduleIdAndStatus(Long scheduleId, String status);
    
}


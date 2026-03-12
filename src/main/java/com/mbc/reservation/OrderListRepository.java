package com.mbc.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderListRepository  extends JpaRepository<OrderList, Long> {
    // 필요한 경우 추가적인 조회 메서드 작성
	
	// [핵심] 특정 유저가 특정 회차(schedule)에 예매한 내역이 있는지 확인
    // schedule 객체 내부의 scheduleId 필드를 참조합니다.
	boolean existsByUserIdxAndSchedule_ScheduleIdAndStatusNot(Long userIdx, Long scheduleId, String status);    
    // (선택사항) 나중에 마이페이지에서 내 예매 내역을 조회할 때 필요할 수 있습니다.
    // List<OrderList> findByUserIdxOrderByReserveDateDesc(Long userIdx);
}
package com.mbc.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.mbc.reservation.OrderList;

public interface UserReservationRepository extends JpaRepository<OrderList, Long> {

	@Query(value = "SELECT " +
            "r.reserve_idx AS reserveIdx, r.reserve_num AS reserveNum, r.show_idx AS showIdx, r.name, r.seat_num AS seatNum, " +
            "r.payment_amount AS paymentAmount, r.status, r.reserve_date AS reserveDate, " +
            "ps.performance_id AS performanceId, ps.start_time AS startTime, " +
            "p.title, p.poster_image_name AS posterImageName, " +
            "pgc.grade_order AS gradeOrder, pgc.grade_name AS gradeName " +
            "FROM reservation r " +
            "JOIN performance_schedule ps ON r.show_idx = ps.schedule_id " +
            "JOIN performance p ON ps.performance_id = p.performance_id " +
            "JOIN performance_grade_config pgc ON pgc.performance_id = p.performance_id AND r.seat_grade = pgc.grade_order " +
            "WHERE r.user_idx = :user_idx AND r.status = :status", // status 조건 추가
            countQuery = "SELECT count(*) FROM reservation r WHERE r.user_idx = :user_idx AND r.status = :status", // 페이징을 위한 카운트 쿼리
            nativeQuery = true)
    Page<UserReservationDTO> findReservationListByUserIdAndStatus(
            @Param("user_idx") Long userIdx, 
            @Param("status") String status, 
            Pageable pageable); // Pageable 추가	
	
	@Query(value = "SELECT " +
            "r.reserve_idx AS reserveIdx, r.reserve_num AS reserveNum, r.show_idx AS showIdx, r.name, r.seat_num AS seatNum, " +
            "r.payment_amount AS paymentAmount, r.status, r.reserve_date AS reserveDate, " +
            "ps.performance_id AS performanceId, ps.start_time AS startTime, " +
            "p.title, p.poster_image_name AS posterImageName, " +
            "pgc.grade_order AS gradeOrder, pgc.grade_name AS gradeName " +
            "FROM reservation r " +
            "JOIN performance_schedule ps ON r.show_idx = ps.schedule_id " +
            "JOIN performance p ON ps.performance_id = p.performance_id " +
            "JOIN performance_grade_config pgc ON pgc.performance_id = p.performance_id AND r.seat_grade = pgc.grade_order " +
            "WHERE r.reserve_num = :reserveNum", // 예매 번호로 단건 필터링
            nativeQuery = true)
    Optional<UserReservationDTO> findByReserveNum(@Param("reserveNum") String reserveNum);
	
	@Modifying
	@Transactional
	@Query(value = "UPDATE reservation SET status = 'CANCELLED', cancel_date = NOW() " +
					"WHERE reserve_num = :reserveNum AND status = 'CONFIRMED'",
					nativeQuery = true)
	int cancelReservation(@Param("reserveNum") String reserveNum);
	
}

package com.mbc.user;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mbc.reservation.OrderList;

public interface UserReservationRepository extends JpaRepository<OrderList, Long> {

	
	@Query(value = "SELECT " +
            "r.reserve_num AS reserveNum, r.show_idx AS showIdx, r.name, r.seat_num AS seatNum, " +
            "r.payment_amount AS paymentAmount, r.status, r.reserve_date AS reserveDate, " +
            "ps.performance_id AS performanceId, ps.start_time AS startTime, " +
            "p.title, p.poster_image_name AS posterImageName, " +
            "pgc.grade_order AS gradeOrder, pgc.grade_name AS gradeName " +
            "FROM reservation r " +
            "JOIN performance_schedule ps ON r.show_idx = ps.schedule_id " +
            "JOIN performance p ON ps.performance_id = p.performance_id " +
            "JOIN performance_grade_config pgc ON pgc.performance_id = p.performance_id AND r.seat_grade = pgc.grade_order " +
            "WHERE r.user_idx = :user_idx", nativeQuery = true)
    List<UserReservationDTO> findReservationListByUserId(@Param("user_idx") Long userIdx);
	
	
}

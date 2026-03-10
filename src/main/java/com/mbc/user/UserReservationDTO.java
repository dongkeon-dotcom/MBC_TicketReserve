package com.mbc.user;

import java.time.LocalDateTime;

//Spring Data JPA가 제공하는 기능으로, 인터페이스만 선언하면 구현체는 JPA가 알아서 만들어주고 읽기전용에 적합하대서 interface로 DTO 만들어봄 
public interface UserReservationDTO {
	// reservation 테이블
    String getReserveNum();
    Long getShowIdx();
    String getName();
    String getSeatNum();
    Integer getPaymentAmount();
    String getStatus();
    LocalDateTime getReserveDate();

    // performance_schedule 테이블
    Long getPerformanceId();
    LocalDateTime getStartTime();

    // performance 테이블
    String getTitle();
    String getPosterImageName();

    // performance_grade_config 테이블
    Integer getGradeOrder();
    String getGradeName();
}

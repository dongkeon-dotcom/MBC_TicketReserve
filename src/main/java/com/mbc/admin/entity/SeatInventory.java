package com.mbc.admin.entity;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter 
@Setter
@NoArgsConstructor // 기본 생성자 추가
public class SeatInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seatId;
    @JsonIgnore // <--- 이 줄을 반드시 추가하세요! (루프 차단)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private PerformanceSchedule schedule;

    private String seatNumber;  // 1 ~ 30 (관리자 화면의 좌석 번호)
    private Integer seatType;   // 관리자가 선택한 등급 번호 (1~5)
    private Integer price;      // 해당 등급의 가격
    private Integer isReserved = 0; // 0: 미예약, 1: 예약완료, 2: 선점 중
    
    
    
    private LocalDateTime reservedAt; // 선점 시간 기록
    private String reservedBy;       // 누가 선점했는지(세션ID나 회원ID)
    
    @Version
    private Long version = 0L;  // 동시성 제어용 (낙관적 락)

    // --- [연관 관계 편의 메소드] ---
    public void setSchedule(PerformanceSchedule schedule) {
        this.schedule = schedule;
        // 스케줄 객체 쪽에서도 이 좌석을 인지할 수 있도록 리스트에 추가 (양방향일 경우 대비)
        if (schedule != null && !schedule.getSeats().contains(this)) {
            schedule.getSeats().add(this);
        }
    }

    // --- [좌석 생성을 위한 편의 생성자/메소드] ---
    /**
     * @param schedule 해당 회차 스케줄 객체
     * @param seatNumber 좌석 번호 (1~30)
     * @param seatType 등급 번호 (1~5)
     * @param price 등급에 따른 가격
     */
    public static SeatInventory createSeat(PerformanceSchedule schedule, String seatNumber, Integer seatType, Integer price,boolean isSecret) {
        SeatInventory seat = new SeatInventory();
        seat.setSchedule(schedule);
        seat.setSeatNumber(seatNumber);
        seat.setSeatType(seatType);
        seat.setPrice(price);
     // 보유석(3)이면 3을 넣고, 아니면 0(예약가능)을 넣음
        seat.setIsReserved(isSecret ? 3 : 0);
        return seat;
    }
}
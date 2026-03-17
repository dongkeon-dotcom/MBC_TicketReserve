package com.mbc.admin;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
public class ScheduleStatDto {
    private Long scheduleId;
    private LocalDateTime startTime;
    private int totalSeats;      // 전체 좌석 수 (예: 100)
    private int reservedSeats;   // 예매 완료 수 (CONFIRMED 상태)
    private double reservationRate; // 예매율 (%)
}
package com.mbc.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter 
@Setter
public class PerformanceSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id")
    private Performance performance;

    private LocalDateTime startTime;    // 실제 공연 시간
    private LocalDateTime openingTime;  // 예약 오픈 시간
    private Integer isOpened = 0;
    private LocalDateTime updatedAt;

    // 회차별 실제 좌석 인벤토리와 1:N
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SeatInventory> seats = new ArrayList<>();

    public void addSeat(SeatInventory seat) {
        seats.add(seat);
        seat.setSchedule(this);
    }
    public void setPerformance(Performance performance) {
        this.performance = performance;
    }
}
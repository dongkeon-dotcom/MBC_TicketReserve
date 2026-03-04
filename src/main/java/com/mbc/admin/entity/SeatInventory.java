package com.mbc.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter 
@Setter
public class SeatInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private PerformanceSchedule schedule;

    private String seatNumber;  // A-1 등
    private Integer seatType;   // gradeOrder와 매칭
    private Integer price;      // 확정 가격
    private Integer isReserved = 0;

    @Version
    private Long version = 0L;  // 동시성 제어용
    
 public void setSchedule(PerformanceSchedule schedule) {
    	
    	this.schedule =schedule;
    }
}
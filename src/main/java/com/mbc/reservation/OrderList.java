package com.mbc.reservation;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.mbc.admin.entity.PerformanceSchedule;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderList {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reserve_idx")
    private Long reserveIdx;

    @Column(name = "reserve_num", nullable = false, unique = true, length = 50)
    private String reserveNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_idx") // referencedColumnName 속성 삭제
    private PerformanceSchedule schedule;

    @Column(name = "user_idx", nullable = false)
    private Long userIdx;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "seat_grade", length = 10)
    private String seatGrade;

    @Column(name = "seat_num", length = 20)
    private String seatNum;

    @Column(name = "payment_amount")
    private Integer paymentAmount;

    @Column(length = 20)
    @Builder.Default
    private String status = "CONFIRMED";

    @CreationTimestamp
    @Column(name = "reserve_date", updatable = false)
    private LocalDateTime reserveDate;

    @Column(name = "cancel_date")
    private LocalDateTime cancelDate;
}
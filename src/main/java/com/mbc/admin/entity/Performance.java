package com.mbc.admin.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "performance") // DB에 이미 만든 테이블 이름과 정확히 맞춰주세요
@Getter @Setter
public class Performance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long performanceId;

    private String title;
    private String posterImageName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer isDeleted = 0;
    private LocalDateTime createdAt = LocalDateTime.now();

    // 상세 이미지와 1:N 관계
    @OneToMany(mappedBy = "performance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PerformanceDetailImage> detailImages = new ArrayList<>();

    // 등급 설정과 1:N 관계
    @OneToMany(mappedBy = "performance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PerformanceGradeConfig> grades = new ArrayList<>();

    // 회차 스케줄과 1:N 관계
    @OneToMany(mappedBy = "performance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PerformanceSchedule> schedules = new ArrayList<>();

    // 연관관계 편의 메서드들
    public void addDetailImage(PerformanceDetailImage image) {
        detailImages.add(image);
        image.setPerformance(this);
    }

    public void addGradeConfig(PerformanceGradeConfig grade) {
        grades.add(grade);
        grade.setPerformance(this);
    }

    public void addSchedule(PerformanceSchedule schedule) {
        schedules.add(schedule);
        schedule.setPerformance(this);
    }
}
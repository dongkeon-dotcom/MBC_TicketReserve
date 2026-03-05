package com.mbc.admin.entity;
// 3일전 생성을 위해  지정된 좌석등급을 미리 저장해둘 테이블이 필요합니다 그걸 위한 엔티티 

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "performance_seat_template")
@Getter
@Setter
@NoArgsConstructor
public class PerformanceSeatTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long templateId;

    // 어떤 공연의 좌석 배치도인지 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id")
    private Performance performance;

    private String seatNumber; // "1" ~ "30"
    
    private Integer gradeOrder; // 관리자가 드래그로 정한 등급 번호 (1~5)

    // --- [생성 편의 메소드] ---
    public static PerformanceSeatTemplate create(Performance performance, String seatNo, int grade) {
        PerformanceSeatTemplate template = new PerformanceSeatTemplate();
        template.setPerformance(performance);
        template.setSeatNumber(seatNo);
        template.setGradeOrder(grade);
        return template;
    }
}
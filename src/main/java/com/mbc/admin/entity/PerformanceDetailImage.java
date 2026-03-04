package com.mbc.admin.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class PerformanceDetailImage {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imageId;

    // 3. 필드명이 performance여야 setPerformance()가 생성됩니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id")
    private Performance performance;

    private String imageName;
    private String imageHash;
    private Integer displayOrder;



 // PerformanceDetailImage.java 안에 직접 추가
    public void setPerformance(Performance performance) {
        this.performance = performance;
    }


}
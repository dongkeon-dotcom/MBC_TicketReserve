package com.mbc.admin.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class PerformanceGradeConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long configId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id")
    private Performance performance;

    private Integer gradeOrder;   // 1, 2, 3...
    private String gradeName;     // VIP, R석 등
    private Integer gradePrice;
    
    

    public void setPerformance(Performance performance) {
        this.performance = performance;
    }
}
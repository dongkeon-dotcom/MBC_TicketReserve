package com.mbc.admin.repositiry;

import com.mbc.admin.entity.PerformanceGradeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PerformanceGradeConfigRepository extends JpaRepository<PerformanceGradeConfig, Long> {

    // [핵심] 해당 공연의 등급별 가격 설정 리스트 조회
    //List<PerformanceGradeConfig> findByPerformanceId(Long performanceId);
// 얘도 PerformanceSeatTemplateRepository랑 동일한 이유로 위의 코드에서 아래처럼 잡아뒀습니다 

   // [핵심] 객체 연관관계를 통해 성능 ID로 가격 설정을 조회
    @Query("SELECT g FROM PerformanceGradeConfig g WHERE g.performance.performanceId = :performanceId")
    List<PerformanceGradeConfig> findByPerformanceId(@Param("performanceId") Long performanceId);
    
    
    
    @Modifying // 삭제/수정 쿼리에는 필수
    @Query("DELETE FROM PerformanceSchedule s WHERE s.performance.performanceId = :performanceId")
    void deleteByPerformanceId(@Param("performanceId") Long performanceId);
    }
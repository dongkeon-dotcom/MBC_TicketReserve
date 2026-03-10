package com.mbc.admin.repositiry;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mbc.admin.entity.PerformanceDetailImage;

import java.util.List;

@Repository
public interface PerformenceDetailImageRepository extends JpaRepository<PerformanceDetailImage, Long> {
    
    // 특정 공연에 속한 모든 상세 이미지를 찾는 기능 (필요 시 사용)
    List<PerformanceDetailImage> findByPerformance_PerformanceId(Long performanceId);
    
    // 리스트로 여러 ID를 한 번에 삭제하는 기능 (기본 제공되지만 명시적으로 확인)
    void deleteAllByImageIdIn(List<Long> imageIds);}
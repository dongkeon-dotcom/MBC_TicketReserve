package com.mbc.admin.repositiry;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.mbc.admin.entity.Performance;

@Repository
public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    // 기본 save, findById, findAll 등 제공
	
	// Search에서 사용
	// 1. 노출 여부가 1인 것 중, 추천(featured) 우선순위로 랜덤 8개 가져오기
    @Query(value = "SELECT * FROM performance WHERE is_visible = 1 " +
                   "ORDER BY is_featured DESC, RAND() LIMIT 8", nativeQuery = true)
    List<Performance> findMainDisplayPerformances();

    // 2. 일반 검색 시 노출 설정된 것만 페이징 조회
    Page<Performance> findByTitleContainingAndIsVisible(String keyword, Integer isVisible, Pageable pageable);


 // 검색어 + 노출여부(isVisible)를 동시에 만족하는 데이터 페이징 조회
    Page<Performance> findByTitleContainingIgnoreCaseAndIsVisible(String title, Integer isVisible, Pageable pageable);

 // 관리자용: 노출 여부 상관없이 제목으로만 검색
    Page<Performance> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);






}
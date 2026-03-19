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
	// [추가] 삭제되지 않은 모든 공연 목록 가져오기 (Service의 findAll에서 사용)
    List<Performance> findAllByIsDeleted(Integer isDeleted);

    // 1. 메인 전시용: 노출(1) + 삭제안됨(0) 중 추천순 랜덤 8개
    @Query(value = "SELECT * FROM performance WHERE is_visible = 1 AND is_deleted = 0 " +
                   "ORDER BY is_featured DESC, RAND() LIMIT 8", nativeQuery = true)
    List<Performance> findMainDisplayPerformances();

    // 2. 일반 검색: 제목포함 + 노출(isVisible) + 삭제안됨(isDeleted=0)
    Page<Performance> findByTitleContainingIgnoreCaseAndIsVisibleAndIsDeleted(String title, Integer isVisible, Integer isDeleted, Pageable pageable);

    // 3. 관리자용 검색: 노출 여부 상관없이 제목 + 삭제안됨(isDeleted=0)
    Page<Performance> findByTitleContainingIgnoreCaseAndIsDeleted(String keyword, Integer isDeleted, Pageable pageable);

    // [기존 메서드 유지/수정] 하위 호환성을 위해 남겨두거나 위 메서드로 대체 사용 가능합니다.
    Page<Performance> findByTitleContainingAndIsVisibleAndIsDeleted(String keyword, Integer isVisible, Integer isDeleted, Pageable pageable);
}
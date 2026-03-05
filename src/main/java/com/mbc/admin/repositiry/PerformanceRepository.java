package com.mbc.admin.repositiry;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mbc.admin.entity.Performance;

@Repository
public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    // 기본 save, findById, findAll 등 제공
	
	// Search에서 사용
	Page<Performance> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
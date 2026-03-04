package com.mbc.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mbc.admin.entity.Performance;

@Repository
public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    // 기본 save, findById, findAll 등 제공
}
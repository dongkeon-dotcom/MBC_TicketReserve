package com.mbc.admin;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mbc.admin.entity.VenueSeatMaster;

@Repository
public interface VenueSeatMasterRepository extends JpaRepository<VenueSeatMaster, Long> {
    // 공연장 고정 좌석 30개를 가져오는 용도
}
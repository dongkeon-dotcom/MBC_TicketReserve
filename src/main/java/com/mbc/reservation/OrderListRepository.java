package com.mbc.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderListRepository  extends JpaRepository<OrderList, Long> {
    // 필요한 경우 추가적인 조회 메서드 작성
}
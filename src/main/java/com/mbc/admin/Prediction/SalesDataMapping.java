package com.mbc.admin.Prediction;

public interface SalesDataMapping {
	// 1. 회차 시간 (X축 라벨용: 03/18 19:30)
    String getStartTime();    
    
    // 2. 해당 회차의 현재 예약된 좌석 수 (Y축 데이터용)
    Integer getCurrentCount(); 
    
    // 3. (옵션) 회차 ID가 필요하다면 추가
    Long getScheduleId();
}
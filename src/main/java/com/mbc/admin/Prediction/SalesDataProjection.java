package com.mbc.admin.Prediction;

public interface SalesDataProjection {
	java.time.LocalDateTime getDs(); // String 대신 LocalDateTime 사용
	long getY();                     // SUM 결과는 bigint일 수 있으므로 long 권장
}

//데이터를 담기위한 인터페이스 입니다 
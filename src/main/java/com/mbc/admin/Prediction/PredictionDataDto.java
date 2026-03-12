package com.mbc.admin.Prediction;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PredictionDataDto {
    private String ds;
    private int y;
    private int totalSeats; // 공연장의 총 좌석 수 (이게 있어야 판매율 예측 가능!)
} 
//seat_Invetory에서 쿼리로 담은 데이터를 파이썬에 넘기기위해 dto에 담아서 넘겨줍니다 
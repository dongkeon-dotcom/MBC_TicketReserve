package com.mbc.admin.Prediction;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class PredictionResultDto {
    private String ds;
    private double yhat; // 파이썬의 예측값
}
package com.mbc.admin.Prediction;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PredictionRequest {
    private List<Integer> sales; // 과거 판매량 리스트 [10, 15, 22, ...]
    private int days;            // 예측하고 싶은 미래 일수
}
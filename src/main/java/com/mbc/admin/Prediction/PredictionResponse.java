package com.mbc.admin.Prediction;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class PredictionResponse {
    private List<PredictionResultDto> prediction;
}

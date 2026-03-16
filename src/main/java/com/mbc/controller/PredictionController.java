package com.mbc.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mbc.admin.Prediction.PredictionService;
import com.mbc.admin.repositiry.PerformanceRepository;

@RestController
@RequestMapping("/api")
public class PredictionController {

    private final PredictionService predictionService;
    private final PerformanceRepository performanceRepository; // DB 조회용

    public PredictionController(PredictionService ps, PerformanceRepository pr) {
        this.predictionService = ps;
        this.performanceRepository = pr;
    }

    @GetMapping("/predict/{id}")
    public ResponseEntity<?> predict(@PathVariable Long id) {
        // 1. DB에서 해당 공연의 과거 판매 데이터 조회
        List<Map<String, Object>> data = performanceRepository.findSalesDataById(id);
        
        // 2. 서비스 호출하여 FastAPI 결과 받아오기
        List<Map<String, Object>> prediction = predictionService.getPrediction(data);
        
        // 3. React로 JSON 응답
        return ResponseEntity.ok(prediction);
    }
}
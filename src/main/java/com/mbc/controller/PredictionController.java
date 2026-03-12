package com.mbc.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mbc.admin.Prediction.PredictionResultDto;
import com.mbc.admin.Prediction.SalesPredictionService;

import java.util.List;

@RestController
@RequestMapping("/api") // 이 경로가 프론트엔드 fetch 주소의 기본이 됩니다.
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173") // Vite의 주소
public class PredictionController {
    
    // 서비스 객체 주입
    private final SalesPredictionService salesPredictionService;

    // React에서 호출할 엔드포인트: http://localhost:8080/api/prediction/{performanceId}
    @GetMapping("/prediction/{performanceId}")
    public ResponseEntity<List<PredictionResultDto>> getPrediction(@PathVariable Long performanceId) {
        // 서비스에서 데이터를 받아옴
        List<PredictionResultDto> result = salesPredictionService.getPrediction(performanceId);
        
        // 결과 반환 (React는 이 데이터를 JSON으로 받습니다)
        return ResponseEntity.ok(result);
    }
}
package com.mbc.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mbc.admin.Prediction.PredictionService;

import java.util.Map;

@RestController
@RequestMapping("/api/prediction")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173") // React(Vite) 앱 주소 허용
public class PredictionController {

    private final PredictionService predictionService;

    /**
     * 특정 공연의 향후 회차별 점유율을 예측하여 반환합니다.
     * GET /api/prediction/forecast/211
     */
    @GetMapping("/forecast/{performanceId}")
    public ResponseEntity<?> getForecast(@PathVariable Long performanceId) {

        try {
            // 💡 Service의 메서드명을 getForecast로 일치시켰습니다.
            Map<String, Object> forecastResult = predictionService.getForecast(performanceId);
            
            // 데이터가 없는 경우 처리
            if ("no_data".equals(forecastResult.get("status"))) {
                return ResponseEntity.ok(Map.of(
                    "status", "no_data",
                    "message", "향후 일주일간 예정된 공연 회차가 없습니다."
                ));
            }

            return ResponseEntity.ok(forecastResult);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "서ver 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}

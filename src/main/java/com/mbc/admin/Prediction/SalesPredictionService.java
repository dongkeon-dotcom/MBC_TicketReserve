package com.mbc.admin.Prediction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.mbc.admin.repositiry.SeatInventoryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SalesPredictionService {
	private final SeatInventoryRepository seatInventoryRepository;
    private final RestTemplate restTemplate = new RestTemplate(); // 필요 시 @Bean으로 등록하여 주입받으세요.

    // 1. 기존 DB 조회 메서드
    public List<PredictionDataDto> getSalesDataForPrediction(Long performanceId) {
        List<SalesDataProjection> stats = seatInventoryRepository.getSalesStatsByPerformanceId(performanceId);
        
        return stats.stream().map(s -> {
            PredictionDataDto dto = new PredictionDataDto();
            dto.setDs(s.getDs());
            dto.setY(s.getY());
            return dto;
        }).collect(Collectors.toList());
    }

    // 2. Python 서버에 예측 요청하는 메서드
    public List<PredictionResultDto> getPrediction(Long performanceId) {
        // DB에서 데이터 조회
        List<PredictionDataDto> dataList = getSalesDataForPrediction(performanceId);
        
        // 데이터가 부족하면 학습이 안 되므로 방어 코드 추가
        if (dataList.size() < 2) {
            return new ArrayList<>(); 
        }

        // Python 서버 URL (FastAPI)
        String pythonUrl = "http://localhost:8080/predict";
        
        // Python 서버로 전송 후 결과 받기
        // Python 서버의 응답 구조: {"prediction": [{"ds": "...", "yhat": ...}, ...]}
        PredictionResponse response = restTemplate.postForObject(pythonUrl, dataList, PredictionResponse.class);
        
        return (response != null) ? response.getPrediction() : new ArrayList<>();
    }
}







//dto에서 담은 데이터를 반환하는메서드를위한 서비스 getSalesDataForPrediction 해당 sql을 통해  dto에 담은 가지고 파이선에 넘길 준비 
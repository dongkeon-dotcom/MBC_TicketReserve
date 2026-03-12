package com.mbc.admin.Prediction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
        // 1. 기존 데이터 가져오기 (Projection에 getTotalSeats()가 있다고 가정)
        List<SalesDataProjection> stats = seatInventoryRepository.getSalesStatsByPerformanceId(performanceId);
        
        // 2. 공연장 총 좌석 수 조회 (별도 메서드 혹은 DB에서 직접 가져오기)
        int totalSeats = seatInventoryRepository.getTotalSeatsByPerformanceId(performanceId); 

        return stats.stream()
                .sorted((o1, o2) -> o1.getDs().compareTo(o2.getDs()))
                .map(s -> {
                    PredictionDataDto dto = new PredictionDataDto();
                    dto.setDs(s.getDs());
                    dto.setY(s.getY());
                    dto.setTotalSeats(totalSeats); // 좌석 정보 추가
                    return dto;
                }).collect(Collectors.toList());
    }
    

    // 2. Python 서버에 예측 요청하는 메서드
    public List<PredictionResultDto> getPrediction(Long performanceId) {
        // DB 데이터 조회
        List<PredictionDataDto> dataList = getSalesDataForPrediction(performanceId);
        
        System.out.println("가져온 데이터 리스트 사이즈: " + (dataList != null ? dataList.size() : "null"));
        if (dataList == null || dataList.size() < 1) {
	
        	// 테스트를 위해 조건을 0으로 변경
        	//if (dataList == null || dataList.isEmpty()) {
        	System.out.println("데이터가 2개 미만이라 예측을 건너뜁니다.");
            return new ArrayList<>(); 
        }

        // [수정] 포트를 8000으로 변경!
        String pythonUrl = "http://localhost:8000/predict";
        
        // RestTemplate 설정: 데이터를 명확하게 JSON으로 전달
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<PredictionDataDto>> request = new HttpEntity<>(dataList, headers);
        
        try {
            // [수정] postForObject로 전송
            PredictionResponse response = restTemplate.postForObject(pythonUrl, request, PredictionResponse.class);
            return (response != null && response.getPrediction() != null) ? response.getPrediction() : new ArrayList<>();
        } catch (Exception e) {
            // Python 서버 연결 실패 시 예외 처리
            System.err.println("Python 서버 연결 에러: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    
}







//dto에서 담은 데이터를 반환하는메서드를위한 서비스 getSalesDataForPrediction 해당 sql을 통해  dto에 담은 가지고 파이선에 넘길 준비 
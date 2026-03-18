package com.mbc.admin.Prediction;

import lombok.RequiredArgsConstructor; // 💡 Lombok 추가
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.mbc.admin.repositiry.PerformanceScheduleRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // 💡 생성자 자동 생성 (final 필드 대상)
public class PredictionService {

    // 💡 final로 선언해야 @RequiredArgsConstructor가 자동으로 주입해줍니다.
    private final PerformanceScheduleRepository performanceScheduleRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String PYTHON_SERVER_URL = "http://localhost:8000/forecast";

    public Map<String, Object> getForecast(Long performanceId) {
        // 1. DB에서 향후 7일간의 회차별 예약 현황 조회
        List<SalesDataMapping> scheduleData = performanceScheduleRepository.findUpcomingSchedules(performanceId);

        // 2. 데이터 추출
        List<Integer> currentCounts = scheduleData.stream()
                .map(SalesDataMapping::getCurrentCount)
                .collect(Collectors.toList());

        List<String> labels = scheduleData.stream()
                .map(SalesDataMapping::getStartTime)
                .collect(Collectors.toList());

        if (currentCounts.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("status", "no_data");
            return emptyResult;
        }

        // 3. Python 요청 데이터 준비
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("sales", currentCounts);
        requestPayload.put("days", currentCounts.size());

        try {
            // 4. Python 서버 호출
            Map<String, Object> pythonResponse = restTemplate.postForObject(PYTHON_SERVER_URL, requestPayload, Map.class);

            // 5. 최종 결과 반환
            Map<String, Object> finalResult = new HashMap<>();
            finalResult.put("status", "success");
            finalResult.put("current_counts", currentCounts);
            finalResult.put("predictions", pythonResponse.get("predictions"));
            finalResult.put("labels", labels);
            
            return finalResult;

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            return errorResult;
        }
    }
}
package com.mbc.admin.Prediction;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PredictionService {
private final RestTemplate restTemplate = new RestTemplate();

public List<Map<String, Object>> getSalesPrediction(List<Map<String, Object>> data) {
    String fastApiUrl = "http://localhost:8000/predict";
    
    // FastAPI로 데이터를 던짐
    ResponseEntity<Map> response = restTemplate.postForEntity(fastApiUrl, data, Map.class);
    
    // 결과 리턴
    return (List<Map<String, Object>>) response.getBody().get("prediction");
}
}
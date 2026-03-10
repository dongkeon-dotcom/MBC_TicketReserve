package com.mbc.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mbc.admin.WaitingQueueService;

@RestController
@RequestMapping("/reserve")
public class WaitingQueueController {

	private final WaitingQueueService waitingQueueService;

    public WaitingQueueController(WaitingQueueService waitingQueueService) {
        this.waitingQueueService = waitingQueueService;
    }

    // [신규] 예매 버튼 클릭 시 대기열 상태 확인 및 진입 처리
    @GetMapping("/check-queue")
    public ResponseEntity<Map<String, Object>> checkQueue(Principal principal) {
        String userId = principal.getName();
        Map<String, Object> response = new HashMap<>();

        // 1. 대기열 활성화 여부 확인 (서비스에 이 메서드를 추가해주세요)
        if (!waitingQueueService.isQueueEnabled()) {
            response.put("status", "DIRECT");
            return ResponseEntity.ok(response);
        }

        // 2. 대기열 진입
        waitingQueueService.enterQueue(userId);
        
        // 3. 내 순번 조회
        Long rank = waitingQueueService.getRank(userId);
        
        // 4. 즉시 입장 가능 여부 판단 (예: 100등 안이면 바로 입장)
        if (rank != null && rank <= 100) {
            response.put("status", "DIRECT");
        } else {
            response.put("status", "WAITING");
            response.put("rank", rank != null ? rank + 1 : 1); // 0부터 시작하므로 +1
        }

        return ResponseEntity.ok(response);
    }

    // 기존 내 순번 확인 API
    @GetMapping("/check-rank")
    public ResponseEntity<Map<String, Object>> checkRank(Principal principal) {
        String userId = principal.getName();
        Long rank = waitingQueueService.getRank(userId);
        
        Map<String, Object> response = new HashMap<>();
        // 대기열에 없으면 0, 있으면 순번 전달
        response.put("rank", rank != null ? rank + 1 : 0);
        return ResponseEntity.ok(response);
    }
}
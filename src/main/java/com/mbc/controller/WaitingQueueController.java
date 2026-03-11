package com.mbc.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mbc.admin.WaitingQueueService;
import com.mbc.user.Users;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/reserve")
public class WaitingQueueController {

	private final WaitingQueueService waitingQueueService;

    public WaitingQueueController(WaitingQueueService waitingQueueService) {
        this.waitingQueueService = waitingQueueService;
    }

    // [신규] 예매 버튼 클릭 시 대기열 상태 확인 및 진입 처리
 // 스프링 시큐리티 적용전이라 임시로 session으로 처리 후추 변경 필요 
    @GetMapping("/check-queue")
    public ResponseEntity<Map<String, Object>> checkQueue(HttpSession session) {
        // 1. 세션에서 "user" 객체를 가져옵니다.
        Users user = (Users) session.getAttribute("user");
        
        // 2. 로그인 안 했으면 처리
        if (user == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "REDIRECT_LOGIN");
            return ResponseEntity.ok(errorResponse);
        }

        // 3. User 객체에서 ID를 추출 (getUserId()는 Users 클래스의 getter 메서드 이름에 맞게 수정하세요)
        String userId = user.getUserId(); 
        
        Map<String, Object> response = new HashMap<>();

        // 4. 대기열 로직 실행
        long LIMIT = 0;
        if (!waitingQueueService.isQueueEnabled() && waitingQueueService.getQueueSize() < LIMIT) {
            response.put("status", "DIRECT");
            return ResponseEntity.ok(response);
        }

        if (!waitingQueueService.isInQueue(userId)) {
            waitingQueueService.enterQueue(userId);
        }
        
        Long rank = waitingQueueService.getRank(userId);
        response.put("status", "WAITING");
        response.put("rank", rank != null ? rank : 1);

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
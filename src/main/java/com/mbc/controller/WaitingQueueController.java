package com.mbc.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.mbc.admin.WaitingQueueService;
import com.mbc.security.SecurityUserDetails;
import com.mbc.user.Users;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/reserve")
public class WaitingQueueController {

	private final WaitingQueueService waitingQueueService;

    public WaitingQueueController(WaitingQueueService waitingQueueService) {
        this.waitingQueueService = waitingQueueService;
    }

   /** // [신규] 예매 버튼 클릭 시 대기열 상태 확인 및 진입 처리
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
**/
 //시큐리티 적용하면   session 때문에 이걸로 교체 필요 
    /**
     * [신규] 예매 버튼 클릭 시 공연별 대기열 상태 확인 및 진입 처리
     */
    @GetMapping("/check-queue")
    public ResponseEntity<Map<String, Object>> checkQueue(
            @RequestParam String showId, // 공연 ID 추가
            @AuthenticationPrincipal SecurityUserDetails userDetails) {

        // 1. 로그인 여부 확인
        if (userDetails == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "REDIRECT_LOGIN");
            return ResponseEntity.ok(errorResponse);
        }

        String userId = userDetails.getUsername();
        Map<String, Object> response = new HashMap<>();

        // 2. 공연별 대기열 로직
        long LIMIT = 1;
        
        // 해당 공연의 대기열이 꺼져 있고, 현재 대기 인원이 LIMIT 미만이면 바로 입장
        if (!waitingQueueService.isQueueEnabled(showId) && waitingQueueService.getQueueSize(showId) < LIMIT) {
            response.put("status", "DIRECT");
            return ResponseEntity.ok(response);
        }

        // 3. 대기열 진입 (공연 ID별로 관리)
        if (!waitingQueueService.isInQueue(showId, userId)) {
            waitingQueueService.enterQueue(showId, userId);
        }

        Long rank = waitingQueueService.getRank(showId, userId);
        response.put("status", "WAITING");
        response.put("rank", rank != null ? rank : 1);

        return ResponseEntity.ok(response);
    }

    /**
     * [신규] 공연별 실시간 순번 확인
     */
    @GetMapping("/check-rank")
    public ResponseEntity<Map<String, Object>> checkRank(
            @RequestParam String showId, // 공연 ID 추가
            @AuthenticationPrincipal SecurityUserDetails userDetails) {
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        
        String userId = userDetails.getUsername();
        Long currentRank = waitingQueueService.getRank(showId, userId);
        
        Map<String, Object> response = new HashMap<>();

        if (currentRank == null) {
            // 이미 대기열에 없거나 처리가 완료됨
            response.put("rank", 0);
        } else if (currentRank == 1) {
            // 본인이 1등임 -> 입장 처리하고 해당 공연 대기열에서 삭제
            waitingQueueService.popFirstFromQueue(showId);
            response.put("rank", 0);
        } else {
            // 대기 중 (1등이 아님)
            // 사용자에게는 1부터 시작하는 순번을 보여줌 (0번을 제외한 실제 대기 순서)
            response.put("rank", currentRank - 1);
        }

        return ResponseEntity.ok(response);
    }
}
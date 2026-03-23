package com.mbc.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mbc.admin.WaitingQueueService;
import com.mbc.admin.entity.Performance;
import com.mbc.admin.entity.PerformanceSchedule;
import com.mbc.admin.service.AdminPerformanceService;

@Controller
@RequestMapping("/admin/queue")
@PreAuthorize("hasRole('ADMIN')") // 관리자만 접근 가능하게 설정
public class AdminQueueController {

	private final WaitingQueueService waitingQueueService;
    private final AdminPerformanceService performanceService;

    public AdminQueueController(WaitingQueueService waitingQueueService, AdminPerformanceService performanceService) {
        this.waitingQueueService = waitingQueueService;
        this.performanceService = performanceService;
    }

    /**
     * 대기열 관제 센터 페이지 이동 (HTML 리턴)
     */
    @GetMapping("/dashboard.do")
    public String showWaitingQ(Model model) {
        // 공연 마스터가 아닌 '회차(Schedule)' 정보를 가져옵니다.
        List<PerformanceSchedule> upcomingSchedules = performanceService.getUpcomingTicketingSchedules();
        model.addAttribute("activeSchedules", upcomingSchedules);
        return "admin/showWaitingQ";
    }

    /**
     * [AJAX] 특정 공연의 실시간 대기 인원수 조회
     */
    @GetMapping("/{showId}/size")
    @ResponseBody // 데이터 리턴을 위해 명시
    public ResponseEntity<Long> getQueueSize(@PathVariable String showId) {
        Long size = waitingQueueService.getQueueSize(showId);
        return ResponseEntity.ok(size != null ? size : 0L);
    }

    /**
     * [AJAX] 특정 공연의 대기열 활성화 여부 확인
     */
    @GetMapping("/{showId}/is-enabled")
    @ResponseBody
    public ResponseEntity<Boolean> isEnabled(@PathVariable String showId) {
        // 여기서 waitingQueueService.isQueueEnabled(showId)를 호출해야 합니다.
        boolean enabled = waitingQueueService.isQueueEnabled(showId);
        System.out.println("공연 ID " + showId + "의 가동 상태: " + enabled); // 서버 로그로 확인!
        return ResponseEntity.ok(enabled);
    }

    /**
     * [AJAX] 대기열 강제 활성화 (ON)
     */
    @PostMapping("/{showId}/enable")
    @ResponseBody
    public ResponseEntity<String> enableQueue(@PathVariable String showId) {
        waitingQueueService.enableQueue(showId);
        return ResponseEntity.ok("대기열이 활성화되었습니다.");
    }

    /**
     * [AJAX] 대기열 중지 (OFF)
     */
    @PostMapping("/{showId}/disable")
    @ResponseBody
    public ResponseEntity<String> disableQueue(@PathVariable String showId) {
        waitingQueueService.disableQueue(showId);
        return ResponseEntity.ok("대기열이 비활성화되었습니다.");
    }
}
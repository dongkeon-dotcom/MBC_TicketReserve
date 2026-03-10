package com.mbc.controller;
import com.mbc.admin.entity.Performance;
import com.mbc.admin.service.AdminPerformanceService; // 기존 서비스 재사용 가능

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mbc.admin.entity.PerformanceGradeConfig; // 관리자 엔티티 패키지 경로
import com.mbc.admin.entity.PerformanceSchedule;
import com.mbc.admin.entity.SeatInventory;
import com.mbc.admin.repositiry.SeatInventoryRepository; // 실제 패키지 경로에 맞게 수정


@Controller
@RequestMapping("/reserve")
public class TheaterController {

	private final AdminPerformanceService performanceService;
    private final SeatInventoryRepository seatInventoryRepository; // 필드 추가

    // 2. 생성자에서 두 가지 모두를 주입받도록 수정
    public TheaterController(AdminPerformanceService performanceService, 
                             SeatInventoryRepository seatInventoryRepository) {
        this.performanceService = performanceService;
        this.seatInventoryRepository = seatInventoryRepository;
    }
    
    /**
     * 예매 메인 페이지 (공연 선택 및 일정 확인)
     */
    @GetMapping("/ticket.do")
    public String ticketPage(@RequestParam("performanceId") Long performanceId, Model model) {
        Performance performance = performanceService.findById(performanceId);
        
        // 1. 등급 리스트 가져오기
        List<PerformanceGradeConfig> gradeList = performance.getGrades();

        // 2. 스케줄 및 실시간 좌석수 가공
        List<Map<String, Object>> scheduleData = performance.getSchedules().stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("scheduleId", s.getScheduleId());
            map.put("startTime", s.getStartTime().toString()); 
            
            List<Map<String, Object>> seatStatus = new ArrayList<>();
            for (int i = 0; i < gradeList.size(); i++) {
                PerformanceGradeConfig gradeConfig = gradeList.get(i);
                Map<String, Object> seatMap = new HashMap<>();
                seatMap.put("gradeName", gradeConfig.getGradeName()); 
                int remainCount = seatInventoryRepository.countAvailableSeats(s.getScheduleId(), i + 1);
                seatMap.put("remainCount", remainCount);
                seatStatus.add(seatMap);
            }
            map.put("seatStatus", seatStatus); 
            return map;
        }).collect(Collectors.toList());

        // 3. [핵심] 여러 회차 중 가장 빠른 오픈 시간 찾기
        // import java.util.Objects; 필수
        java.time.LocalDateTime earliestOpenTime = performance.getSchedules().stream()
                .map(PerformanceSchedule::getOpeningTime)
                .filter(java.util.Objects::nonNull)
                .min(java.time.LocalDateTime::compareTo)
                .orElse(null);

        model.addAttribute("performance", performance);
        model.addAttribute("schedules", scheduleData); 
        model.addAttribute("grades", gradeList); 
        
        // JS로 오픈 시간 전달
        if (earliestOpenTime != null) {
            model.addAttribute("openDate", earliestOpenTime.toString());
        }

        return "reserve/ticket";
    }
    //아 예매창빨리 진행하고싶다고  알아서 움직이라고 
    
    
    /**
     * 좌석 선택 페이지
     */
    @GetMapping("/seat.do")
    public String seatPage(@RequestParam("scheduleId") Long scheduleId, Model model) {
        // 1. 데이터 조회
        PerformanceSchedule schedule = performanceService.findScheduleById(scheduleId);
        Performance performance = schedule.getPerformance();
        
        // [수정] 보유석(3)을 제외하지 않고, 모든 좌석을 그대로 사용합니다.
        // 그래야 화면에 보유석 자리가 비어 보이지 않고 회색으로 표시됩니다.
        List<SeatInventory> allSeats = schedule.getSeats();
        
        // 2. 스케줄 정보 가공
        List<PerformanceSchedule> schedulesFromEntity = (performance.getSchedules() != null) ? performance.getSchedules() : new ArrayList<>();
        List<Map<String, Object>> schedulesForJS = new ArrayList<>();
        for (PerformanceSchedule s : schedulesFromEntity) {
            Map<String, Object> map = new HashMap<>();
            map.put("scheduleId", s.getScheduleId());
            map.put("startTime", s.getStartTime() != null ? s.getStartTime().toString() : "");
            schedulesForJS.add(map);
        }
        
        // 3. 등급별 정보 및 잔여 좌석 계산
        List<PerformanceGradeConfig> gradeConfigs = performance.getGrades();
        List<Map<String, Object>> gradeData = new ArrayList<>();
        for (int i = 0; i < gradeConfigs.size(); i++) {
            PerformanceGradeConfig config = gradeConfigs.get(i);
            Map<String, Object> map = new HashMap<>();
            map.put("gradeName", config.getGradeName());
            map.put("price", config.getGradePrice());
            
            // 주의: 보유석(3)을 제외하고 카운트하려면 countAvailableSeats 메서드 내부 로직을 확인하세요.
            // 현재 countAvailableSeats가 보유석을 자동으로 제외하고 있다면 그대로 두시면 됩니다.
            int remainCount = seatInventoryRepository.countAvailableSeats(scheduleId, i + 1);
            map.put("remainCount", remainCount);
            gradeData.add(map);
        }
        
        // 4. 모델에 담기
        model.addAttribute("performance", performance); 
        model.addAttribute("schedule", schedule);
        model.addAttribute("seats", allSeats); // [수정] 전체 좌석 리스트 전달
        model.addAttribute("grades", gradeData);
        model.addAttribute("schedules", schedulesForJS); 
        
        return "reserve/seat";
    }


@PostMapping("/selectSeat.do")
@ResponseBody
public ResponseEntity<String> selectSeat(@RequestParam Long seatId, HttpSession session) {
    // 세션에서 현재 사용자 정보를 가져오거나 생성
    String userId = session.getId(); 
    
    try {
        // 서비스의 선점 로직 호출
        boolean success = performanceService.selectSeat(seatId, userId);
        
        if (success) {
            return ResponseEntity.ok("좌석 선점 성공");
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 다른 사용자가 선택한 좌석입니다.");
        }
    } catch (ObjectOptimisticLockingFailureException e) {
        // 동시에 여러 명이 클릭해서 버전 충돌이 발생한 경우
        return ResponseEntity.status(HttpStatus.CONFLICT).body("잠시 후 다시 시도해주세요.");
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류가 발생했습니다.");
    }
}




//새고하면 좌석풀
@PostMapping("/cancelSeat.do")
@ResponseBody
public ResponseEntity<String> cancelSeat(@RequestParam("seatId") Long seatId, HttpSession session) {
    performanceService.cancelSeat(seatId, session.getId());
    return ResponseEntity.ok("선점 취소 완료");
}





























}

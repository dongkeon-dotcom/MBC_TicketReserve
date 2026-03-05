package com.mbc.controller;
import com.mbc.admin.entity.Performance;
import com.mbc.admin.service.AdminPerformanceService; // 기존 서비스 재사용 가능

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        // 1. 회차 정보 조회 (서비스에 findScheduleById 메서드가 있어야 함)
        PerformanceSchedule schedule = performanceService.findScheduleById(scheduleId);
        Performance performance = schedule.getPerformance();
        
        // 2. 등급 정보 및 잔여석 계산 (HTML의 'grades' 반복문을 위한 데이터)
        List<PerformanceGradeConfig> gradeConfigs = performance.getGrades();
        List<Map<String, Object>> gradeData = new ArrayList<>();
        
        for (int i = 0; i < gradeConfigs.size(); i++) {
            PerformanceGradeConfig config = gradeConfigs.get(i);
            Map<String, Object> map = new HashMap<>();
            map.put("gradeName", config.getGradeName());
            map.put("price", config.getGradePrice());
            
            // 해당 회차(scheduleId)에서 해당 등급(index+1)의 잔여 좌석수 계산
            int remainCount = seatInventoryRepository.countAvailableSeats(scheduleId, i + 1);
            map.put("remainCount", remainCount);
            
            gradeData.add(map);
        }

        // 3. 모델에 담기
        model.addAttribute("schedule", schedule);
        model.addAttribute("performance", performance);
        model.addAttribute("seats", schedule.getSeats()); // 좌석 리스트
        model.addAttribute("grades", gradeData);          // 등급별 요약 리스트 (중요!)
        
        return "reserve/seat";
    }
}











































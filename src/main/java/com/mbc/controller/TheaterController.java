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
        // 1. 현재 회차 및 공연 정보 조회
        PerformanceSchedule schedule = performanceService.findScheduleById(scheduleId);
        Performance performance = schedule.getPerformance();
        
        // [핵심 해결] 엔티티 대신 순수 Map 리스트로 변환하여 전달
        //엔티티로 넘기니깐 자꾸 무한루프 걸려서 맵으로 바꿔서 전달중 
        List<PerformanceSchedule> schedulesFromEntity = (performance.getSchedules() != null) ? performance.getSchedules() : new ArrayList<>();
        
        List<Map<String, Object>> schedulesForJS = new ArrayList<>();
        for (PerformanceSchedule s : schedulesFromEntity) {
            Map<String, Object> map = new HashMap<>();
            map.put("scheduleId", s.getScheduleId());
            map.put("startTime", s.getStartTime() != null ? s.getStartTime().toString() : "");
            schedulesForJS.add(map);
        }
        
        // 2. 등급 정보 및 잔여석 계산 로직 (유지)
        List<PerformanceGradeConfig> gradeConfigs = performance.getGrades();
        List<Map<String, Object>> gradeData = new ArrayList<>();
        
        for (int i = 0; i < gradeConfigs.size(); i++) {
            PerformanceGradeConfig config = gradeConfigs.get(i);
            Map<String, Object> map = new HashMap<>();
            map.put("gradeName", config.getGradeName());
            map.put("price", config.getGradePrice());
            int remainCount = seatInventoryRepository.countAvailableSeats(scheduleId, i + 1);
            map.put("remainCount", remainCount);
            gradeData.add(map);
        }

        // 3. 모델에 담기
        model.addAttribute("schedule", schedule);
        // 중요: 엔티티를 직접 넘기면 타임리프가 무한 루프를 돌 수 있으니, 필요한 것만 넘기거나 
        // performance 엔티티 안의 schedules를 제거한 DTO로 넘기는 것이 안전합니다.
        model.addAttribute("performance", performance); 
        model.addAttribute("seats", schedule.getSeats());
        model.addAttribute("grades", gradeData);
        
        // [핵심 해결] 엔티티가 아닌 가공된 Map 리스트를 전달
        model.addAttribute("schedules", schedulesForJS); 
        
        return "reserve/seat";
    }
}











































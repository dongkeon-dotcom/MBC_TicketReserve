package com.mbc.controller;
import com.mbc.admin.entity.Performance;
import com.mbc.admin.service.AdminPerformanceService; // 기존 서비스 재사용 가능
import com.mbc.security.SecurityUserDetails;
import com.mbc.user.Users;

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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String seatPage(@RequestParam("scheduleId") Long scheduleId, 
                           @AuthenticationPrincipal SecurityUserDetails userDetails,
                           Model model, 
                           RedirectAttributes rttr) {
        
        // 0. 로그인 체크
        if (userDetails == null) {
            return "redirect:/user/login.do";
        }

        Long userIdx = userDetails.getUserIdx(); 
        
        // 1. 예매 내역 체크
        boolean alreadyReserved = performanceService.hasAlreadyReserved(userIdx, scheduleId);
        if (alreadyReserved) {
            rttr.addFlashAttribute("error", "이미 해당 회차에 대한 예매 내역이 존재합니다. (1인 1매 제한)");
            return "redirect:/"; 
        }

        // 2. [추가] 진입 시 만료된 좌석(5분 초과) 자동 해제 로직
        List<SeatInventory> allSeats = seatInventoryRepository.findByScheduleScheduleId(scheduleId);
        LocalDateTime limit = LocalDateTime.now().minusMinutes(5);
        boolean isChanged = false;

        for (SeatInventory s : allSeats) {
            if (s.getIsReserved() == 2 && s.getReservedAt() != null && s.getReservedAt().isBefore(limit)) {
                s.setIsReserved(0);
                s.setReservedAt(null);
                s.setReservedBy(null);
                isChanged = true;
            }
        }
        if (isChanged) {
            seatInventoryRepository.saveAll(allSeats);
        }

        // 3. [추가] 새로고침/재진입 시 내가 선점 중인 좌석이 있다면 상태 유지(선택됨 표시)
        // SeatInventoryRepository에 아래 메서드가 정의되어 있어야 합니다:
        // List<SeatInventory> findByReservedByAndIsReservedAndSchedule_ScheduleId(String reservedBy, Integer isReserved, Long scheduleId);
        List<SeatInventory> mySeats = seatInventoryRepository.findByReservedByAndIsReservedAndSchedule_ScheduleId(
            String.valueOf(userIdx), 2, scheduleId);
        
        if (!mySeats.isEmpty()) {
            model.addAttribute("mySelectedSeatId", mySeats.get(0).getSeatId());
        }

        // 4. 데이터 조회 (기존 로직)
        PerformanceSchedule schedule = performanceService.findScheduleById(scheduleId);
        Performance performance = schedule.getPerformance();
        
        // 리포지토리에서 다시 조회 (위에서 변경된 상태가 반영된 최신 좌석 리스트)
        List<SeatInventory> updatedSeats = seatInventoryRepository.findByScheduleScheduleId(scheduleId);
        
        List<PerformanceSchedule> schedulesFromEntity = (performance.getSchedules() != null) ? performance.getSchedules() : new ArrayList<>();
        List<Map<String, Object>> schedulesForJS = new ArrayList<>();
        for (PerformanceSchedule s : schedulesFromEntity) {
            Map<String, Object> map = new HashMap<>();
            map.put("scheduleId", s.getScheduleId());
            map.put("startTime", s.getStartTime() != null ? s.getStartTime().toString() : "");
            schedulesForJS.add(map);
        }
        
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
        
        // 5. 모델에 담기
        model.addAttribute("performance", performance); 
        model.addAttribute("schedule", schedule);
        model.addAttribute("seats", updatedSeats);
        model.addAttribute("grades", gradeData);
        model.addAttribute("schedules", schedulesForJS); 
        
        return "reserve/seat";
    }
    
    



    @PostMapping("/selectSeat.do")
    @ResponseBody
    public ResponseEntity<String> selectSeat(@RequestParam Long seatId, 
                                             @AuthenticationPrincipal SecurityUserDetails userDetails) {
        
        // 0. 로그인 체크
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요한 서비스입니다.");
        }
        
        // 1. 세션 ID 대신 DB상의 고유 식별자(userIdx) 사용
        Long userIdx = userDetails.getUserIdx();
        
        try {
            // 2. 서비스 호출 시 userIdx 전달 (서비스 로직도 이 인자를 받도록 수정되어 있어야 합니다)
        	boolean success = performanceService.selectSeat(seatId, String.valueOf(userIdx));            
            if (success) {
                return ResponseEntity.ok("좌석 선점 성공");
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 다른 사용자가 선택한 좌석입니다.");
            }
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("잠시 후 다시 시도해주세요.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류가 발생했습니다.");
        }
    }




//새고하면 좌석풀
@PostMapping("/cancelSeat.do")
@ResponseBody
public ResponseEntity<String> cancelSeat(@RequestParam("seatId") Long seatId, 
        @AuthenticationPrincipal SecurityUserDetails userDetails) {

if (userDetails == null) {
return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 정보가 없습니다.");
}

// selectSeat.do와 동일하게 userIdx를 문자열로 변환하여 전달
String userIdStr = String.valueOf(userDetails.getUserIdx());

// 서비스의 cancelSeat 로직이 (seatId, userId)를 받는지 확인 필요
performanceService.cancelSeat(seatId, userIdStr);

return ResponseEntity.ok("선점 취소 완료");
}


//동일회차 매수재한 api 
@GetMapping("/check-reservation.do")
@ResponseBody
public Map<String, Object> checkReservation(@RequestParam("scheduleId") Long scheduleId, HttpSession session) {
    Users user = (Users) session.getAttribute("user");
    Map<String, Object> response = new HashMap<>();
    
    if (user == null) {
        response.put("reserved", false); // 로그인이 안 되어 있으면 일단 통과
        return response;
    }

    boolean isReserved = performanceService.hasAlreadyReserved(user.getUserIdx(), scheduleId);
    response.put("reserved", isReserved);
    return response;
}

























}







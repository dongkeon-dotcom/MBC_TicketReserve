package com.mbc.controller;

import com.mbc.admin.PerformanceListDto;
import com.mbc.admin.PerformanceSaveDto;
import com.mbc.admin.entity.Performance;
import com.mbc.admin.entity.PerformanceSchedule;
import com.mbc.admin.service.AdminPerformanceService;
import com.mbc.reservation.OrderList;
import com.mbc.admin.entity.PerformanceSeatTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminPerformanceService performanceService;

    // 생성자 주입 (롬복 에러 방지용)
    public AdminController(AdminPerformanceService performanceService) {
        this.performanceService = performanceService;
    }
    
    /**
     * java.time.DayOfWeek를 한국어 요일 문자열로 변환하는 헬퍼 메서드
     */
    private String getKoreanDay(java.time.DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
            default -> throw new IllegalArgumentException("잘못된 요일 정보입니다.");
        };
    }
    
    
    //====이동을위한 단순 컨트롤러 ==================================================================
    // "admin.insertPage.do  를 통해 관리자의 상품 입력 제어 " 
    @GetMapping("/insertPage.do")
    public String insertPage() { 
        System.out.println("==>공연등록페이지로 이동 .do");
        return "admin/showInsert";
    }
    
    
 // 목록으로 이동하기 위한 페이지 
    @GetMapping("/listPage.do")
    public String listPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            @PageableDefault(size = 10, sort = "performanceId", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {
        
        // 1. 검색어 여부에 따라 서비스 메서드 호출
        Page<Performance> performancePage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            performancePage = performanceService.searchByTitle(keyword, pageable);
        } else {
            performancePage = performanceService.findAll(pageable);
        }
        
        // 2. DTO 변환
        Page<PerformanceListDto> dtoPage = performancePage.map(PerformanceListDto::new);
        
        // 3. 모델 전달 (검색어 포함)
        model.addAttribute("performanceList", dtoPage.getContent());
        model.addAttribute("page", dtoPage);
        model.addAttribute("keyword", keyword); // 검색창에 입력값 유지용
        
        return "admin/showList";
    }
      
    

    
    @GetMapping("/editPage.do")
    public String editPage(@RequestParam("id") Long performanceId, Model model) {
        Performance performance = performanceService.getPerformance(performanceId);
        if (performance == null) return "redirect:/admin/listPage.do";

        model.addAttribute("performance", performance);
        model.addAttribute("gradeConfigs", performanceService.getGradeConfigs(performanceId));

        List<PerformanceSeatTemplate> templates = performanceService.getTemplates(performanceId);
        model.addAttribute("templates", templates);

        Map<String, Integer> seatGradeMap = new HashMap<>();
        for (PerformanceSeatTemplate t : templates) {
            seatGradeMap.put(t.getSeatNumber().toString(), t.getGradeOrder());
        }

        List<Map<String, Object>> openPeriods = new ArrayList<>();
        Map<String, List<String>> weeklyMap = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        if (performance.getSchedules() != null) {
            // openingTime 기준으로 그룹화
            Map<LocalDateTime, List<PerformanceSchedule>> grouped = performance.getSchedules().stream()
                    .filter(s -> s.getOpeningTime() != null)
                    .collect(Collectors.groupingBy(PerformanceSchedule::getOpeningTime));

            // [핵심 수정] 엔트리를 시간순으로 정렬하여 리스트 생성
            List<Map.Entry<LocalDateTime, List<PerformanceSchedule>>> sortedEntries = grouped.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()) 
                    .collect(Collectors.toList());

            for (Map.Entry<LocalDateTime, List<PerformanceSchedule>> entry : sortedEntries) {
                LocalDateTime ot = entry.getKey();
                List<PerformanceSchedule> schedules = entry.getValue();

                Map<String, Object> p = new HashMap<>();
                p.put("openingTime", ot);

                // HTML 이스케이프 충돌 방지를 위한 포맷팅 문자열 생성
                if (ot != null) {
                    p.put("formattedOpeningTime", ot.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
                } else {
                    p.put("formattedOpeningTime", "");
                }
                
                // 각 세트별 날짜 범위 계산
                LocalDate minDate = schedules.stream()
                        .map(s -> s.getStartTime().toLocalDate())
                        .min(LocalDate::compareTo).orElse(LocalDate.now());
                LocalDate maxDate = schedules.stream()
                        .map(s -> s.getStartTime().toLocalDate())
                        .max(LocalDate::compareTo).orElse(LocalDate.now());
                
                p.put("start", minDate);
                p.put("end", maxDate);
                
                // 해당 회차의 오픈 시간이 지났는지 개별적으로 판단
                boolean isThisSetStarted = now.isAfter(ot);
                p.put("isStarted", isThisSetStarted); 
                
                openPeriods.add(p);

                // 주간 스케줄 데이터 구성
                for (PerformanceSchedule sched : schedules) {
                    String korDay = getKoreanDay(sched.getStartTime().getDayOfWeek());
                    String timeStr = sched.getStartTime().toLocalTime().toString().substring(0, 5);
                    weeklyMap.computeIfAbsent(korDay, k -> new ArrayList<>()).add(timeStr);
                }
            }
        }

        // 모델에 전달
        model.addAttribute("openPeriods", openPeriods);
        
        // 페이지 전체의 '조회 전용 모드'를 결정할 플래그
        model.addAttribute("isStarted", openPeriods.stream().anyMatch(p -> (Boolean)p.get("isStarted")));

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            model.addAttribute("weeklySchedule", mapper.writeValueAsString(weeklyMap));
            model.addAttribute("seatGradeMap", mapper.writeValueAsString(seatGradeMap));
        } catch (Exception e) {
            model.addAttribute("weeklySchedule", "{}");
            model.addAttribute("seatGradeMap", "{}");
        }

        return "admin/showEdit";
    }
    
    
    
    
    //======================================================================
    /**
     * 공연 등록 처리 (ShowInsert)
     * HTML Form에서 submit 하면 이쪽으로 들어옵니다.
     */
    /**
     * 공연 및 좌석 일괄 등록 처리
     * @ModelAttribute를 사용하면 폼 데이터가 DTO에 자동으로 매핑됩니다.
     */
    
    @PostMapping("/showsave.do")
    @ResponseBody 
    public String showInsert(
            @ModelAttribute PerformanceSaveDto dto,
            @RequestParam(value = "posterFile", required = false) MultipartFile posterFile,
            @RequestParam(value = "detailFiles", required = false) List<MultipartFile> detailFiles) {
        
        System.out.println("==> 공연 저장 요청 진입: " + dto.getTitle());
        
        try {
            // 1. DTO에 파일 데이터 주입
            // 파라미터로 받은 파일이 null인지 한번 더 체크하면 안전합니다.
            dto.setPosterFile(posterFile);
            dto.setDetailFiles(detailFiles != null ? detailFiles : new ArrayList<>());
            
            // 2. 서비스 호출
            performanceService.processShowInsert(dto);
            
            // 3. 성공 시 페이지 리다이렉트
            // 목록 페이지가 /admin/listPage.do 이라면 경로를 확인하세요.
            return "<script>alert('공연 등록이 완료되었습니다!'); location.href='/admin/listPage.do';</script>";
            
        } catch (Exception e) {
            // [중요] 상세 에러 로그 출력
            e.printStackTrace();
            return "<script>alert('등록 실패: " + e.getMessage() + "'); history.back();</script>";
        }
    }
    
    
    
    /**
     * 공연 수정 처리 (ShowUpdate)
     * 기존 데이터를 삭제하고 새 데이터를 저장하는 로직을 호출합니다.
     */
    @PostMapping("/showupdate.do")
    @ResponseBody
    public String showUpdate(@ModelAttribute PerformanceSaveDto dto) {
        System.out.println("==> 공연 수정 요청 진입: " + dto.getPerformanceId());
        
        // 1. 파일 확인 로그 (디버깅용)
        if(dto.getPosterFile() != null && !dto.getPosterFile().isEmpty()) {
            System.out.println("포스터 파일 수신: " + dto.getPosterFile().getOriginalFilename());
        }
        
        try {
            // 2. 서비스로 DTO 전달 (DTO 안에 파일 정보가 다 들어있음)
            performanceService.processShowUpdate(dto);
            
            return "<script>alert('공연 수정이 완료되었습니다!'); location.href='/main.do';</script>";
        } catch (Exception e) {
            e.printStackTrace();
            return "<script>alert('수정 실패: " + e.getMessage() + "'); history.back();</script>";
        }
    }
    
    
    
    
    
    
    
    
    /**
     * 공연 삭제 처리
     * /admin/delete.do?id=123
     */
    @GetMapping("/delete.do")
    public String deletePerformance(@RequestParam("id") Long id, RedirectAttributes rttr) {
        System.out.println("==> 관리자 공연 삭제 요청 (ID: " + id + ")");
        
        try {
            // 1. 공연 존재 여부 및 판매 상태 체크 (선택 사항)
            Performance performance = performanceService.getPerformance(id);
            if (performance == null) {
                rttr.addFlashAttribute("errorMsg", "존재하지 않는 공연입니다.");
                return "redirect:/admin/listPage.do";
            }

            // [비즈니스 로직] 만약 이미 판매가 시작된 공연은 삭제를 막고 싶다면?
            // if (isStarted(performance)) { 
            //     rttr.addFlashAttribute("errorMsg", "이미 예매가 진행 중인 공연은 삭제할 수 없습니다.");
            //     return "redirect:/admin/listPage.do";
            // }

            // 2. 서비스 호출하여 삭제 실행 (Cascade 설정이 없다면 내부에서 연관 데이터 모두 삭제 필요)
            performanceService.deletePerformance(id);
            
            rttr.addFlashAttribute("successMsg", "공연이 성공적으로 삭제되었습니다.");
            
        } catch (Exception e) {
            System.err.println("삭제 중 오류 발생: " + e.getMessage());
            rttr.addFlashAttribute("errorMsg", "삭제 중 오류가 발생했습니다. (참조 데이터 확인 필요)");
        }
        
        return "redirect:/admin/listPage.do";
    }   
    
    
    @PostMapping("/seat/set-secret.do")
    @ResponseBody
    public ResponseEntity<String> setSecretSeat(@RequestParam("seatId") Long seatId) {
        try {
            // 서비스에서 좌석 상태를 3으로 바꾸는 메서드 호출
            performanceService.updateSeatToSecret(seatId);
            return ResponseEntity.ok("보유석 설정 완료");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("설정 실패");
        }
    }
  ///////캔슬 관련 컨트롤러 
  ///
  ///
  ///  
    
    
 // 주소를 /cancelled.do 형식으로 변경
    @GetMapping("/cancelled.do")
    public String viewCancelledList(
    		@RequestParam(required = false) Long showIdx,
            @RequestParam(required = false) String reserveNum,
            @RequestParam(required = false) String showTitle, // 추가
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        List<OrderList> cancelledList = performanceService.getCancelledOrders(showIdx, reserveNum, showTitle, startDate, endDate);
        
        model.addAttribute("orders", cancelledList);
        model.addAttribute("showIdx", showIdx);
        model.addAttribute("reserveNum", reserveNum);
        model.addAttribute("showTitle", showTitle); // 추가
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "admin/showCancle"; 
    }   
    
    ////예매율 확인용
    ///
    
    @GetMapping("/stats.do")
    public String showStats(@RequestParam(required = false) String showTitle, Model model) {
        if (showTitle != null && !showTitle.isEmpty()) {
            // 공연별 통계 데이터 로직 호출
            var stats = performanceService.getPerformanceStats(showTitle);
            model.addAttribute("stats", stats);
            model.addAttribute("showTitle", showTitle);
        }
        return "statistics/statisCalendar";
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}
package com.mbc.controller;

import com.mbc.admin.PerformanceListDto;
import com.mbc.admin.PerformanceSaveDto;
import com.mbc.admin.entity.Performance;
import com.mbc.admin.entity.PerformanceSchedule;
import com.mbc.admin.service.AdminPerformanceService;
import com.mbc.admin.entity.PerformanceSeatTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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
            @PageableDefault(size = 10, sort = "performanceId", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {
        
        // 1. 페이징된 엔티티 데이터를 가져옵니다.
        Page<Performance> performancePage = performanceService.findAll(pageable);
        
        // 2. 엔티티를 DTO로 변환합니다. (Stream 사용)
        Page<PerformanceListDto> dtoPage = performancePage.map(PerformanceListDto::new);
        
        // 3. 모델에 담습니다. (getContent()가 아니라 dtoPage 자체를 넘겨도 좋습니다)
        model.addAttribute("performanceList", dtoPage.getContent());
        model.addAttribute("page", dtoPage);
        
        return "admin/showList";
    }
    
    
    
    @GetMapping("/editPage.do")
    public String editPage(@RequestParam("id") Long performanceId, Model model) { 
        System.out.println("==> 관리자 공연 수정 (ID: " + performanceId + ")");
        
        // 1. 공연 정보 조회
        Performance performance = performanceService.getPerformance(performanceId);
        if (performance == null) {
            return "redirect:/admin/listPage.do";
        }
        model.addAttribute("performance", performance);
        
        // 2. 가격 설정 조회
        model.addAttribute("gradeConfigs", performanceService.getGradeConfigs(performanceId));
        
        // 3. 좌석 템플릿 조회 및 JS용 Map 변환
        List<PerformanceSeatTemplate> templates = performanceService.getTemplates(performanceId);
        model.addAttribute("templates", templates);
        
        // [중요] HTML에서 좌석을 그리기 위해 { "1": 1, "2": 2 } 형태의 JSON 생성
        Map<String, Integer> seatGradeMap = new HashMap<>();
        for (PerformanceSeatTemplate t : templates) {
            seatGradeMap.put(t.getSeatNumber().toString(), t.getGradeOrder());
        }

        // 4. 오픈 세트 및 판매 시작 여부(isStarted) 계산
        List<Map<String, Object>> openPeriods = new ArrayList<>();
        Map<String, List<String>> weeklyMap = new HashMap<>();
        String[] days = {"월", "화", "수", "목", "금", "토", "일"};
        for(String d : days) weeklyMap.put(d, new ArrayList<>());

        boolean isStarted = false; // 기본값 false (에러 방지)
        LocalDateTime now = LocalDateTime.now();

        if (performance.getSchedules() != null && !performance.getSchedules().isEmpty()) {
            Map<LocalDateTime, Map<String, Object>> periodGroups = new LinkedHashMap<>();

            for (PerformanceSchedule sched : performance.getSchedules()) {
                LocalDateTime st = sched.getStartTime();
                LocalDateTime ot = sched.getOpeningTime();
                LocalDate sd = st.toLocalDate();

                // 판매 시작 여부 체크: 어떤 회차라도 예매 시작 시간이 지났다면 true
                if (ot != null && now.isAfter(ot)) {
                    isStarted = true;
                }

                // 주간 요일/시간 추출
                String korDay = getKoreanDay(st.getDayOfWeek());
                String timeStr = st.toLocalTime().toString().substring(0, 5); 
                
                if (!weeklyMap.get(korDay).contains(timeStr)) {
                    weeklyMap.get(korDay).add(timeStr);
                }

                // 기간 세트 정보 추출 (OpeningTime 기준 그룹화)
                if (ot != null) {
                    if (!periodGroups.containsKey(ot)) {
                        Map<String, Object> p = new HashMap<>();
                        p.put("openingTime", ot);
                        p.put("start", sd);
                        p.put("end", sd);
                        periodGroups.put(ot, p);
                    } else {
                        Map<String, Object> p = periodGroups.get(ot);
                        if (sd.isBefore((LocalDate)p.get("start"))) p.put("start", sd);
                        if (sd.isAfter((LocalDate)p.get("end"))) p.put("end", sd);
                    }
                }
            }
            openPeriods.addAll(periodGroups.values());
        }

        // 5. 모델에 데이터 담기
        model.addAttribute("isStarted", isStarted); // 에러 해결의 핵심!
        model.addAttribute("openPeriods", openPeriods);
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            // 요일별 시간 JSON
            model.addAttribute("weeklySchedule", mapper.writeValueAsString(weeklyMap));
            // 좌석 배치 JSON
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
    
    @PostMapping("/showsave.do") // HTML 폼의 action 경로와 일치시켜주세요.
    @ResponseBody 
    public String showInsert(@ModelAttribute PerformanceSaveDto dto) {
        System.out.println("==> 공연 저장 요청 진입: " + dto.getTitle());
        
        try {
            // 1. 서비스 호출 (이미지 파일 처리 + 날짜별 회차 생성 + 좌석 30개씩 복사)
            performanceService.processShowInsert(dto);
            
            // 2. 성공 시 알림 및 목록 이동
            return "<script>alert('공연 등록 및 좌석 생성이 완료되었습니다!');  location.href='/main.do';</script>";
            
            
        } catch (Exception e) {
            e.printStackTrace();
            // 에러 발생 시 메시지 출력 후 이전 페이지로
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
        try {
            // 기존 템플릿/가격을 지우고 새로 저장하는 서비스 로직 호출
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
    
    
    
    
    
    
}
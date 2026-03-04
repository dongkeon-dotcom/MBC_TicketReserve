package com.mbc.controller;

import com.mbc.admin.PerformanceSaveDto;
import com.mbc.admin.AdminPerformanceService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminPerformanceService performanceService;

    // 생성자 주입 (롬복 에러 방지용)
    public AdminController(AdminPerformanceService performanceService) {
        this.performanceService = performanceService;
    }
    
    // "admin.insertPage.do  를 통해 관리자의 상품 입력 제어 " 
    @GetMapping("/insertPage.do")
    public String insertPage() { // void를 String으로 변경!
        System.out.println("==>공연등록페이지로 이동 .do");
        return "admin/showInsert"; // 이제 정상적으로 해당 경로의 페이지를 찾아갑니다.
    }
    
    
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
}
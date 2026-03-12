package com.mbc.controller;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mbc.admin.entity.Performance;
import com.mbc.admin.entity.PerformanceSchedule;
import com.mbc.admin.service.AdminPerformanceService;

@Controller
public class IndexController {
	
	
    private final AdminPerformanceService adminService;

    @Autowired
    public IndexController(AdminPerformanceService adminService) {
        this.adminService = adminService;
    }
    
    
    
    @GetMapping({"/", "/index.do"})
    public String index(Model model) {
        return main(model); // 위에서 만드신 main 메서드를 그대로 호출
    }
    
    

    @GetMapping("main.do")
    public String main(Model model) {
        try {
            // 1. 랜덤 공연 리스트 가져오기 (데이터가 없을 경우 빈 리스트 반환)
            List<Performance> allList = adminService.findAllPerformances();
            List<Performance> randomList = (allList != null) ? 
                    allList.stream().limit(8).collect(Collectors.toList()) : Collections.emptyList();
            
            // 2. 가장 빠른 티켓팅 회차 가져오기
            PerformanceSchedule fastSchedule = adminService.findFastestOpeningSchedule();
            
            // 3. 모델에 데이터 전달 (null 체크 강화)
            model.addAttribute("randomProducts", randomList);
            
            if (fastSchedule != null && fastSchedule.getPerformance() != null) {
                model.addAttribute("fastTitle", fastSchedule.getPerformance().getTitle());
                model.addAttribute("fastId", fastSchedule.getPerformance().getPerformanceId());
                model.addAttribute("fastOpeningTime", fastSchedule.getOpeningTime());
            } else {
                // 데이터가 없을 때 템플릿 에러 방지를 위해 명시적 null 처리
                model.addAttribute("fastTitle", null);
                model.addAttribute("fastId", null);
                model.addAttribute("fastOpeningTime", null);
            }
            
        } catch (Exception e) {
            // 에러 발생 시 로그 출력
            e.printStackTrace();
            // 에러 발생 시에도 빈 리스트는 넘겨서 템플릿이 깨지지 않게 함
            model.addAttribute("randomProducts", Collections.emptyList());
            model.addAttribute("fastTitle", null);
        }
        
        return "main";
    }

    
    
    
    
    
    // API는 그대로 유지
    @GetMapping("/api/random-performances")
    @ResponseBody
    public List<Performance> getRandomPerformances() {
        List<Performance> allList = adminService.findAllPerformances();
        Collections.shuffle(allList);
        return allList.stream().limit(8).collect(Collectors.toList());
    }
}
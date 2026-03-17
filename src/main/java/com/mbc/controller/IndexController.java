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
            // 1. 수정된 부분: 전체 리스트가 아닌 'is_featured = 1'인 공연만 가져옴
            // (Service에 findFeaturedPerformances 메서드를 새로 만든다고 가정)
            List<Performance> featuredList = adminService.findFeaturedPerformances();
            
            // 8개까지만 제한 (이미 SQL에서 제한했다면 limit 생략 가능)
            List<Performance> displayList = (featuredList != null) ? 
                    featuredList.stream().limit(8).collect(Collectors.toList()) : Collections.emptyList();
            
            // 2. 가장 빠른 티켓팅 회차 가져오기 (기존 유지)
            PerformanceSchedule fastSchedule = adminService.findFastestOpeningSchedule();
            
            // 3. 모델에 데이터 전달
            // HTML 변수명인 'randomProducts'를 유지해야 프론트엔드 코드가 깨지지 않습니다.
            model.addAttribute("randomProducts", displayList);
            
            if (fastSchedule != null && fastSchedule.getPerformance() != null) {
                model.addAttribute("fastTitle", fastSchedule.getPerformance().getTitle());
                model.addAttribute("fastId", fastSchedule.getPerformance().getPerformanceId());
                model.addAttribute("fastOpeningTime", fastSchedule.getOpeningTime());
            } else {
                model.addAttribute("fastTitle", null);
                model.addAttribute("fastId", null);
                model.addAttribute("fastOpeningTime", null);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
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
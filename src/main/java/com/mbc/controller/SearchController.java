package com.mbc.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mbc.admin.entity.Performance;
import com.mbc.admin.service.AdminPerformanceService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {
	
	private final AdminPerformanceService service;
	
	@GetMapping("list.do")
	public String search(
			@RequestParam(value="keyword", required = false) String keyword,
			@RequestParam(value="page", defaultValue = "0") int page,
			Model model) {
        if (keyword != null && !keyword.trim().isEmpty()) {
        	Pageable pageable = PageRequest.of(page, 10);
            // 기존 서비스의 검색 메서드 호출
        	Page<Performance> searchPage = service.searchByTitle(keyword, pageable);
        	model.addAttribute("list", searchPage.getContent()); // 실제 데이터 리스트
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", searchPage.getTotalPages());
            model.addAttribute("totalElements", searchPage.getTotalElements());
            model.addAttribute("keyword", keyword);
        }
        return "search/list"; 
    }
}

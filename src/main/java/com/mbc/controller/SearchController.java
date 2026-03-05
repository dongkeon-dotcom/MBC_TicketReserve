package com.mbc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.mbc.user.UsersService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {
	
	@GetMapping("main.do")
	void main() {
		System.out.println("==> main.do");
	}

}

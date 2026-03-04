package com.mbc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/user")
@Controller
public class UserController {
	
	@GetMapping("join.do")
	String join(Model model) {
		System.out.println("==> main.do");
		model.addAttribute("isSocial", false);
		return "user/join";
	}

}

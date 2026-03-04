package com.mbc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mbc.user.Users;
import com.mbc.user.UsersService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RequestMapping("/user")
@Controller
@RequiredArgsConstructor
public class UserController {
	
	private final UsersService service;
	
	@GetMapping("join.do")
	String join(Model model) {
		System.out.println("==> main.do");
		// 나중에 if문으로 isSocial인지 아닌지 구분해야됨
		model.addAttribute("isSocial", false);
		return "user/join";
	}
	
	@PostMapping("joinOK.do")
	String joinOK(Users user) {
		
		service.join(user);		
		
		return "redirect:/user/login.do";
	}

	@PostMapping("/checkEmail.do")
	@ResponseBody
	public boolean checkEmail(@RequestParam("userId") String userId) {
		return service.findOne(userId).isPresent();
	}
	
	
	@GetMapping("/login.do")
	public String login() {
		
		return "user/login";
	}
	
	@GetMapping("/loginOK.do")
	public String loginOK(HttpSession session) {
		//null 체크 때문에 int 말고 Integer 형으로
        Integer userIdx = (Integer) session.getAttribute("userIdx");
        String userName = (String) session.getAttribute("userName");

        System.out.println("로그인 성공 유저 PK: " + userIdx);
        System.out.println("로그인 성공 유저 이름: " + userName);
        
        return "redirect:/";
	}
	
	
	
	
}

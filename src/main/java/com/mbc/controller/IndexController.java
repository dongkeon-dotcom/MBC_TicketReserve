package com.mbc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {
	
	@GetMapping("main.do")
	void main() {
		System.out.println("==> main.do");
	}

}

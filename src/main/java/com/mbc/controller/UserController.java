package com.mbc.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mbc.user.Users;
import com.mbc.user.UsersService;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RequestMapping("/user")
@Controller
@RequiredArgsConstructor
public class UserController {
	
	private final UsersService service;
	
	@ResponseBody
	@PostMapping("/mail.do")
	public ResponseEntity<String> EmailSendTest() {
		try {
			service.sendEmail();
			return ResponseEntity.ok("success");
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("fail");
		}
	}
	
	@GetMapping("join.do")
	public String join(Model model) {
		System.out.println("==> main.do");
		// 나중에 if문으로 isSocial인지 아닌지 구분해야됨
		model.addAttribute("isSocial", false);
		return "user/join";
	}
	
	@PostMapping("joinOK.do")
	public String joinOK(Users user) {
		
		service.join(user);		
		
		return "redirect:/user/login.do";
	}

	@PostMapping("/checkEmail.do")
	@ResponseBody
	public boolean checkEmail(@RequestParam("userId") String userId) {
		return service.checkIdExists(userId);
	}
	
	
	@GetMapping("/login.do")
	public String login() {
		
		return "user/login";
	}
	
	@PostMapping("/loginOK.do")
	public String loginOK(
			@RequestParam("userId") String userId,
			@RequestParam("password") String password,
			HttpSession session,
			RedirectAttributes rttr) {
		
		if(userId == null || password == null) {
			rttr.addFlashAttribute("msg","입력 정보가 누락되었습니다.");
			return "redirect:/user/login.do";
		}
		Optional<Users> userOpt = service.findOne(userId);
		
		
		if(userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
			session.setAttribute("user", userOpt.get());
			return "redirect:/"; 
		} else{
			rttr.addFlashAttribute("msg", "아이디 또는 비밀번호가 일치하지 않습니다.");
	        return "redirect:/user/login.do";
		}
		
		//Sprint Security 적용 이후 코드
		//null 체크 때문에 int 말고 Integer 형으로	
        //Integer userIdx = (Integer) session.getAttribute("userIdx");
        //String userName = (String) session.getAttribute("userName");
        //System.out.println("로그인 성공 유저 PK: " + userIdx);
        //System.out.println("로그인 성공 유저 이름: " + userName);        
        //return "redirect:/";
	}
	
	@GetMapping("/logout.do")
	public String logout(HttpSession session) {
		session.invalidate(); //세선 삭제
		return "redirect:/";
	}
	
	@GetMapping("/mypage.do")
	public String mypage(HttpSession session, Model model) {
		
		Users user = (Users)session.getAttribute("user");
		if(user == null) {
			return "redirect:/user/login.do";
		}
		model.addAttribute("user", user);
		
	    // 1. 소셜/일반 로그인 구분해서 유저 정보를 가져오는 메서드 호출
	    //UserVO userVO = getLoginUser();
	    // 2. 로그인 안 되어 있으면 로그인 페이지로
	    //if (userVO == null) {
	        //return "redirect:/user/login.do";
	    //}
	    // 3. 모델에 담기 (이제 userVO.userName에 "홍길동" 같은 진짜 이름이 들어있음)
	    //model.addAttribute("user", userVO); 
		
		return "user/mypage";
	}
	
	@GetMapping("/edit.do")
	public String edit(HttpSession session, Model model) {
		Users loginUser = (Users)session.getAttribute("user");
		Users user = service.getUserById(loginUser.getUserId());
		model.addAttribute("user",user);
		
		return "user/edit";
	}
	
	@PostMapping("/editOK.do")
	public String editOK(Users user, HttpSession session, RedirectAttributes rttr) {
		
		try {
			Users updateUser = service.updateMember(user);
			session.setAttribute("user", updateUser);
			rttr.addFlashAttribute("msg", "회원 정보가 수정되었습니다.");
			return "redirect:/user/mypage.do";
			
		} catch(Exception e) {
			return "redirect:/user/edit.do";
		}
		
		
	}

	@GetMapping("/pwChnage.do")
	public String pwEdit() {
		
		return "user/pwChange";
	}
	
	@PostMapping("/pwChangeOK.do")
	public String pwEditOK(
			@RequestParam String userId,
	        @RequestParam String currentPw,
	        @RequestParam String newPw,
	        RedirectAttributes rttr
			) {
		boolean isMatch = service.checkPassword(userId, currentPw);
		
		if (!isMatch) {
	        rttr.addFlashAttribute("msg", "현재 비밀번호가 일치하지 않습니다.");
	        return "redirect:/user/pwChange.do"; // 실패 시 다시 변경 페이지로
	    }
	    
	    // 비밀번호 업데이트
	    service.updatePassword(userId, newPw);
	    
	    rttr.addFlashAttribute("msg", "비밀번호가 성공적으로 변경되었습니다.");
		
		
		return "redirect:/user/mypage.do";
	}
	
	@GetMapping("reservationList.do")
	public String reservationList() {
		
		return "user/reservationList";
	}
	
	
	
}

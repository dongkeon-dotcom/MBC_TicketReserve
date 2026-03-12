package com.mbc.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mbc.security.SecurityUserDetails;
import com.mbc.user.UserReservationDTO;
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
	public String join(
			@RequestParam(value="social", required = false) String social,
			HttpSession session,
			Model model) {

		Users tempUser = (Users) session.getAttribute("tempUser");
		//System.out.println("유저컨트롤러 join 진입");
		//System.out.println("USER: " + tempUser);
		
		if(social != null && tempUser != null) {
			model.addAttribute("user", tempUser);
			model.addAttribute("isSocial", true);
		} else {
			model.addAttribute("user", new Users());
			model.addAttribute("isSocial", false);
		}

		return "user/join";
	}
	
	@PostMapping("joinOK.do")
	public String joinOK(Users user, HttpSession session) {
		
		//System.out.println("유저컨트롤러 join OK 진입");
		//System.out.println("USER: " + user);
		service.join(user);
		
		session.removeAttribute("tempUser");
		
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

	
	@GetMapping("/mypage.do")
	public String mypage(
			@AuthenticationPrincipal SecurityUserDetails userDetails,
			Model model) {
		
		if (userDetails == null) return "redirect:/user/login.do";
	    
	    // 이미 DB 유저 객체를 들고 있으므로 바로 전달 가능!
	    model.addAttribute("user", userDetails.getUser());
		
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
	public String edit(
			@AuthenticationPrincipal SecurityUserDetails userDetails,
			Model model) {
		
		model.addAttribute("user", userDetails.getUser());
		
		return "user/edit";
	}
	
	@PostMapping("/editOK.do")
	public String editOK(
			@AuthenticationPrincipal SecurityUserDetails userDetails,
			Users user,
			RedirectAttributes rttr) {
		
		try {
			Users updateUser = service.updateMember(user);
			userDetails.setUser(updateUser);
			rttr.addFlashAttribute("msg", "회원 정보가 수정되었습니다.");
			return "redirect:/user/mypage.do";
			
		} catch(Exception e) {
			return "redirect:/user/edit.do";
		}
		
		
	}

	@GetMapping("/pwChange.do")
	public String pwEdit(
			@AuthenticationPrincipal SecurityUserDetails userDetails,
			Model model) {
		
		if (userDetails == null) return "redirect:/user/login.do";
	    
	    model.addAttribute("user", userDetails.getUser());
		
		return "user/pwChange";
	}
	
	@PostMapping("/pwChangeOK.do")
	public String pwEditOK(
			@RequestParam String userId,
	        @RequestParam String currentPw,
	        @RequestParam String newPw,
	        @AuthenticationPrincipal SecurityUserDetails userDetails,
	        RedirectAttributes rttr
			) {
		boolean isMatch = service.checkPassword(userId, currentPw);
		
		if (!isMatch) {
	        rttr.addFlashAttribute("msg", "현재 비밀번호가 일치하지 않습니다.");
	        return "redirect:/user/pwChange.do"; // 실패 시 다시 변경 페이지로
	    }
	    
	    // 비밀번호 업데이트
	    service.updatePassword(userId, newPw);
	    Users updatedUser = service.getUserById(userId);
	    userDetails.setUser(updatedUser);
	    
	    rttr.addFlashAttribute("msg", "비밀번호가 성공적으로 변경되었습니다.");
		
		
		return "redirect:/user/mypage.do";
	}
	
	@GetMapping("reservationList.do")
	public String reservationList(
			@RequestParam(defaultValue = "CONFIRMED") String status,
			@RequestParam(defaultValue = "0") int page,
			@AuthenticationPrincipal SecurityUserDetails userDetails,
			Model model) {
		Pageable pageable = PageRequest.of(page, 5);
		
		Page<UserReservationDTO> reservePage = service.getMyReservations(userDetails.getUser().getUserIdx(), status, pageable);
		model.addAttribute("list", reservePage.getContent());
		model.addAttribute("currentPage", reservePage.getNumber());
		model.addAttribute("totalPages", reservePage.getTotalPages());
		model.addAttribute("currentStatus", status);
		return "user/reservationList";
	}
	
	@GetMapping("reservationOne.do")
	public String reservationOne(@RequestParam String reserveNum, Model model) {
		UserReservationDTO detail = service.getReservationDetail(reserveNum);
		
		model.addAttribute("detail",detail);
		return "user/reservationOne";
	}
	
	@PostMapping("cancelReservation.do")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> cancelReservation(
			@RequestParam long reserveIdx) {
	    Map<String, Object> response = new HashMap<>();
	    try {
	    	boolean isCancelled = service.processCancellation(reserveIdx);	    
		    if (isCancelled) {
		        response.put("success", true);
		        response.put("message", "예매 취소가 완료되었습니다.");
		    } else {
		        response.put("success", false);
		        response.put("message", "이미 취소되었거나 취소할 수 없는 상태입니다.");
		    }	
	    }catch (IllegalArgumentException | IllegalStateException e) {
	        // 서비스에서 던진 구체적인 에러 메시지를 사용자에게 전달
	        response.put("success", false);
	        response.put("message", e.getMessage());
	    } catch (Exception e) {
	        response.put("success", false);
	        response.put("message", "서버 오류로 인해 취소에 실패했습니다.");
	    }
	    
	    return ResponseEntity.ok(response);
	}
	
	@PostMapping("/cancelUser.do")
	public String cancelUser(HttpSession session, Authentication authentication, RedirectAttributes rttr) {
		if (authentication == null || !authentication.isAuthenticated()) {
	        return "redirect:/user/login.do";
	    }

	    // 1. 현재 로그인한 유저 아이디 가져오기
	    String userId = authentication.getName();
	    
	    try {
	        // 2. DB 상태 변경 서비스 호출
	        service.cancelUser(userId);
	        
	        // 3. 스프링 시큐리티 세션 및 HTTP 세션 무효화 (강제 로그아웃)
	        SecurityContextHolder.clearContext(); // 시큐리티 권한 정보 삭제
	        if (session != null) {
	            session.invalidate(); // 세션 무효화
	        }
	        
	        // 4. 탈퇴 성공 메시지 전달
	        rttr.addAttribute("status", "withdrawn");
	        return "redirect:/user/login.do";
	        
	    } catch (Exception e) {
	        rttr.addFlashAttribute("msg", "탈퇴 처리 중 오류가 발생했습니다.");
	        return "redirect:/user/mypage.do";
	    }
	}
	
	
	
	
}

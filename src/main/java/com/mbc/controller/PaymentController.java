package com.mbc.controller;



import com.mbc.admin.service.AdminPerformanceService;
import com.mbc.reservation.OrderList;
import com.mbc.reservation.OrderListRepository;
import com.mbc.user.Users;

import jakarta.servlet.http.HttpSession;

import com.mbc.admin.entity.*;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor // 생성자 주입을 자동으로 해줍니다 (Lombok 필수)
public class PaymentController {

	@Value("${portone.store-id}")
    private String storeId;

    @Value("${portone.channel-key}")
    private String channelKey;
	
	
    // 1. 필드 선언 (이게 없어서 에러가 난 것입니다)
    private final AdminPerformanceService performanceService;
    private final OrderListRepository orderListRepository; // 이름 확인!
    
    
    
    @GetMapping("/payment.do")
    public String beforePaymentPage(@RequestParam("seatId") Long seatId, Model model,HttpSession session) {
        // 1. 좌석 정보 가져오기 (AdminPerformanceService에 메서드 추가 필요할 수 있음)
        // 만약 SeatInventory를 직접 조회하는 메서드가 없다면 추가하세요
        SeatInventory seat = performanceService.findSeatById(seatId); 
        
        // 2. 스케줄 정보 가져오기
        PerformanceSchedule schedule = performanceService.findScheduleById(seat.getSchedule().getScheduleId());
        
        // 3. 공연 정보 가져오기
        Performance performance = performanceService.findById(schedule.getPerformance().getPerformanceId());

        
     // 1. 세션에서 유저 객체 가져오기
        Users loginUser = (Users) session.getAttribute("user");
        
        // 2. 로그인 안 되어 있으면 로그인 페이지로 리다이렉트
        if (loginUser == null) {
            return "redirect:/user/login.do";
        }
        model.addAttribute("userName", loginUser.getName()); // 추가된 이름 정보
        model.addAttribute("userPhone", loginUser.getPhone()); // getPhone() 메서드 사용
        model.addAttribute("userEmail", loginUser.getUserId());
        model.addAttribute("seat", seat);
        model.addAttribute("schedule", schedule);
        model.addAttribute("performance", performance);
        model.addAttribute("storeId", storeId);
        model.addAttribute("channelKey", channelKey);
        
        return "reserve/beforePayment";
 
    
 }
    
    
    @PostMapping("/complete.do")
    public String completeOrder(@RequestParam("paymentId") String paymentId,
                                @RequestParam("totalAmount") Integer totalAmount,
                                @RequestParam("seatId") Long seatId,
                                @RequestParam("performanceId") Long performanceId,
                                HttpSession session,
                                RedirectAttributes rttr) {
    	System.out.println("전달받은 공연 ID: " + performanceId); // 콘솔에 찍히는지 확인
        Users user = (Users) session.getAttribute("user");

        // 1. OrderList 객체 생성 및 정보 세팅
        OrderList order = OrderList.builder()
                .reserveNum(paymentId) // 포트원 결제 ID를 예매 번호로 사용
                .showIdx(performanceId)           // 현재 공연 ID (상황에 맞게 수정 필요)
                .userIdx(user.getUserIdx())
                .name(user.getName())
                .phone(user.getPhone())
                .paymentAmount(totalAmount)
                .status("CONFIRMED")
                .build();

        // 2. DB에 저장
        orderListRepository.save(order);

        // 3. 좌석 상태 업데이트 (기존 서비스 호출)
        performanceService.reserveSeat(seatId);

        rttr.addFlashAttribute("msg", "예매가 성공적으로 완료되었습니다!");
        return "reserve/complete";
        }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}
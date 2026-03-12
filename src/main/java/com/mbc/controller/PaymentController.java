package com.mbc.controller;



import com.mbc.admin.service.AdminPerformanceService;
import com.mbc.reservation.OrderList;
import com.mbc.reservation.OrderListRepository;
import com.mbc.security.SecurityUserDetails;
import com.mbc.user.Users;

import jakarta.servlet.http.HttpSession;

import com.mbc.admin.entity.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public String beforePaymentPage(@RequestParam("seatId") Long seatId, 
                                    Model model, 
                                    @AuthenticationPrincipal SecurityUserDetails userDetails) {
        
        // 1. 로그인 체크 (시큐리티)
        if (userDetails == null) {
            return "redirect:/user/login.do";
        }
        Users loginUser = userDetails.getUser();

        // 2. 좌석 정보 가져오기
        SeatInventory seat = performanceService.findSeatById(seatId); 
        
        // [중요] 5분 선점 시간 검증 (사용자 PK 비교로 수정)
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        
        if (seat.getIsReserved() == 2 && 
            (seat.getReservedAt() == null || seat.getReservedAt().isBefore(fiveMinutesAgo))) {
            seat.setIsReserved(0);
            seat.setReservedAt(null);
            seat.setReservedBy(null); // 서비스에서 String->Long으로 변경되었다면 null
            performanceService.save(seat);
            return "redirect:/reserve/seat.do?scheduleId=" + seat.getSchedule().getScheduleId();
        }
        
        // ★ 중요: session.getId()를 userDetails.getUserIdx().toString()으로 대체
        if (seat.getIsReserved() == 2 && !String.valueOf(userDetails.getUserIdx()).equals(seat.getReservedBy())) {
            return "redirect:/reserve/seat.do?scheduleId=" + seat.getSchedule().getScheduleId();
        }

        // 3. 모델 데이터 전달
        model.addAttribute("userName", loginUser.getName());
        model.addAttribute("userPhone", loginUser.getPhone());
        model.addAttribute("userEmail", loginUser.getUserId());
        model.addAttribute("seat", seat);
        model.addAttribute("schedule", performanceService.findScheduleById(seat.getSchedule().getScheduleId()));
        model.addAttribute("performance", performanceService.findById(seat.getSchedule().getPerformance().getPerformanceId()));
        model.addAttribute("storeId", storeId);
        model.addAttribute("channelKey", channelKey);
        
        return "reserve/beforePayment";
    }
    
    
    
    
    
    
    @PostMapping("/complete.do")
    public String completeOrder(@RequestParam("paymentId") String paymentId,
                                @RequestParam("totalAmount") Integer totalAmount,
                                @RequestParam("seatId") Long seatId,
                                @RequestParam("scheduleId") Long scheduleId,
                                @AuthenticationPrincipal SecurityUserDetails userDetails,
                                RedirectAttributes rttr) {
        
        if (userDetails == null) return "redirect:/user/login.do";
        Users user = userDetails.getUser();

        try {
            SeatInventory seat = performanceService.findSeatById(seatId);
            PerformanceSchedule schedule = performanceService.findScheduleById(scheduleId);

            OrderList order = OrderList.builder()
                    .reserveNum(paymentId)
                    .schedule(schedule)
                    .userIdx(user.getUserIdx())
                    .name(user.getName())
                    .phone(user.getPhone())
                    .seatNum(seat.getSeatNumber())
                    .seatGrade(String.valueOf(seat.getSeatType()))
                    .paymentAmount(totalAmount)
                    .status("CONFIRMED")
                    .reserveDate(LocalDateTime.now())
                    .build();

            orderListRepository.save(order);
            performanceService.reserveSeat(seatId, user.getUserIdx());

            rttr.addFlashAttribute("msg", "예매가 성공적으로 완료되었습니다!");
            return "reserve/complete";
        } catch (Exception e) {
            e.printStackTrace(); 
            rttr.addFlashAttribute("error", "예매 처리 중 오류가 발생했습니다.");
            return "redirect:/";
        }
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}
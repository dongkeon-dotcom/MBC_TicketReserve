package com.mbc.controller;



import com.mbc.admin.service.AdminPerformanceService;
import com.mbc.reservation.OrderList;
import com.mbc.reservation.OrderListRepository;
import com.mbc.user.Users;

import jakarta.servlet.http.HttpSession;

import com.mbc.admin.entity.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
    public String beforePaymentPage(@RequestParam("seatId") Long seatId, Model model, HttpSession session) {
        
        // 1. 좌석 정보 가져오기
        SeatInventory seat = performanceService.findSeatById(seatId); 
        
        // [중요] 5분 선점 시간 검증 로직
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        
        // 조건 1: 선점 중(2)인데, 선점 시간이 5분이 지났다면?
        // -> 좌석 상태를 미예약(0)으로 초기화 후 좌석 선택 페이지로 리다이렉트
        if (seat.getIsReserved() == 2 && 
            (seat.getReservedAt() == null || seat.getReservedAt().isBefore(fiveMinutesAgo))) {
            
            seat.setIsReserved(0);
            seat.setReservedAt(null);
            seat.setReservedBy(null);
            performanceService.save(seat); // 서비스 클래스에 구현해둔 save 메서드 활용
            
            return "redirect:/reserve/seat.do?scheduleId=" + seat.getSchedule().getScheduleId();
        }
        
        // 조건 2: 선점 중(2)인데, 선점한 사람이 내가 아니라면? (다른 사람의 결제 진입 방지)
        // 세션 ID가 일치하지 않으면 좌석 선택 페이지로 돌려보냄
        if (seat.getIsReserved() == 2 && !session.getId().equals(seat.getReservedBy())) {
            return "redirect:/reserve/seat.do?scheduleId=" + seat.getSchedule().getScheduleId();
        }

        // 2. 나머지 정보 가져오기
        PerformanceSchedule schedule = performanceService.findScheduleById(seat.getSchedule().getScheduleId());
        Performance performance = performanceService.findById(schedule.getPerformance().getPerformanceId());

        // 3. 사용자 인증 체크
        Users loginUser = (Users) session.getAttribute("user");
        if (loginUser == null) {
            return "redirect:/user/login.do";
        }

        // 4. 모델 데이터 전달
        model.addAttribute("userName", loginUser.getName());
        model.addAttribute("userPhone", loginUser.getPhone());
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
        
        Users user = (Users) session.getAttribute("user");
        if (user == null) {
            return "redirect:/user/login.do";
        }

        try {
            // 1. 좌석 상태 최종 업데이트 (이미 선점된 좌석을 확정)
            // 기존 reserveSeat를 호출하면 isReserved = 1로 바뀝니다.
            performanceService.reserveSeat(seatId);

            // 2. 주문 정보 저장
            OrderList order = OrderList.builder()
                    .reserveNum(paymentId)
                    .showIdx(performanceId)
                    .userIdx(user.getUserIdx())
                    .name(user.getName())
                    .phone(user.getPhone())
                    .paymentAmount(totalAmount)
                    .status("CONFIRMED")
                    .build();

            orderListRepository.save(order);

            rttr.addFlashAttribute("msg", "예매가 성공적으로 완료되었습니다!");
            return "reserve/complete";

        } catch (ObjectOptimisticLockingFailureException e) {
            // [중요] 두 명이 동시에 결제 버튼을 눌렀을 때, 
            // 한 명은 성공하고 다른 한 명은 여기서 예외가 발생하여 중복 예약을 막습니다.
            rttr.addFlashAttribute("error", "결제 처리 중 문제가 발생했습니다. 좌석이 이미 점유되었을 수 있습니다.");
            return "redirect:/"; // 메인이나 에러 페이지로 리다이렉트
        } catch (Exception e) {
            rttr.addFlashAttribute("error", "예매 처리 중 오류가 발생했습니다.");
            return "redirect:/";
        }
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}
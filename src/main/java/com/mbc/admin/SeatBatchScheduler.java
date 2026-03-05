package com.mbc.admin;

import com.mbc.admin.entity.PerformanceGradeConfig;
import com.mbc.admin.entity.PerformanceSchedule;
import com.mbc.admin.entity.PerformanceSeatTemplate;
import com.mbc.admin.entity.SeatInventory;
import com.mbc.admin.repositiry.PerformanceGradeConfigRepository;
import com.mbc.admin.repositiry.PerformanceScheduleRepository;
import com.mbc.admin.repositiry.PerformanceSeatTemplateRepository;
import com.mbc.admin.repositiry.SeatInventoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
//실제로 3일전에 시간을 확인하고 좌석을 생성하는스케쥴러 
 메인 클래스에 @EnableScheduling  이걸 통해 알아서스케쥴러들이 자동으로 실행 되는 단계 
아니 왜 RequiredArgsConstructor이거 자꾸 사용 안되죠? 나도 좀 편하게 살자 

**/
@Slf4j
@Component
public class SeatBatchScheduler {

    private final PerformanceScheduleRepository scheduleRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final PerformanceSeatTemplateRepository templateRepository;
    private final PerformanceGradeConfigRepository gradeConfigRepository;

    // 직접 생성자 주입 (Lombok 미사용)
    public SeatBatchScheduler(
            PerformanceScheduleRepository scheduleRepository,
            SeatInventoryRepository seatInventoryRepository,
            PerformanceSeatTemplateRepository templateRepository,
            PerformanceGradeConfigRepository gradeConfigRepository) {
        this.scheduleRepository = scheduleRepository;
        this.seatInventoryRepository = seatInventoryRepository;
        this.templateRepository = templateRepository;
        this.gradeConfigRepository = gradeConfigRepository;
    }

    /**
     * 매일 자정(00:00)에 실행
     * 예매 시작일(openingTime) 3일 전인 회차의 좌석 30개를 자동 생성
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void generateSeatsThreeDaysBeforeOpening() {
        // 1. 기준일 계산 (오늘 + 3일)
        LocalDate targetDate = LocalDate.now().plusDays(3);
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(LocalTime.MAX);
        
        // 2. 3일 뒤가 '예매 시작 시간'인 공연 회차들을 조회
        List<PerformanceSchedule> schedules = scheduleRepository.findByOpeningTimeBetween(startOfDay, endOfDay);
        
        for (PerformanceSchedule schedule : schedules) {
            // 3. 중복 생성 방지 체크
            if (seatInventoryRepository.existsByScheduleScheduleId(schedule.getScheduleId())) {
                continue; 
            }

            try {
                Long perfId = schedule.getPerformance().getPerformanceId();

                // 4. 해당 공연의 '좌석 설계도(Template)' 30개 가져오기
                List<PerformanceSeatTemplate> templates = templateRepository.findByPerformanceId(perfId);
                
                // 5. 해당 공연의 '등급별 가격 설정(GradeConfig)' 가져오기 (Map으로 변환해서 찾기 쉽게 함)
                Map<Integer, Integer> priceMap = gradeConfigRepository.findByPerformanceId(perfId)
                        .stream()
                        .collect(Collectors.toMap(PerformanceGradeConfig::getGradeOrder, PerformanceGradeConfig::getGradePrice));

                // 6. 설계도를 바탕으로 판매용 좌석(SeatInventory) 30개 생성
                for (PerformanceSeatTemplate template : templates) {
                    SeatInventory seat = new SeatInventory();
                    seat.setSchedule(schedule); // 회차 연결
                    seat.setSeatNumber(template.getSeatNumber()); // "1" ~ "30"
                    seat.setSeatType(template.getGradeOrder());    // 등급 번호
                    seat.setPrice(priceMap.get(template.getGradeOrder())); // 테이블에서 가져온 가격
                    seat.setIsReserved(0); // 초기값 미예약
                    
                    seatInventoryRepository.save(seat);
                }
                
                log.info("공연 ID {}: 회차 ID {}의 좌석 30개 생성 완료", perfId, schedule.getScheduleId());

            } catch (Exception e) {
                log.error("좌석 자동 생성 실패 (스케줄 ID: {}): {}", schedule.getScheduleId(), e.getMessage());
            }
        }
    }
}
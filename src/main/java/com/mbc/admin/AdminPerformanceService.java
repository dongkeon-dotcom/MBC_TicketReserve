package com.mbc.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mbc.admin.entity.Performance;
import com.mbc.admin.entity.PerformanceGradeConfig;
import com.mbc.admin.entity.PerformanceSchedule;
import com.mbc.admin.entity.SeatInventory;
import com.mbc.admin.entity.VenueSeatMaster;

@Service
@Transactional // 모든 과정이 하나의 트랜잭션으로 묶임
public class AdminPerformanceService {

    // 1. 의존성 필드 (final로 설정하여 불변성 유지)
    private final PerformanceRepository performanceRepository;
    private final VenueSeatMasterRepository venueMasterRepository;
    private final ObjectMapper objectMapper;

    // 2. 수동 생성자 (Lombok @RequiredArgsConstructor 대체)
    public AdminPerformanceService(
            PerformanceRepository performanceRepository, 
            VenueSeatMasterRepository venueMasterRepository
    ) {
        this.performanceRepository = performanceRepository;
        this.venueMasterRepository = venueMasterRepository;
        
        // ObjectMapper를 직접 생성하고 날짜 모듈을 등록합니다.
        // 이렇게 하면 스프링 컨테이너의 영향 없이 라이브러리 존재만으로 동작합니다.
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); 
    }

    /**
     * 공연 등록 및 회차/좌석 자동 생성 메인 로직
     */
    public void processShowInsert(PerformanceSaveDto dto) throws Exception {
        
        // [A] 공연(Performance) 메인 정보 생성 및 저장
        Performance performance = new Performance();
        performance.setTitle(dto.getTitle());
        performance.setStartDate(LocalDate.parse(dto.getStartDate()));
        performance.setEndDate(LocalDate.parse(dto.getEndDate()));
        // (필요 시 이미지 저장 로직 추가: performance.setPosterImageName(...))

     // [B] 등급 설정(GradeConfig) 추가 부분 수정
        for (int i = 0; i < dto.getGradeNames().size(); i++) {
            PerformanceGradeConfig grade = new PerformanceGradeConfig();
            grade.setGradeName(dto.getGradeNames().get(i));
            
            // String을 int로 변환 (콤마가 있다면 제거 후 변환)
            String priceStr = dto.getGradePrices().get(i).replace(",", "");
            grade.setGradePrice(Integer.parseInt(priceStr)); 
            
            grade.setGradeOrder(i + 1);
            performance.addGradeConfig(grade); 
        }

        // [C] JSON 문자열 파싱 (화면에서 넘어온 데이터)
        // 요일별 시간 설정 파싱
        Map<String, List<String>> scheduleMap = objectMapper.readValue(
            dto.getWeeklySchedule(), new TypeReference<Map<String, List<String>>>() {});
            
        // 좌석별 등급 배치도 파싱
        Map<String, Integer> seatGradeMap = objectMapper.readValue(
            dto.getSeatGradeMap(), new TypeReference<Map<String, Integer>>() {});

        // [D] 공연장의 고정 좌석 마스터(30개) 정보 가져오기
        List<VenueSeatMaster> masters = venueMasterRepository.findAll();

        // [E] 공연 기간 루프 (시작일부터 종료일까지 하루씩 증가)
        LocalDate current = performance.getStartDate();
        LocalDate end = performance.getEndDate();

        while (!current.isAfter(end)) {
            // 해당 날짜의 요일(월~일) 문자열 추출
            String dayOfWeek = getKoreanDayOfWeek(current);
            List<String> times = scheduleMap.get(dayOfWeek);

            // 해당 요일에 공연 시간(회차)이 설정되어 있다면 실행
            if (times != null && !times.isEmpty()) {
                for (String timeStr : times) {
                    LocalTime showTime = LocalTime.parse(timeStr);
                    
                    // 1. 회차(Schedule) 객체 생성
                    PerformanceSchedule schedule = new PerformanceSchedule();
                    schedule.setStartTime(current.atTime(showTime)); 
                    schedule.setOpeningTime(LocalDateTime.parse(dto.getOpeningTime()));
                    
                    // 2. 해당 회차에 30개의 좌석(SeatInventory) 생성
                    for (VenueSeatMaster master : masters) {
                        SeatInventory seat = new SeatInventory();
                        
                        // 1. 타입을 String으로 받습니다.
                        String seatNo = master.getSeatNumber(); 
                        seat.setSeatNumber(seatNo); 
                        
                        // 2. seatGradeMap의 Key는 String이므로 seatNo를 그대로 넣습니다.
                        // 만약 드래그 설정이 안 된 좌석은 기본값으로 1번 등급을 부여합니다.
                        int gradeNum = seatGradeMap.getOrDefault(seatNo, 1);
                        
                        // 3. 매칭되는 등급 정보(이름, 가격) 세팅
                        // 리스트 인덱스는 0부터 시작하므로 등급번호(1~5)에서 1을 뺍니다.
                        PerformanceGradeConfig matchedGrade = performance.getGrades().get(gradeNum - 1);
                        
                        seat.setSeatType(matchedGrade.getGradeOrder()); // 등급 번호(1~5)
                        seat.setPrice(matchedGrade.getGradePrice());   // 해당 등급의 가격
                        seat.setIsReserved(0); // 미예약 상태
                        
                        schedule.addSeat(seat); // 회차(Schedule)에 좌석 추가
                    }
                    performance.addSchedule(schedule); // 공연에 회차 추가
                }
            }
            current = current.plusDays(1); // 다음 날짜로 이동
        }

        // [F] 최종 저장 (CascadeType.ALL에 의해 연관된 모든 데이터가 함께 저장됨)
        performanceRepository.save(performance);
    }

    /**
     * 자바 LocalDate 요일을 한국어 요일로 변환
     */
    private String getKoreanDayOfWeek(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }
}
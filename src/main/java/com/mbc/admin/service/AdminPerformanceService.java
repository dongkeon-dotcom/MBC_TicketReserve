package com.mbc.admin.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mbc.admin.FileUtil;
import com.mbc.admin.PerformanceSaveDto;
import com.mbc.admin.entity.Performance;
import com.mbc.admin.entity.PerformanceDetailImage;
import com.mbc.admin.entity.PerformanceGradeConfig;
import com.mbc.admin.entity.PerformanceSchedule;
import com.mbc.admin.entity.PerformanceSeatTemplate;
import com.mbc.admin.entity.SeatInventory;
import com.mbc.admin.entity.VenueSeatMaster;
import com.mbc.admin.repositiry.PerformanceGradeConfigRepository;
import com.mbc.admin.repositiry.PerformanceRepository;
import com.mbc.admin.repositiry.PerformanceScheduleRepository;
import com.mbc.admin.repositiry.PerformanceSeatTemplateRepository;
import com.mbc.admin.repositiry.PerformenceDetailImageRepository;
import com.mbc.admin.repositiry.SeatInventoryRepository;
import com.mbc.admin.repositiry.VenueSeatMasterRepository;
import com.mbc.aws.S3UploaderService;
import com.mbc.reservation.OrderList;
import com.mbc.reservation.OrderListRepository;

@Service
@Transactional // 모든 과정이 하나의 트랜잭션으로 묶임 (하나라도 실패하면 롤백)
public class AdminPerformanceService {

	private final PerformanceRepository performanceRepository;
    private final VenueSeatMasterRepository venueMasterRepository;
    private final PerformanceSeatTemplateRepository templateRepository;
    private final PerformanceScheduleRepository scheduleRepository;     // [추가] 회차 삭제용
    private final PerformanceGradeConfigRepository gradeConfigRepository; // [추가] 가격 설정 삭제용
    private final ObjectMapper objectMapper;
    private final SeatInventoryRepository seatInventoryRepository; // 추가
    private final S3UploaderService s3UploaderService;
    private final PerformenceDetailImageRepository  detailImageRepository ;
    private final  OrderListRepository orderListRepository;
    // 생성자 주입 (모든 리포지토리를 포함하도록 업데이트)
    public AdminPerformanceService(
            PerformanceRepository performanceRepository, 
            VenueSeatMasterRepository venueMasterRepository,
            PerformanceSeatTemplateRepository templateRepository,
            PerformanceScheduleRepository scheduleRepository,      // [추가]
            PerformanceGradeConfigRepository gradeConfigRepository,  // [추가]
            SeatInventoryRepository seatInventoryRepository, // 추가
            S3UploaderService s3UploaderService	,					// AWS S3 Upload용
            PerformenceDetailImageRepository  detailImageRepository,
            OrderListRepository orderListRepository
    ) {
        this.performanceRepository = performanceRepository;
        this.venueMasterRepository = venueMasterRepository;
        this.templateRepository = templateRepository;
        this.scheduleRepository = scheduleRepository;              // [주입]
        this.gradeConfigRepository = gradeConfigRepository;        // [주입]
        this.seatInventoryRepository = seatInventoryRepository;
        this.s3UploaderService = s3UploaderService;					
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); 
        this.detailImageRepository  = detailImageRepository;
        this.orderListRepository =orderListRepository;
    }

    /**
     * [1] 공연 등록 처리 (Insert)
     */
    @Transactional
    public void processShowInsert(PerformanceSaveDto dto) throws Exception {
    	Performance performance = new Performance();
        performance.setTitle(dto.getTitle());
        performance.setStartDate(LocalDate.parse(dto.getStartDate()));
        performance.setEndDate(LocalDate.parse(dto.getEndDate()));

        // [수정] 1. 먼저 공연 정보를 저장해서 ID를 생성합니다.
        performanceRepository.save(performance);

        // [수정] 2. 이제 ID가 생긴 performance 객체를 넘겨서 이미지를 저장합니다.
        saveImages(performance, dto);

        // [수정] 3. 스케줄 및 좌석 생성도 ID가 생성된 performance를 사용합니다.
        List<String> startDates = dto.getOpenStartDates();
        if (startDates != null) {
            for (int i = 0; i < startDates.size(); i++) {
                LocalDate start = LocalDate.parse(startDates.get(i));
                LocalDate end = LocalDate.parse(dto.getOpenEndDates().get(i));
                generateSchedulesForPeriod(performance, start, end, dto, i);
            }
        }
    }
    /**
     * [2] 공연 수정 처리 (Update)
     * 기존 데이터를 삭제 후 재등록하여 정합성을 유지합니다.
     */
    @Transactional
    public void processShowUpdate(PerformanceSaveDto dto) throws Exception {
        // 1. 기존 공연 정보 로드
        Performance performance = performanceRepository.findById(dto.getPerformanceId())
                .orElseThrow(() -> new IllegalArgumentException("해당 공연이 존재하지 않습니다. ID: " + dto.getPerformanceId()));

        // 2. 기본 정보 업데이트 (제목, 날짜 등)
        updatePerformanceBasicInfo(performance, dto);

        // 3. 이미지 업데이트 (포스터 + 상세이미지)
        saveImages(performance, dto); // 아래 정의한 saveImages 메서드 호출

        // 5. 기존 연관 데이터 초기화 (회차/등급/좌석 재설정 시)
        // 주의: 실제 예매된 좌석이 있을 경우 예외 처리가 필요합니다.
        performance.getGrades().clear();
        performance.getSchedules().clear();
        
        List<PerformanceSeatTemplate> oldTemplates = templateRepository.findByPerformance(performance);
        //templateRepository.deleteAll(oldTemplates);

        // 6. 수정된 설정으로 등급 및 회차/좌석 재생성
        List<String> startDates = dto.getOpenStartDates();
        if (startDates != null) {
            for (int i = 0; i < startDates.size(); i++) {
                LocalDate start = LocalDate.parse(startDates.get(i));
                LocalDate end = LocalDate.parse(dto.getOpenEndDates().get(i));
                generateSchedulesForPeriod(performance, start, end, dto, i);
            }
        }

        performanceRepository.save(performance);
    }

    /**
     * [공통] 공연 기본 정보 업데이트 로직
     */
    private void updatePerformanceBasicInfo(Performance performance, PerformanceSaveDto dto) {
        performance.setTitle(dto.getTitle());
        performance.setStartDate(LocalDate.parse(dto.getStartDate()));
        performance.setEndDate(LocalDate.parse(dto.getEndDate()));

    }

    /**
     * [공통] 이미지 저장 로직 (메인 포스터 + 상세 이미지)
     */
    private void saveImages(Performance performance, PerformanceSaveDto dto) throws Exception {
        // 1. 메인 포스터 처리
        if (dto.getPosterFile() != null && !dto.getPosterFile().isEmpty()) {
            String newPosterPath = s3UploaderService.uploadFile(dto.getPosterFile(), "MAIN_POSTER");
            performance.setPosterImageName(newPosterPath);
        }

        // 2. 삭제할 상세 이미지 처리
        if (dto.getDeleteImageIds() != null && !dto.getDeleteImageIds().isEmpty()) {
            for (Long imageId : dto.getDeleteImageIds()) {
                detailImageRepository.deleteById(imageId);
            }
        }

        // 3. 신규 상세 이미지 추가 처리
        if (dto.getDetailFiles() != null && !dto.getDetailFiles().isEmpty()) {
            for (MultipartFile file : dto.getDetailFiles()) {
                if (!file.isEmpty()) {
                    String path = s3UploaderService.uploadFile(file, "DETAIL_IMAGE");
                    PerformanceDetailImage detailImage = new PerformanceDetailImage();
                    detailImage.setPerformance(performance);
                    detailImage.setImageName(path);
                    detailImageRepository.save(detailImage);
                }
            }
        }
    }
    /**
     * [공통] 특정 기간 동안의 회차 및 좌석 인벤토리를 생성하는 핵심 메서드
     */
   
    public void generateSchedulesForPeriod(Performance performance, LocalDate openStart, LocalDate openEnd, PerformanceSaveDto dto, int index) throws Exception {
        
        // [A] 등급 설정(GradeConfig) 생성 (최초 1회만 실행됨)
        if (performance.getGrades().isEmpty()) {
            for (int i = 0; i < dto.getGradeNames().size(); i++) {
                PerformanceGradeConfig grade = new PerformanceGradeConfig();
                grade.setGradeName(dto.getGradeNames().get(i));
                String priceStr = dto.getGradePrices().get(i).replace(",", "");
                grade.setGradePrice(Integer.parseInt(priceStr)); 
                grade.setGradeOrder(i + 1);
                performance.addGradeConfig(grade); 
            }
        }

        // [B] JSON 파싱 - 요일별 시간 목록
        Map<String, List<String>> scheduleMap = objectMapper.readValue(
            dto.getWeeklySchedule(), new TypeReference<Map<String, List<String>>>() {});
        
        // [중요] seatGradeMap 파싱 로직 강화 (복합 객체 대응)
        Map<String, Object> rawSeatMap = objectMapper.readValue(
            dto.getSeatGradeMap(), new TypeReference<Map<String, Object>>() {});
        
        Map<String, Integer> seatGradeMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawSeatMap.entrySet()) {
            Object val = entry.getValue();
            
            // 1. 이미 숫자라면 그대로 사용
            if (val instanceof Integer) {
                seatGradeMap.put(entry.getKey(), (Integer) val);
            } 
            // 2. {grade: 1, ...} 형태의 객체라면 'grade' 키 추출
            else if (val instanceof Map) {
                Map<String, Object> mapVal = (Map<String, Object>) val;
                Object gradeVal = mapVal.get("grade");
                seatGradeMap.put(entry.getKey(), Integer.parseInt(gradeVal.toString()));
            } 
            // 3. 기타(문자열 등)라면 강제 파싱
            else {
                seatGradeMap.put(entry.getKey(), Integer.parseInt(val.toString()));
            }
        }
        
        // [C] 템플릿(설계도) 저장 (최초 1회만 실행됨)
        if (templateRepository.findByPerformance(performance).isEmpty()) {
            for (Map.Entry<String, Integer> entry : seatGradeMap.entrySet()) {
                PerformanceSeatTemplate template = new PerformanceSeatTemplate();
                template.setPerformance(performance);
                template.setSeatNumber(entry.getKey());
                int gradeOrder = (entry.getValue() != null) ? entry.getValue() : 1;
                template.setGradeOrder(gradeOrder);
                templateRepository.save(template);
            }
        }

        List<VenueSeatMaster> masters = venueMasterRepository.findAll();

        // [D] 예매 오픈 시간 파싱
        LocalDateTime openingTime = LocalDateTime.parse(dto.getOpeningTimes().get(index));

        // [E] 날짜별 순회하며 스케줄 생성
        LocalDate current = openStart;
        while (!current.isAfter(openEnd)) {
            String dayOfWeek = getKoreanDayOfWeek(current);
            List<String> times = scheduleMap.get(dayOfWeek);

            if (times != null && !times.isEmpty()) {
                for (String timeStr : times) {
                    LocalDateTime startDateTime = current.atTime(LocalTime.parse(timeStr));

                    boolean isAlreadyExists = performance.getSchedules().stream()
                            .anyMatch(s -> s.getStartTime().equals(startDateTime));
                    if (isAlreadyExists) continue;

                    PerformanceSchedule schedule = new PerformanceSchedule();
                    schedule.setStartTime(startDateTime);
                    schedule.setOpeningTime(openingTime);

                    for (VenueSeatMaster master : masters) {
                        String seatNo = master.getSeatNumber(); 
                        
                        Integer gradeNum = seatGradeMap.get(seatNo);
                        if (gradeNum == null) gradeNum = 1;
                        
                        boolean isSecret = false; 

                        if (gradeNum > performance.getGrades().size()) gradeNum = 1;
                        PerformanceGradeConfig matchedGrade = performance.getGrades().get(gradeNum - 1);
                        
                        SeatInventory seat = SeatInventory.createSeat(
                            schedule, 
                            seatNo, 
                            matchedGrade.getGradeOrder(), 
                            matchedGrade.getGradePrice(),
                            isSecret
                        );
                        
                        schedule.addSeat(seat);
                    }
                    performance.addSchedule(schedule);
                }
            }
            current = current.plusDays(1);
        }
    }

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

    /**
     * 수정을 위해 기존 데이터를 조회하는 용도
     */
    public Performance getPerformance(Long id) {
        return performanceRepository.findById(id).orElse(null);
    }
    
    /**
     * [추가] 수정 페이지용 등급 설정 조회
     */
    public List<PerformanceGradeConfig> getGradeConfigs(Long performanceId) {
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new IllegalArgumentException("공연을 찾을 수 없습니다."));
        return performance.getGrades(); // Performance 엔티티의 @OneToMany 리스트
    }

    /**
     * [추가] 수정 페이지용 좌석 배치도(템플릿) 데이터 조회
     * 질문자님의 구조에서는 첫 번째 회차의 좌석 배치를 가져오거나, 
     * 별도의 Template 테이블이 없다면 VenueSeatMaster를 활용할 수 있습니다.
     */
    public List<PerformanceSeatTemplate> getTemplates(Long performanceId) {
        // 공연 존재 여부 확인
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new IllegalArgumentException("공연을 찾을 수 없습니다. ID: " + performanceId));
        
        // 설계도(Template) 테이블에서 해당 공연의 좌석 데이터를 가져옵니다.
        // 리턴 타입이 List<PerformanceSeatTemplate>이므로 컨트롤러와 일치하게 됩니다.
        return templateRepository.findByPerformance(performance);
    }
    
    
    
    
 // AdminPerformanceService.java

   

    public Page<Performance> searchByTitle(String keyword, Pageable pageable){
    	return performanceRepository.findByTitleContainingIgnoreCase(keyword, pageable);
    }
    

    
    
    /**
     * 공연 삭제 로직
     */
    public void deletePerformance(Long performanceId) {
        // fk 안한줄알았는데 지멋대로 만들었는지 fk를 가지고있습ㄴ디다 
        
        // 1. 가장 하위 데이터인 '재고 좌석(Inventory)'부터 삭제 (FK 해결)
        seatInventoryRepository.deleteByPerformanceId(performanceId);
        
        // 2. 공연 회차(Schedule) 삭제
        scheduleRepository.deleteByPerformanceId(performanceId);
        
        // 3. 가격 설정(GradeConfig) 삭제
        gradeConfigRepository.deleteByPerformanceId(performanceId);
        
        // 4. 좌석 템플릿(Template) 삭제
        templateRepository.deleteByPerformanceId(performanceId);
        
        // 5. 마지막으로 공연(Performance) 삭제
        performanceRepository.deleteById(performanceId);
        
        System.out.println("==> 모든 연관 데이터 삭제 완료 (ID: " + performanceId + ")");
    }

    
    
    
    
 // 1. 해당 공연의 모든 등급 및 가격 설정 가져오기
    public List<PerformanceGradeConfig> getGradesByPerformanceId(Long performanceId) {
        return gradeConfigRepository.findByPerformanceId(performanceId);
    }

    // 2. 해당 공연의 모든 회차 일정 가져오기
    public List<PerformanceSchedule> getSchedulesByPerformanceId(Long performanceId) {
        return scheduleRepository.findByPerformanceId(performanceId);
    }
    
    
    
    //자꾸 예매 페이지에서 무한루프 걸려서 수정용 
    public Performance findById(Long performanceId) {
        // performanceRepository 부분은 본인이 설정한 리포지토리 변수명으로 맞추세요.
        return performanceRepository.findById(performanceId)
                .orElseThrow(() -> new IllegalArgumentException("해당 공연이 존재하지 않습니다. id=" + performanceId));
    }
    
    
    //스케쥴찾기 
    public PerformanceSchedule findScheduleById(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회차를 찾을 수 없습니다. ID: " + scheduleId));
    }
    
    
   //  beforePaymentPage 여기에서 좌석정보를 불러오기위한 매서드 
    public SeatInventory findSeatById(Long seatId) {
        return seatInventoryRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("해당 좌석을 찾을 수 없습니다. ID: " + seatId));
    }
    /**
     * 좌석을 예약 상태로 변경하는 메서드
     */
    @Transactional
    public void reserveSeat(Long seatId, Long userIdx) {
        SeatInventory seat = seatInventoryRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        seat.setIsReserved(1); // 예약 완료
        seat.setReservedBy(String.valueOf(userIdx)); // 예약자 기록
        seatInventoryRepository.save(seat);
    }
    
 // 목록화 하고  공연 기간이지난경우 뒤로 아닌경우 앞으로 배치 되도록 함 
    public Page<Performance> findAll(Pageable pageable) {
        // 1. 모든 공연을 가져온 뒤 자바 메모리에서 정렬 (공연 수가 수백 개 이하일 때 적합)
        List<Performance> all = performanceRepository.findAll();
        LocalDate now = LocalDate.now();

        all.sort((p1, p2) -> {
            boolean p1Expired = p1.getEndDate().isBefore(now);
            boolean p2Expired = p2.getEndDate().isBefore(now);
            
            if (p1Expired && !p2Expired) return 1;  // p1이 지났으면 뒤로
            if (!p1Expired && p2Expired) return -1; // p2가 지났으면 앞으로
            return p2.getPerformanceId().compareTo(p1.getPerformanceId()); // 둘 다 같으면 최신순
        });

        // 2. 수동으로 페이징 처리하여 반환
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), all.size());
        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }
    
    /**
     * [좌석 선점 로직]
     * 동시 접속 방지를 위해 좌석 상태를 2(선점 중)로 변경합니다.
     */
    @Transactional
    public boolean selectSeat(Long seatId, String userId) {
        SeatInventory seat = seatInventoryRepository.findById(seatId)
            .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다."));

        // 1. 이미 예약(1)된 좌석이면 절대 불가
        if (seat.getIsReserved() == 1) return false;

        // 2. 만료 체크 (5분)
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        
        // [중요 수정] 내가 선점했던 좌석이라면, 5분이 지나지 않았어도 
        // "재선점"을 허용해주거나, 5분이 지났다면 당연히 해제 후 다시 선점
        if (seat.getIsReserved() == 2) {
            if (seat.getReservedBy().equals(userId)) {
                // 내가 점유 중인 좌석 -> 그냥 계속 점유(시간 갱신)
                seat.setReservedAt(LocalDateTime.now());
                seatInventoryRepository.save(seat);
                return true;
            } else if (seat.getReservedAt().isBefore(fiveMinutesAgo)) {
                // 다른 사람이 잡았는데 5분 지남 -> 해제 후 내가 잡음
                seat.setIsReserved(0);
            } else {
                // 다른 사람이 잡았고 5분 안 지남 -> 실패
                return false;
            }
        }

        // 3. 선점 로직
        if (seat.getIsReserved() == 0) {
            seat.setIsReserved(2);
            seat.setReservedAt(LocalDateTime.now());
            seat.setReservedBy(userId);
            seatInventoryRepository.save(seat);
            return true;
        }
        return false;
    }
    
    
   //예약사항 저장 매서드 
    public void save(SeatInventory seat) {
        seatInventoryRepository.save(seat);
    }
    
    
    //새고하면 좌석 풀리기 
    @Transactional
    public void cancelSeat(Long seatId, String userId) {
        SeatInventory seat = seatInventoryRepository.findById(seatId).orElse(null);
        // 내가 선점한 좌석(2)인 경우에만 해제
        if (seat != null && seat.getIsReserved() == 2 && userId.equals(seat.getReservedBy())) {
            seat.setIsReserved(0);
            seat.setReservedAt(null);
            seat.setReservedBy(null);
            seatInventoryRepository.save(seat);
        }
    }
    
    
    
    
    
    //메인에서 8개 뽑아서 돌릴려고 
    public List<Performance> findAllPerformances() {
        return performanceRepository.findAll();
    }
    
    
    
    
    
 // 1. 전체 회차 중 가장 빠른 예약 오픈 시간을 가진 회차 찾기
 // AdminPerformanceService.java
    public PerformanceSchedule findFastestOpeningSchedule() {
        return scheduleRepository.findTopByOpeningTimeAfterOrderByOpeningTimeAsc(LocalDateTime.now());
    }
    
    
    
    
    @Transactional
    public void updateSeatToSecret(Long seatId) {
        SeatInventory seat = seatInventoryRepository.findById(seatId)
            .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다."));
        
        // 3 = 보유석으로 상태값 변경
        seat.setIsReserved(3); 
        
        // JPA 더티 체킹으로 자동 저장되지만, 명시적으로 save를 호출해도 무방합니다.
        seatInventoryRepository.save(seat);
    }
    

    //1인당 회차 당 1매 매수제한 
    public boolean hasAlreadyReserved(Long userIdx, Long scheduleId) {
    // 1. 해당 사용자가 특정 회차에 예매한 내역이 있는지 확인
    // OrderListRepository에 메서드가 없다면 생성해야 합니다.
 // "CANCELLED" 상태가 아닌 데이터가 존재하는지 확인
    return orderListRepository.existsByUserIdxAndSchedule_ScheduleIdAndStatusNot(
        userIdx, 
        scheduleId, 
        "CANCELLED"
    );
}

    
    
    
    // 티켓취소시 다시 좌석 풀리게 하기 위한 매서드 내일의 내가 쓰겟지...
    
    @Transactional
    public void cancelReservation(Long orderIdx) {
        // 1. 주문 내역 조회
        OrderList order = orderListRepository.findById(orderIdx)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문 정보를 찾을 수 없습니다."));

        // 2. 이미 취소된 주문인지 확인
        if ("CANCELED".equals(order.getStatus())) {
            throw new IllegalStateException("이미 취소된 주문입니다.");
        }

        // 3. 연결된 좌석 정보 조회 (order의 schedule과 seatNumber로 특정)
        // ※ 주의: SeatInventory에 해당 정보가 없다면 예외 발생
        SeatInventory seat = seatInventoryRepository.findByScheduleAndSeatNumber(
                order.getSchedule(), 
                order.getSeatNum()
        ).orElseThrow(() -> new IllegalArgumentException("해당 좌석 정보를 찾을 수 없습니다."));

        // 4. 좌석 상태를 '예매 가능(0)'으로 초기화
        seat.setIsReserved(0);
        seat.setReservedAt(null);
        seat.setReservedBy(null);

        // 5. 주문 상태 변경 및 취소 시간 기록
        order.setStatus("CANCELLED");
        order.setCancelDate(LocalDateTime.now());

        // 저장 (JPA는 @Transactional이 끝나면 변경사항을 자동으로 DB에 반영합니다)
        orderListRepository.save(order);
        seatInventoryRepository.save(seat);
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
}
package com.mbc.admin.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
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
import com.mbc.admin.repositiry.SeatInventoryRepository;
import com.mbc.admin.repositiry.VenueSeatMasterRepository;
import com.mbc.aws.S3UploaderService;

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
    
    // 생성자 주입 (모든 리포지토리를 포함하도록 업데이트)
    public AdminPerformanceService(
            PerformanceRepository performanceRepository, 
            VenueSeatMasterRepository venueMasterRepository,
            PerformanceSeatTemplateRepository templateRepository,
            PerformanceScheduleRepository scheduleRepository,      // [추가]
            PerformanceGradeConfigRepository gradeConfigRepository,  // [추가]
            SeatInventoryRepository seatInventoryRepository, // 추가
            S3UploaderService s3UploaderService						// AWS S3 Upload용
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

        // 1. 이미지 저장 (연관관계 편의 메서드 내부에서 setPerformance 호출됨)
        saveImages(performance, dto);

        // 2. 공연 저장 (ID 생성)
        performanceRepository.save(performance);

        // 3. 스케줄 및 좌석 생성
        List<String> startDates = dto.getOpenStartDates();
        if (startDates != null) {
            for (int i = 0; i < startDates.size(); i++) {
                LocalDate start = LocalDate.parse(startDates.get(i));
                LocalDate end = LocalDate.parse(dto.getOpenEndDates().get(i));
                generateSchedulesForPeriod(performance, start, end, dto, i);
            }
        }
        // @Transactional에 의해 메서드 종료 시 자동 커밋됨
    }
    /**
     * [2] 공연 수정 처리 (Update)
     * 기존 데이터를 삭제 후 재등록하여 정합성을 유지합니다.
     */
    public void processShowUpdate(PerformanceSaveDto dto) throws Exception {
    	// 1. 기존 공연 정보 로드
        Performance performance = performanceRepository.findById(dto.getPerformanceId())
                .orElseThrow(() -> new IllegalArgumentException("해당 공연이 존재하지 않습니다. ID: " + dto.getPerformanceId()));

        // 2. 기본 정보 업데이트 (제목, 날짜 등)
        updatePerformanceBasicInfo(performance, dto);

        // 3. 이미지 업데이트 (새 파일이 있을 때만 추가 저장)
        saveImages(performance, dto);

        // 4. [중요] 판매 시작 여부 체크 (비즈니스 로직에 따라 추가 가능)
        // 만약 이미 예매가 진행된 좌석이 있다면 삭제 시 에러가 발생할 수 있습니다.
        
        // 5. 기존 연관 데이터 초기화
        // orphanRemoval = true 설정이 되어 있다면 clear() 만으로 DB에서 삭제됩니다.
        performance.getGrades().clear();
        performance.getSchedules().clear();
        
        // 기존 템플릿(좌석 배치도)도 현재 수정된 드래그 정보로 갱신하기 위해 삭제
        List<PerformanceSeatTemplate> oldTemplates = templateRepository.findByPerformance(performance);
        templateRepository.deleteAll(oldTemplates);

        // 6. 수정된 설정으로 등급 및 회차/좌석 재생성
        // 수정 페이지에서 전달된 다중 오픈 세트 리스트를 순회하며 생성합니다.
        List<String> startDates = dto.getOpenStartDates();
        if (startDates != null) {
            for (int i = 0; i < startDates.size(); i++) {
                LocalDate start = LocalDate.parse(startDates.get(i));
                LocalDate end = LocalDate.parse(dto.getOpenEndDates().get(i));
                
                // 재사용: 등록 시 사용했던 로직을 그대로 호출하여 정합성 유지
                generateSchedulesForPeriod(performance, start, end, dto, i);
            }
        }

        // 7. 변경 감지(Dirty Checking)에 의해 자동 반영되지만, 명시적으로 save
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
        // 메인 포스터 저장
        if (dto.getPosterFile() != null && !dto.getPosterFile().isEmpty()) {
            String savedPosterName = s3UploaderService.uploadFile(dto.getPosterFile(),"MAIN_POSTER");
            performance.setPosterImageName(savedPosterName); 
        }

        // 상세 이미지 저장
        if (dto.getDetailFiles() != null && !dto.getDetailFiles().isEmpty()) {
            for (MultipartFile detailFile : dto.getDetailFiles()) {
                if (!detailFile.isEmpty()) {
                    String savedName = s3UploaderService.uploadFile(detailFile,"DETAIL_IMAGE");
                    if (savedName != null) {
                        PerformanceDetailImage detailEntity = new PerformanceDetailImage();
                        detailEntity.setImageName(savedName);
                        performance.addDetailImage(detailEntity); 
                    }
                }
            }
        }
    }

    /**
     * [공통] 특정 기간 동안의 회차 및 좌석 인벤토리를 생성하는 핵심 메서드
     */
public void generateSchedulesForPeriod(Performance performance, LocalDate openStart, LocalDate openEnd, PerformanceSaveDto dto, int index) throws Exception {
        
        // [A] 등급 설정(GradeConfig) 생성
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

        // [B] JSON 파싱
        Map<String, List<String>> scheduleMap = objectMapper.readValue(
            dto.getWeeklySchedule(), new TypeReference<Map<String, List<String>>>() {});
        
        Map<String, Map<String, Object>> seatGradeMap = objectMapper.readValue(
            dto.getSeatGradeMap(), new TypeReference<Map<String, Map<String, Object>>>() {});
        
        // [C] 템플릿(설계도) 저장
        if (templateRepository.findByPerformance(performance).isEmpty()) {
            for (Map.Entry<String, Map<String, Object>> entry : seatGradeMap.entrySet()) {
                PerformanceSeatTemplate template = new PerformanceSeatTemplate();
                template.setPerformance(performance);
                template.setSeatNumber(entry.getKey());
                
                Object gradeVal = entry.getValue().get("grade");
                int gradeOrder = (gradeVal != null) ? Integer.parseInt(String.valueOf(gradeVal)) : 1;
                
                template.setGradeOrder(gradeOrder);
                templateRepository.save(template);
            }
        }

        List<VenueSeatMaster> masters = venueMasterRepository.findAll();

        // [D] 예매 오픈 시간 파싱
        String openingTimeStr = dto.getOpeningTimes().get(index); 
        LocalDateTime openingTime = LocalDateTime.parse(openingTimeStr);

        // [E] 날짜별 순회하며 스케줄 생성
        LocalDate current = openStart;
        while (!current.isAfter(openEnd)) {
            String dayOfWeek = getKoreanDayOfWeek(current);
            List<String> times = scheduleMap.get(dayOfWeek);

            if (times != null && !times.isEmpty()) {
                for (String timeStr : times) {
                    PerformanceSchedule schedule = new PerformanceSchedule();
                    schedule.setStartTime(current.atTime(LocalTime.parse(timeStr)));
                    schedule.setOpeningTime(openingTime);

                    for (VenueSeatMaster master : masters) {
                        String seatNo = master.getSeatNumber(); 
                        
                        // 1. 데이터 파싱
                        Map<String, Object> seatData = seatGradeMap.get(seatNo);
                        int gradeNum = 1;
                        boolean isSecret = false; 

                        if (seatData != null) {
                            if (seatData.get("grade") != null) {
                                gradeNum = Integer.parseInt(String.valueOf(seatData.get("grade")));
                            }
                            if (seatData.get("isSecret") != null) {
                                isSecret = Boolean.parseBoolean(String.valueOf(seatData.get("isSecret")));
                            }
                        }
                        
                        // 2. 등급 매칭
                        if (gradeNum > performance.getGrades().size()) gradeNum = 1;
                        PerformanceGradeConfig matchedGrade = performance.getGrades().get(gradeNum - 1);
                        
                        // 3. [핵심] 수정된 createSeat 호출 (5개 파라미터 전달)
                        // 주의: SeatInventory.java의 createSeat 메서드도 파라미터 5개짜리로 수정되어 있어야 합니다.
                        SeatInventory seat = SeatInventory.createSeat(
                            schedule, 
                            seatNo, 
                            matchedGrade.getGradeOrder(), 
                            matchedGrade.getGradePrice(),
                            isSecret // 마지막에 보유석 여부 전달
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
    public void reserveSeat(Long seatId) {
        // 1. 해당 seatId로 좌석을 조회 (없으면 에러 처리)
        // 주의: 프로젝트 내 실제 Seat 엔티티 이름에 맞게 수정하세요.
        SeatInventory seat = seatInventoryRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        // 2. 예약 상태 업데이트 (예: isReserved를 1로 변경)
        // 주의: 엔티티의 필드명에 맞게 수정하세요 (예: setReserved(true) 등)
        seat.setIsReserved(1); 
        
        // 3. 변경 사항 저장
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
        // 1. 좌석 조회 (Optimistic Lock을 위해 버전 체크가 포함됨)
        SeatInventory seat = seatInventoryRepository.findById(seatId)
            .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다. ID: " + seatId));

        // 2. 이미 예약(1)된 좌석인지 확인
        if (seat.getIsReserved() == 1) {
            return false;
        }

        // 3. 선점 중(2)인데 5분이 지나지 않았는지 확인 // 여기서 시간 조절 가능 
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        if (seat.getIsReserved() == 2 && seat.getReservedAt() != null 
            && seat.getReservedAt().isAfter(fiveMinutesAgo)) {
            return false; // 아직 5분이 안 지남 -> 선점 불가
        }

        // 4. 좌석 선점 처리 (isReserved = 2)
        seat.setIsReserved(2);
        seat.setReservedAt(LocalDateTime.now());
        seat.setReservedBy(userId);
        
        // 데이터 저장 (이 시점에 @Version이 자동 체크됨)
        seatInventoryRepository.save(seat); 
        return true;
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
    
    
    
    
    
    
    
    
    
    
    
    
    
}
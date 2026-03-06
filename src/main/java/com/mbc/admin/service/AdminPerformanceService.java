package com.mbc.admin.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
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
    
    // 생성자 주입 (모든 리포지토리를 포함하도록 업데이트)
    public AdminPerformanceService(
            PerformanceRepository performanceRepository, 
            VenueSeatMasterRepository venueMasterRepository,
            PerformanceSeatTemplateRepository templateRepository,
            PerformanceScheduleRepository scheduleRepository,      // [추가]
            PerformanceGradeConfigRepository gradeConfigRepository,  // [추가]
            SeatInventoryRepository seatInventoryRepository // 추가
    ) {
        this.performanceRepository = performanceRepository;
        this.venueMasterRepository = venueMasterRepository;
        this.templateRepository = templateRepository;
        this.scheduleRepository = scheduleRepository;              // [주입]
        this.gradeConfigRepository = gradeConfigRepository;        // [주입]
        this.seatInventoryRepository = seatInventoryRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); 
    }

    /**
     * [1] 공연 등록 처리 (Insert)
     */
    @Transactional
    public void processShowInsert(PerformanceSaveDto dto) throws Exception {
        Performance performance = new Performance();
        
        // 1. 기본 정보 매핑 및 이미지 저장
        updatePerformanceBasicInfo(performance, dto); 
        saveImages(performance, dto);
        
        // 2. [중요] 부모인 Performance를 먼저 DB에 저장합니다.
        // 이렇게 해야 performance_id(PK)가 생성되어 자식들이 참조할 수 있습니다.
        performance = performanceRepository.save(performance);

        // 3. 이제 발급된 ID가 있는 상태로 자식 데이터들(스케줄, 템플릿)을 생성합니다.
        List<String> startDates = dto.getOpenStartDates();
        if (startDates != null) {
            for (int i = 0; i < startDates.size(); i++) {
                LocalDate start = LocalDate.parse(startDates.get(i));
                LocalDate end = LocalDate.parse(dto.getOpenEndDates().get(i));
                
                // 이제 에러 없이 자식 테이블들에 데이터가 들어갑니다.
                generateSchedulesForPeriod(performance, start, end, dto, i);
            }
        }

        // 4. (선택사항) 모든 자식 관계 설정이 끝난 후 한 번 더 저장하거나, 
        // @Transactional이 있으므로 더티 체킹에 의해 자동으로 최종 반영됩니다.
        performanceRepository.save(performance);
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
            String savedPosterName = FileUtil.saveFile(dto.getPosterFile());
            performance.setPosterImageName(savedPosterName); 
        }

        // 상세 이미지 저장
        if (dto.getDetailFiles() != null && !dto.getDetailFiles().isEmpty()) {
            for (MultipartFile detailFile : dto.getDetailFiles()) {
                if (!detailFile.isEmpty()) {
                    String savedName = FileUtil.saveFile(detailFile);
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
        
        // [A] 등급 설정(GradeConfig) 생성 (이미 존재하지 않을 때만 실행)
        if (performance.getGrades().isEmpty()) {
            for (int i = 0; i < dto.getGradeNames().size(); i++) {
                PerformanceGradeConfig grade = new PerformanceGradeConfig();
                grade.setGradeName(dto.getGradeNames().get(i));
                // 가격 콤마 제거 로직 포함
                String priceStr = dto.getGradePrices().get(i).replace(",", "");
                grade.setGradePrice(Integer.parseInt(priceStr)); 
                grade.setGradeOrder(i + 1);
                performance.addGradeConfig(grade); 
            }
        }

        // [B] JSON 파싱
        Map<String, List<String>> scheduleMap = objectMapper.readValue(
            dto.getWeeklySchedule(), new TypeReference<Map<String, List<String>>>() {});
        Map<String, Integer> seatGradeMap = objectMapper.readValue(
            dto.getSeatGradeMap(), new TypeReference<Map<String, Integer>>() {});
        
        // [C] 템플릿(설계도) 저장 - 수정 시에도 새로운 배치 정보를 저장해야 함
        if (templateRepository.findByPerformance(performance).isEmpty()) {
            for (Map.Entry<String, Integer> entry : seatGradeMap.entrySet()) {
                PerformanceSeatTemplate template = new PerformanceSeatTemplate();
                template.setPerformance(performance);
                template.setSeatNumber(entry.getKey());
                template.setGradeOrder(entry.getValue());
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
                        SeatInventory seat = new SeatInventory();
                        String seatNo = master.getSeatNumber(); 
                        seat.setSeatNumber(seatNo); 
                        
                        int gradeNum = seatGradeMap.getOrDefault(seatNo, 1);
                        // 등급 인덱스 초과 방지
                        if (gradeNum > performance.getGrades().size()) gradeNum = 1;
                        
                        PerformanceGradeConfig matchedGrade = performance.getGrades().get(gradeNum - 1);
                        
                        seat.setSeatType(matchedGrade.getGradeOrder());
                        seat.setPrice(matchedGrade.getGradePrice());
                        seat.setIsReserved(0);
                        
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

    public List<Performance> getAllPerformances() {
        // 최신 등록순으로 보고 싶다면 리포지토리에 findAllByOrderByPerformanceIdDesc() 등을 추가해 사용하세요.
        return performanceRepository.findAll();
    }
    

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
    
}
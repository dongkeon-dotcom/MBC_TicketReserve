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
import com.mbc.admin.entity.PerformanceDetailImage;
import com.mbc.admin.entity.PerformanceGradeConfig;
import com.mbc.admin.entity.PerformanceSchedule;
import com.mbc.admin.entity.SeatInventory;
import com.mbc.admin.entity.VenueSeatMaster;
import org.springframework.web.multipart.MultipartFile;
@Service
@Transactional // 모든 과정이 하나의 트랜잭션으로 묶임
public class AdminPerformanceService {
	private final PerformanceRepository performanceRepository;
    private final VenueSeatMasterRepository venueMasterRepository;
    private final ObjectMapper objectMapper;

    public AdminPerformanceService(
            PerformanceRepository performanceRepository, 
            VenueSeatMasterRepository venueMasterRepository
    ) {
        this.performanceRepository = performanceRepository;
        this.venueMasterRepository = venueMasterRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); 
    }

    public void processShowInsert(PerformanceSaveDto dto) throws Exception {
        
    	// [A] 공연(Performance) 메인 정보 생성
        Performance performance = new Performance();
        performance.setTitle(dto.getTitle());
        performance.setStartDate(LocalDate.parse(dto.getStartDate()));
        performance.setEndDate(LocalDate.parse(dto.getEndDate()));

        // 1. 메인 포스터 저장
        if (dto.getPosterFile() != null && !dto.getPosterFile().isEmpty()) {
            String savedPosterName = FileUtil.saveFile(dto.getPosterFile());
            performance.setPosterImageName(savedPosterName); 
        }

        // 📸 [수정 부분] 2. 상세 이미지들 저장 (performance_detail_image 테이블용)
        if (dto.getDetailFiles() != null && !dto.getDetailFiles().isEmpty()) {
            for (MultipartFile detailFile : dto.getDetailFiles()) {
                if (!detailFile.isEmpty()) {
                    String savedName = FileUtil.saveFile(detailFile);
                    
                    if (savedName != null) {
                        // 상세 이미지 전용 엔티티 객체 생성
                        PerformanceDetailImage detailEntity = new PerformanceDetailImage();
                        detailEntity.setImageName(savedName); // 저장된 파일명
                        // detailEntity.setOriginalName(detailFile.getOriginalFilename()); // (선택사항)
                        
                        // Performance와 연관관계 설정 (CascadeType.ALL 설정 시 같이 저장됨)
                        performance.addDetailImage(detailEntity); 
                    }
                }
            }
        }
        
        // --- 📸 이미지 저장 로직 끝 ---

        // [B] 등급 설정(GradeConfig)
        for (int i = 0; i < dto.getGradeNames().size(); i++) {
            PerformanceGradeConfig grade = new PerformanceGradeConfig();
            grade.setGradeName(dto.getGradeNames().get(i));
            String priceStr = dto.getGradePrices().get(i).replace(",", "");
            grade.setGradePrice(Integer.parseInt(priceStr)); 
            grade.setGradeOrder(i + 1);
            performance.addGradeConfig(grade); 
        }

        // [C] JSON 파싱
        Map<String, List<String>> scheduleMap = objectMapper.readValue(
            dto.getWeeklySchedule(), new TypeReference<Map<String, List<String>>>() {});
        Map<String, Integer> seatGradeMap = objectMapper.readValue(
            dto.getSeatGradeMap(), new TypeReference<Map<String, Integer>>() {});

        // [D] 마스터 정보
        List<VenueSeatMaster> masters = venueMasterRepository.findAll();

        // [E] 기간별 회차 및 좌석 생성
        LocalDate current = performance.getStartDate();
        LocalDate end = performance.getEndDate();

        while (!current.isAfter(end)) {
            String dayOfWeek = getKoreanDayOfWeek(current);
            List<String> times = scheduleMap.get(dayOfWeek);

            if (times != null && !times.isEmpty()) {
                for (String timeStr : times) {
                    LocalTime showTime = LocalTime.parse(timeStr);
                    
                    PerformanceSchedule schedule = new PerformanceSchedule();
                    schedule.setStartTime(current.atTime(showTime)); 
                    schedule.setOpeningTime(LocalDateTime.parse(dto.getOpeningTime()));
                    
                    for (VenueSeatMaster master : masters) {
                        SeatInventory seat = new SeatInventory();
                        String seatNo = master.getSeatNumber(); 
                        seat.setSeatNumber(seatNo); 
                        
                        int gradeNum = seatGradeMap.getOrDefault(seatNo, 1);
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

        // [F] 저장
        performanceRepository.save(performance);
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
     * 특정 공연에 대해 지정된 기간의 회차와 좌석을 생성하는 핵심 메서드
     */
    public void generateSchedulesForPeriod(Performance performance, LocalDate openStart, LocalDate openEnd, PerformanceSaveDto dto) throws Exception {
        
        // JSON 파싱 (요일별 시간 및 좌석 배치도)
        Map<String, List<String>> scheduleMap = objectMapper.readValue(dto.getWeeklySchedule(), new TypeReference<Map<String, List<String>>>() {});
        Map<String, Integer> seatGradeMap = objectMapper.readValue(dto.getSeatGradeMap(), new TypeReference<Map<String, Integer>>() {});
        List<VenueSeatMaster> masters = venueMasterRepository.findAll();

        LocalDate current = openStart;
        while (!current.isAfter(openEnd)) {
            String dayOfWeek = getKoreanDayOfWeek(current);
            List<String> times = scheduleMap.get(dayOfWeek);

            if (times != null) {
                for (String timeStr : times) {
                    PerformanceSchedule schedule = new PerformanceSchedule();
                    schedule.setStartTime(current.atTime(LocalTime.parse(timeStr)));
                    schedule.setOpeningTime(LocalDateTime.parse(dto.getOpeningTime()));
                    schedule.setIsOpened(1); // 이 기간은 오픈 대상이므로 1로 설정

                    for (VenueSeatMaster master : masters) {
                        SeatInventory seat = new SeatInventory();
                        seat.setSeatNumber(master.getSeatNumber());
                        
                        int gradeNum = seatGradeMap.getOrDefault(master.getSeatNumber(), 1);
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
}
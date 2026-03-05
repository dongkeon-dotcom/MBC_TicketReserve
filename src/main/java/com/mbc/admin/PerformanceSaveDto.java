package com.mbc.admin;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Getter @Setter
public class PerformanceSaveDto {
    private String title;           // 공연 제목
    private String startDate;       // 시작일 (2026-03-01)
    private String endDate;         // 종료일 (2026-04-02)
    private String openingTime;     // 티켓 오픈 시간
    
 // ⭐ [추가] 이번 티켓팅으로 실제 생성할 회차 기간
    private String openStartDate; // 티켓팅 대상 시작일
    private String openEndDate;   // 티켓팅 대상 종료일
    
    // 파일 업로드 (Entity에는 파일 자체가 아니라 파일 이름만 저장할 거임)
    private MultipartFile posterFile; 
    private List<MultipartFile> detailFiles;

    // 가변 등급 정보
    private List<String> gradeNames;   // ["VIP", "R석", "S석"]
    private List<String> gradePrices; // [150000, 120000, 80000]
    
 // --- 이 두 부분이 추가되어야 합니다! ---
    private String weeklySchedule; // JSON 문자열로 들어옵니다: {"월":["14:00"], ...}
    private String seatGradeMap;   // JSON 문자열로 들어옵니다: {"1":1, "2":2, ...}
}
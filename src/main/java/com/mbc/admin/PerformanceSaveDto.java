
package com.mbc.admin;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Getter 
@Setter
@ToString // 디버깅용 로그 찍을 때 편합니다.
public class PerformanceSaveDto {
    
    private Long performanceId; // 수정 시 필수
    private String title;
    private String startDate;
    private String endDate;
 // [중요 수정] HTML의 name="openStartDates"와 매핑되도록 List로 변경
    private List<String> openStartDates; 
    private List<String> openEndDates;   
    private List<String> openingTimes;
    
    private MultipartFile posterFile; 
    private List<MultipartFile> detailFiles;

    private List<String> gradeNames;
    private List<String> gradePrices;
    
    private String weeklySchedule;
    private String seatGradeMap;
}
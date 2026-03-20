
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
    private List<Long> deleteImageIds;          // 삭제할 이미지 ID들
    private List<String> isSecret; // [추가] 체크된 등급 번호들이 넘어옵니다 (예: ["1", "3"])
    private List<String> gradeNames;
    private List<String> gradePrices;
    
    private String weeklySchedule;
    private String seatGradeMap;
 // [추가] 노출 여부 필드 (HTML의 name="isVisible"과 매핑)
    // DB 타입이 tinyint이므로 Integer 또는 boolean으로 받으면 됩니다.
    private Integer isVisible; 
    
    // [선택 사항 추가] 추천 공연 여부 필드
    private Integer isFeatured;
}
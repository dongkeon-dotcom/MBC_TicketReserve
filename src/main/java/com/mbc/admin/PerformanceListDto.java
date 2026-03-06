
package com.mbc.admin;

import com.mbc.admin.entity.Performance;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PerformanceListDto {
    private Long performanceId;
    private String title;
    private String posterImageName;
    private String startDate;
    private String endDate;

    // 엔티티를 받아서 DTO로 변환하는 생성자
    public PerformanceListDto(Performance p) {
        this.performanceId = p.getPerformanceId();
        this.title = p.getTitle();
        this.posterImageName = p.getPosterImageName();
        // 날짜를 String으로 변환해서 저장 (화면 출력용)
        this.startDate = p.getStartDate() != null ? p.getStartDate().toString() : "";
        this.endDate = p.getEndDate() != null ? p.getEndDate().toString() : "";
    }
}
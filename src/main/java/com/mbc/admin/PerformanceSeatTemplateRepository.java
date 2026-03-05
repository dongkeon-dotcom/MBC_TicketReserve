package com.mbc.admin;
//관리자가 공연 저장 단계에서 지정한 좌석의 등급과 이름 가격을 임시로 저장해두고 3일전 생성을 위해 사용 

import com.mbc.admin.entity.PerformanceSeatTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PerformanceSeatTemplateRepository extends JpaRepository<PerformanceSeatTemplate, Long> {

    
   /** List<PerformanceSeatTemplate> findByPerformanceId(Long performanceId);
기존에 이거였는데 
    PerformanceSeatTemplate안에 performanceId라는 숫자 필드가 없고 Performance performance라는 객체 필드만있어서 
@Query사용으로 수정합니다 아 그지같이 구네 
	// [핵심] 해당 공연의 원본 좌석 배치도(30개)를 가져옴
    // [1] 조회: 객체(performance) 내부의 필드(performanceId)까지 경로를 명시
    **/
    @Query("SELECT p FROM PerformanceSeatTemplate p WHERE p.performance.performanceId = :performanceId")
    List<PerformanceSeatTemplate> findByPerformanceId(@Param("performanceId") Long performanceId);
    
    
    
    
    // 공연 정보가 삭제될 때 설계도도 함께 삭제하기 위함
 /**JPA는 메소드 이름을 보고 쿼리를 자동으로 만드는데,
  *  현재 PerformanceSeatTemplate 엔티티 안에 performanceId (단순 Long) 필드가 있는데도 불구하고, 
  *  JPA가 자꾸 **performance (객체) 안에 있는 id**를 찾으려고 시도해서 발생하는 문제입니다. (No property 'id' found for type 'Performance
  *  이름 못찾겟다고 꼽줘서강제 지정해뒀습니다 
   **/
    @Modifying
    @Transactional
    @Query("DELETE FROM PerformanceSeatTemplate p WHERE p.performance.performanceId = :performanceId")
    void deleteByPerformanceId(@Param("performanceId") Long performanceId);
    
}

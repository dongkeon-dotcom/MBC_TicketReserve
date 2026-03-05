package com.mbc.user;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name="users")
public class Users {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_idx")
    private Long userIdx;

    @Column(name = "user_id", length = 100, nullable = false, unique = true)
    private String userId;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", length = 255)
    private String address;
    
    @Column(name = "extra_address", length = 255)
    private String extraAddress;
    
    @Column(name = "zipcode", length = 10)
    private String zipcode;

    @Column(name = "role", length = 20)
    private String role = "USER"; // 기본값 설정

    @Column(name = "join_date", updatable = false)
    @org.hibernate.annotations.CreationTimestamp // 생성 시 자동으로 현재 시간 입력
    private LocalDateTime joinDate;

    @Column(name = "login_type", length = 20)
    private String loginType = "LOCAL"; // 기본값 설정

    @Column(name = "is_easy_login")
    private boolean isEasyLogin = false; // tinyint(1) 매핑

    @Column(name = "del_yn")
    private boolean delYn = false; // 탈퇴 여부 (기본값 0)	

}

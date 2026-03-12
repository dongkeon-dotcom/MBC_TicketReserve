package com.mbc.security;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.mbc.user.Users;

public class SecurityUserDetails implements UserDetails {
	private Users user; // DB에서 조회한 실제 유저 데이터

    public SecurityUserDetails(Users user) {
        this.user = user;
    }

    // 1. 유저가 가진 권한 목록을 리턴합니다.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collect = new ArrayList<>();
        // DB에 권한 컬럼이 따로 없다면 기본적으로 "ROLE_USER"를 부여합니다.
        collect.add(new SimpleGrantedAuthority("ROLE_USER"));
        return collect;
    }

    // 2. 유저의 비밀번호를 리턴합니다.
    @Override
    public String getPassword() {
        return user.getPassword(); // 실제 DB의 비밀번호 컬럼명에 맞게 호출
    }

    // 3. 유저의 아이디를 리턴합니다.
    @Override
    public String getUsername() {
        return user.getUserId(); // 실제 DB의 아이디 컬럼명에 맞게 호출
    }

    // --- 계정 상태 체크 (일단 모두 true로 설정합니다) ---

    @Override
    public boolean isAccountNonExpired() { return true; } // 계정 만료 여부

    @Override
    public boolean isAccountNonLocked() { return true; } // 계정 잠김 여부

    @Override
    public boolean isCredentialsNonExpired() { return true; } // 비번 만료 여부

    @Override
    public boolean isEnabled() { return true; } // 계정 활성화 여부

    // --- 우리가 필요해서 만든 커스텀 메서드 ---
    
    // 컨트롤러에서 유저 객체 전체를 바로 꺼내 쓰고 싶을 때 사용합니다.
    public Users getUser() {
        return user;
    }
    
    public void setUser(Users user) {
        this.user = user;
    }
 // SecurityUserDetails.java 파일 안에 아래 코드를 넣으세요
    public Long getUserIdx() {
        return this.user.getUserIdx(); // 유저의 숫자 고유값(PK)을 반환
    }
}

package com.mbc.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.mbc.user.Users;

public class SecurityUserDetails implements UserDetails, OAuth2User {
	private Users user; // DB에서 조회한 실제 유저 데이터
	private Map<String, Object> attributes; // 소셜에서 받은 원본 데이터

	// 일반 유저 로그인
    public SecurityUserDetails(Users user) {
        this.user = user;
    }
    
    // 소셜 로그인
    public SecurityUserDetails(Users user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    // 유저 권한 목록
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collect = new ArrayList<>();
        
        String role = user.getRole();
        
        collect.add(new SimpleGrantedAuthority(role));
        return collect;
    }

    // 유저 비밀번호
    @Override
    public String getPassword() {
        return user.getPassword(); // 실제 DB의 비밀번호 컬럼명에 맞게 호출
    }

    // 유저의 아이디
    @Override
    public String getUsername() {
        return user.getUserId(); // 실제 DB의 아이디 컬럼명에 맞게 호출
    }
    
    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    @Override
    public String getName() { return user.getName(); }

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
    
    public String etUserRealName() {
    	return user.getName();
    }
}

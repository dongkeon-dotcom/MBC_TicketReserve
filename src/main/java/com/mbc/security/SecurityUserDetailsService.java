package com.mbc.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.mbc.user.Users;
import com.mbc.user.UsersRepository;

@Service
public class SecurityUserDetailsService implements UserDetailsService{
	
	@Autowired
    private UsersRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 아이디로 유저 찾기
        Users user = userRepository.findByUserId(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        // Security 전용 신분증에 담아서 리턴
        return new SecurityUserDetails(user);
    }
}

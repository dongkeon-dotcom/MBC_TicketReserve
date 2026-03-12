package com.mbc.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
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
    	// 1. 아이디로 유저를 찾음
        Users user = userRepository.findByUserId(username)
                .orElseThrow(() -> new UsernameNotFoundException("아이디 또는 비밀번호가 일치하지 않습니다."));

        System.out.println(" SecurityUserDetailsService 진입 ");
        // 2. 탈퇴 여부(delYn) 확인
        if (user.isDelYn()) {
        	System.out.println(" SecurityUserDetailsService 진입 isDelY");
            // 이 메시지가 FailureHandler로 전달됩니다.
            throw new DisabledException("탈퇴 처리된 계정입니다. 고객센터에 문의하세요.");
        }

        return new SecurityUserDetails(user);
    }
}

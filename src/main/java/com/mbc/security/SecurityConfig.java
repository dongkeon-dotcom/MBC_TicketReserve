package com.mbc.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
            		.ignoringRequestMatchers("/user/checkEmail.do") //이메일 중복검사는 제외
            		.disable()
            	) // 포트폴리오 단계에선 편의상 비활성화 (필요시 활성)
            .authorizeHttpRequests(auth -> auth
            		// 1. 정적 리소스 우선 허용
            	    .requestMatchers("/css/**", "/js/**", "/images/**", "/static/**", "/favicon.ico").permitAll()
            	    
            	    // 2. 누구나 접속 가능한 페이지 (상세히 나열)
            	    .requestMatchers("/", "/index.do", "/main.do", "/api/**").permitAll()
            	    .requestMatchers("/user/login.do", "/user/join.do", "/user/checkEmail.do", "/user/mail.do").permitAll()
            	    .requestMatchers("/reserve/**", "/search/**").permitAll()
            	    
            	    // 3. 관리자 권한
            	    .requestMatchers("/admin/**").permitAll()            	    
            	    // 4. 나머지 유저 전용 페이지 (마이페이지 등)
            	    .requestMatchers("/user/mypage.do", "/user/logout.do").authenticated()
            	    
            	    .anyRequest().authenticated() // 혹은 .permitAll()
            	)
            .formLogin(login -> login
                .loginPage("/user/login.do") // 커스텀 로그인 페이지 주소
                .loginProcessingUrl("/user/loginOK.do") // 시큐리티가 낚아채서 처리할 주소
                .usernameParameter("userId") // 유저 아이디 파라미터명
                .passwordParameter("password") // 비밀번호 파라미터명
                .defaultSuccessUrl("/", true) // 성공 시 이동할 페이지
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/user/logout.do")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 비밀번호 암호화 빈 등록
    }
}
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
            	.requestMatchers("/", "/index.do", "/user/login.do", "/user/join.do").permitAll() // 누구나 가능
                .requestMatchers("/admin/**").hasRole("ADMIN") // ADMIN 권한만
                .requestMatchers("/user/**").authenticated()
                .anyRequest().permitAll() // 누구나 접근 가능
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
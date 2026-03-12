package com.mbc.security;

import com.mbc.user.Users;
import com.mbc.user.UsersRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UsersRepository userRepo;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        SecurityUserDetails userDetails = (SecurityUserDetails) authentication.getPrincipal();
        Users user = userDetails.getUser();

        // DB에서 해당 유저가 실제로 존재하는지 조회
        boolean isExist = userRepo.findByUserId(user.getUserId()).isPresent();

        if (isExist) {
            // 1. 이미 가입된 회원이면 메인 페이지로 이동
            response.sendRedirect("/");
        } else {
            // 2. 신규 소셜 로그인 회원이면 회원가입 페이지로 이동 (쿼리 파라미터로 소셜 여부 전달 가능)
            response.sendRedirect("/user/join.do?social=true");
        }
    }
}
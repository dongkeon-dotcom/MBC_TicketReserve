package com.mbc.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        // 기본 에러 메시지 설정
        String errorMessage = "아이디 또는 비밀번호를 확인해주세요.";

        // 1. 소셜 로그인(OAuth2) 관련 예외 처리
        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) exception;
            String errorCode = oauth2Exception.getError().getErrorCode();
            
            System.out.println("### 소셜 로그인 에러 발생 - 에러 코드: " + errorCode);

            if (errorCode != null) {
                // 탈퇴한 소셜 계정 처리 (CustomOAuth2UserService에서 던진 CANCEL_USER)
                if (errorCode.equals("CANCEL_USER")) {
                    errorMessage = "탈퇴 처리된 계정입니다. 고객센터에 문의하세요.";
                } 
                // 이미 다른 소셜로 가입된 경우
                else if (errorCode.contains("ALREADY_EXISTS")) {
                    String type = errorCode.split("_")[2]; // GOOGLE, NAVER, KAKAO 등
                    errorMessage = "이미 " + type + " 계정으로 가입된 이메일입니다. 해당 수단으로 로그인해주세요.";
                } 
                else {
                    errorMessage = "소셜 로그인 처리 중 오류가 발생했습니다.";
                }
            }
        } 
        else if (exception instanceof InternalAuthenticationServiceException) {
            // 우리가 서비스에서 던진 DisabledException이 이 안에 숨어 있을 수 있습니다.
            if (exception.getCause() instanceof DisabledException) {
                errorMessage = exception.getCause().getMessage();
            } else {
                errorMessage = "계정 정보 확인 중 오류가 발생했습니다.";
            }
        }
        // 3. 직접 DisabledException이 온 경우
        else if (exception instanceof DisabledException) {
            errorMessage = exception.getMessage();
        }
        // 4. 아이디 없음
        else if (exception instanceof UsernameNotFoundException) {
            errorMessage = exception.getMessage();
        } 
        // 5. 비밀번호 불일치
        else if (exception instanceof BadCredentialsException) {
            errorMessage = "아이디 또는 비밀번호가 일치하지 않습니다.";
        }

        //System.out.println("### 최종 에러 메시지: " + errorMessage);

        // URL 파라미터로 전달하기 위해 UTF-8 인코딩
        String encodeMsg = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8.toString());
        
        // 로그인 페이지로 리다이렉트 (error 파라미터와 exception 메시지 동시 전달)
        getRedirectStrategy().sendRedirect(request, response, "/user/login.do?error=true&exception=" + encodeMsg);
    }
}
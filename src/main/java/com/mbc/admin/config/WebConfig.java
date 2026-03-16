package com.mbc.admin.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration

public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173") // React 주소
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowCredentials(true);
    }
}
//WebConfig 사용시 각 컨트롤러에 있는 @CrossOrigin 어노테이션은 지워주세요! 둘 다 있으면 충돌하거나 설정이 꼬일 수 있습니다.
//프로젝트 내의 아무 곳에나(보통 config 패키지) WebConfig라는 이름의 파일을 만들고 아래 내용을 넣으세요.
//그러면 프로젝트 내의 모든 컨트롤러에 CORS가 자동으로 적용됩니다.
//
package com.mbc.security;

import com.mbc.user.Users;
import com.mbc.user.UsersRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UsersRepository userRepository;
    private final HttpSession httpSession;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    	
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 구글, 네이버, 카카오 구분 (google, naver, kakao)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        // 소셜 로그인 시 키가 되는 필드값 (구글은 'sub', 네이버/카카오는 'id')
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 소셜 유저 정보를 DTO로 변환
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());
        
        Optional<Users> userEntity = userRepository.findByUserId(attributes.getEmail());
        
        if(userEntity.isPresent()) {
        	Users existUser = userEntity.get();
        	
        	if(existUser.isDelYn()) {
        		OAuth2Error oauth2Error = new OAuth2Error("CANCEL_USER");
        		throw new OAuth2AuthenticationException(oauth2Error,"탈퇴한 계정입니다.");
        	}
        	
        	// 회원가입한 방식과 로그인한 방식의 login_type이 같은 경우
        	if(!existUser.getLoginType().equals(registrationId.toUpperCase())) {
        		OAuth2Error oauth2Error = new OAuth2Error("ALREADY_EXISTS_" + existUser.getLoginType());
        		throw new OAuth2AuthenticationException(oauth2Error);
            }
        	
        	return new SecurityUserDetails(existUser, oAuth2User.getAttributes());
        
        } else {
        	System.out.println("TEMP USER 생성");
        	Users tempUser = Users.builder()
        			.userId(attributes.getEmail())
        			.name(attributes.getName())
        			.password("h3H#%3!g3Ggh$H$ES^SAAEA6aw464^")
        			.role("ROLE_GUEST")
        			.loginType(registrationId.toUpperCase())
        			.isEasyLogin(true)
        			.build();
        	
        	httpSession.setAttribute("tempUser", tempUser);
        	
        	return new SecurityUserDetails(tempUser, oAuth2User.getAttributes());
        }
        
        
    }

//    private Users saveOrUpdate(OAuthAttributes attributes) {
//    	System.out.println("SAVE OR UPDATE 진입");
//        Users user = userRepository.findByUserId(attributes.getEmail())
//                .map(entity -> {
//                    entity.setName(attributes.getName()); // 이미 있다면 이름 업데이트
//                    return entity;
//                })
//                .orElse(Users.builder() // 없다면 새로 생성 (회원가입)
//                        .userId(attributes.getEmail())
//                        .name(attributes.getName())
//                        .password("") // 소셜 로그인은 비번 필요 없음
//                        .role("USER")
//                        .phone("01000000000")
//                        .address("소셜로그인 유저")
//                        .zipcode("00000")
//                        .build());
//
//        return userRepository.save(user);
//    }
}
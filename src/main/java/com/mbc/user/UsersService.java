package com.mbc.user;

import java.util.Optional;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // Repository 자동 주입 (Lombok)
public class UsersService {

	private final UsersRepository usersRepo;
	
	@Transactional
	public void join(Users user) {
		
		// 추후 SpringSecurity로 여기서 암호화 한번 해야됨
		
		// DB 저장
		usersRepo.save(user);	
	}
	
	@Transactional
	public Optional<Users> findOne(String userId){
		return usersRepo.findByUserId(userId);
	}
	
	
}

package com.mbc.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UsersRepository extends JpaRepository<Users,Long> {

	//로그인 아이디(user_id)로회원 찾기
	Optional<Users> findByUserId(String userId);
	
	//로그인 아이디로 가입 여부 확인(중복체크)
	boolean existsByUserId(String userId);

	// 탈퇴하지 않은(isDeleted = false) 유저 중 아이디로 찾기
    Optional<Users> findByUserIdAndDelYnFalse(String userId);
    
    @Query("SELECT loginType FROM Users WHERE userId = :userId AND name = :name")
    Optional<String> findLoginTypeByUserIdAndName(String userId, String name);
}

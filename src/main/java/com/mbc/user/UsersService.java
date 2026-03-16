package com.mbc.user;

import java.security.SecureRandom;
import java.util.Optional;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mbc.admin.service.AdminPerformanceService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsersService {

	private final UsersRepository usersRepo;
	
	private final UserReservationRepository userReservationRepo;
	
	private final AdminPerformanceService adminPerformanceService;
	
	/* 이메일 테스트 시작*/
	private final JavaMailSender mailSender;
	private final MailVerificationService mailService;
	private final SecureRandom random = new SecureRandom();

    public String generateCode() {
        // 000000 ~ 999999 사이의 숫자를 생성
        int code = random.nextInt(1000000); 
        return String.format("%06d", code); // 6자리 숫자로 포맷팅 (앞자리 0 포함)
    }	

	private final PasswordEncoder passwordEncoder;
	
	@Value("${spring.mail.username}")
	private String fromEmail;
	
	public void sendAuthEmail(String email) throws MessagingException{
		System.out.println("메일 발송 로직 시작! From: " + fromEmail); // 콘솔 확인용
		
		String code = generateCode();
		
		mailService.saveAuthCode(email, code);
		
		
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true);
		
		helper.setTo(email);
		helper.setFrom(fromEmail);
		helper.setSubject("[인증번호] 티켓팅 회원가입");
		helper.setText("서비스 이용을 위한 인증번호 입니다.\n"
				+ "[인증번호]: " + code);
		
		mailSender.send(message);		
		System.out.println("메일 발송 완료");
	}	
	
	

	
	/* 이메일 테스트 끝*/
	
	@Transactional
	public void join(Users user) {
		
		String encPassword = passwordEncoder.encode(user.getPassword());
		user.setPassword(encPassword);
		// DB 저장
		//System.out.println("***************************************** USER 회원가입 **********************");
		//System.out.println("USER: " + user);
		
		usersRepo.save(user);	
	}
	
	public boolean checkIdExists(String userId) {
		return usersRepo.existsByUserId(userId);
	}
	
	@Transactional
	public Optional<Users> findOne(String userId){
		return usersRepo.findByUserId(userId);
	}
	
	public Users getUserById(String userId) {
		
		return usersRepo.findByUserId(userId)
				.orElseThrow(() -> new RuntimeException("해당 아이디의 사용자를 찾을 수 없습니다."));
	}
	
	@Transactional
	public Users updateMember(Users userForm) {
		// 1. DB에서 기존 원본 데이터를 가져옵니다. (영속성 컨텍스트 관리 시작)
	    Users user = usersRepo.findByUserId(userForm.getUserId())
	            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
	    
	    user.setName(userForm.getName());
	    user.setPhone(userForm.getPhone());
	    user.setZipcode(userForm.getZipcode());
	    user.setAddress(userForm.getAddress());
	    user.setExtraAddress(userForm.getExtraAddress());

	    // 3. @Transactional 덕분에 메서드가 끝날 때 자동으로 DB에 UPDATE 쿼리가 나갑니다.
	    return user;
	}
	// 비밀번호 일치 여부 확인
	public boolean checkPassword(String userId, String currentPw) {
	    Users user = usersRepo.findByUserId(userId)
	            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
	    
	    return passwordEncoder.matches(currentPw, user.getPassword());
	}

	// 비밀번호 실제 업데이트
	@Transactional
	public void updatePassword(String userId, String newPw) {
	    Users user = usersRepo.findByUserId(userId)
	            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
	    
	    user.setPassword(passwordEncoder.encode(newPw)); // 더티 체킹으로 자동 업데이트
	}
	
	// 유저 예매정보 가져오기
	public Page<UserReservationDTO> getMyReservations(Long userIdx, String status, Pageable pageable){
		return userReservationRepo.findReservationListByUserIdAndStatus(userIdx, status, pageable);
	}
	
	public UserReservationDTO getReservationDetail(String reserveNum) {
		return userReservationRepo.findByReserveNum(reserveNum)
				.orElseThrow(()->new RuntimeException("해당 예매 내역을 찾을 수 없습니다."));
	}
	
	// 예매 취소 처리
	@Transactional
	public boolean processCancellation(long reserveIdx) {
//	    int updatedRows = userReservationRepo.cancelReservation(reserveNum);
//	    if(updatedRows > 0) {
//	    	adminPerformanceService.cancelReservation(reserveIdx);
//	    	return true;
//	    }
//	    return false;
		try {
			adminPerformanceService.cancelReservation(reserveIdx);
			return true;
		} catch(Exception e) {
			return false;
		}
	}
	
	@Transactional
	public void cancelUser(String userId) {
		Users user = usersRepo.findByUserId(userId)
				.orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
		
		user.setDelYn(true);
		
		usersRepo.save(user);
	}
	
	
}

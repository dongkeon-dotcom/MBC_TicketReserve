package com.mbc.k;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling; // 1. 임포트 추가

@EnableScheduling // 2. 스케줄링 활성화 어노테이션 추가
@ComponentScan(basePackages= {"com.mbc"})
@EntityScan(basePackages= {"com.mbc"})
@EnableJpaRepositories(basePackages= {"com.mbc"})
@SpringBootApplication
public class MbcTicketReserveApplication {

	public static void main(String[] args) {
		SpringApplication.run(MbcTicketReserveApplication.class, args);
	}

}
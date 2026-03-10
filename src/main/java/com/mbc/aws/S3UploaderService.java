package com.mbc.aws;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3UploaderService {
	
	private final S3Client s3Client;
	
	@Value("${spring.cloud.aws.s3.bucket}")
	private String bucketName;
	// URL 생성을 위해 리전 정보도 주입받습니다.
    @Value("${spring.cloud.aws.s3.region}")
    private String region;
	
	public String uploadFile(MultipartFile file, String folderName) {
		if(file == null | file.isEmpty()) {
			return null;
		}
		try {
			String fileName = "TICKET/" + folderName + "/" + UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
			
			PutObjectRequest putObjectRequest = PutObjectRequest .builder()
					.bucket(bucketName)
					.key(fileName)
					.contentType(file.getContentType())
					.build();
			
			s3Client.putObject(putObjectRequest,
					RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
			
			return fileName;
		}catch(IOException e) {
			throw new RuntimeException("S3 파일 업로드 실패", e);
		}
		
	}
		
		/**
	     * 추가된 기능: DB에 저장된 파일 경로(Key)를 주면 전체 URL을 반환합니다.
	     * 예: https://s3-202601052.s3.ap-northeast-2.amazonaws.com/TICKET/...
	     */
	    public String getFullUrl(String fileName) {
	        if (fileName == null || fileName.isEmpty()) {
	            return null;
	        }
	        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + fileName;
	    }


}

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

}

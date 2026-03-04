package com.mbc.admin;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class FileUtil {
    
    // 1. 파일을 저장할 서버 내 경로 (맥북 기준)
    // 실제 존재하는 폴더여야 하며, 마지막에 /를 꼭 붙여주세요.
    private static final String UPLOAD_PATH = "/Users/hololo/downloads/ticketReserved/";

    public static String saveFile(MultipartFile file) {
        // 파일이 없으면 빈 문자열이나 null 반환
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 2. 파일명 중복 방지를 위해 UUID 사용
        // 예: "dog.jpg" -> "a1b2c3d4-dog.jpg"
        String originalName = file.getOriginalFilename();
        String saveName = UUID.randomUUID().toString() + "_" + originalName;

        // 3. 저장할 파일 객체 생성
        File saveFile = new File(UPLOAD_PATH, saveName);

        try {
            // 4. 해당 경로에 폴더가 없다면 자동으로 생성
            if (!saveFile.getParentFile().exists()) {
                saveFile.getParentFile().mkdirs();
            }
            
            // 5. 실제 하드디스크에 파일 저장
            file.transferTo(saveFile);
            
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // 6. DB에 저장할 '변경된 파일명'을 리턴
        return saveName;
    }
}
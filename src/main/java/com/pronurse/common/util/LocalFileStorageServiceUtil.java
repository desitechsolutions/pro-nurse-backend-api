package com.pronurse.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class LocalFileStorageServiceUtil {

    // Defaults to creating an "uploads" folder in the directory where the app is executed
    @Value("${spring.file.upload.dir:uploads}")
    private String uploadDir;

    public String storeFile(MultipartFile file, String folder, Long referenceId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Storage worker rejected request: file payload is empty.");
        }

        String fileName = referenceId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        return storeFileLocally(file, folder, fileName);
    }

    private String storeFileLocally(MultipartFile file, String folder, String fileName) throws IOException {
        try {
            // This now points to an external path on the server, keeping your JAR clean
            Path targetDir = Paths.get(uploadDir, folder).toAbsolutePath().normalize();

            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
                log.info("Storage directory initialized at: {}", targetDir);
            }

            Path filePath = targetDir.resolve(fileName);
            Files.write(filePath, file.getBytes());
            log.info("Asset written successfully to disk: {}", filePath);

            return fileName;

        } catch (IOException e) {
            log.error("Write failure encountered: folder={}, fileName={}", folder, fileName, e);
            throw new IOException("Failed to save file to storage: " + e.getMessage(), e);
        }
    }
}
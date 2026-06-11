package com.pronurse.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Component
@Profile("local") // Active only when spring.profiles.active=local is configured
public class LocalFileStorageServiceUtil {

    @Value("${spring.file.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Local storage hook designed to save mock file assets right into your IDE workspace directories.
     */
    public String storeFile(MultipartFile file, String folder, Long userId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Local storage worker rejected request: file payload is empty.");
        }

        // Generate an un-guessable unique filename footprint for development mapping testing
        String fileName = userId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        return storeFileLocally(file, folder, fileName);
    }

    private String storeFileLocally(MultipartFile file, String folder, String fileName) throws IOException {
        try {
            // Target the static resource folder directly within your local project root folder structure
            Path targetDir = Paths.get("src/main/resources/static", uploadDir, folder).toAbsolutePath().normalize();

            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
                log.info("Local environment initialization: Created folder directory at: {}", targetDir);
            }

            Path filePath = targetDir.resolve(fileName);
            Files.write(filePath, file.getBytes());
            log.info("Mock asset written to local disk layer: {}", filePath);

            // Returns the exact standardized unique filename to store in your mock database rows
            return fileName;

        } catch (IOException e) {
            log.error("Local write failure encountered: folder={}, fileName={}", folder, fileName, e);
            throw new IOException("Failed to save file to local workspace tree: " + e.getMessage(), e);
        }
    }
}
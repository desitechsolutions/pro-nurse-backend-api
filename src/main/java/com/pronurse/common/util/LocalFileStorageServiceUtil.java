package com.pronurse.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

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

    /**
     * Validates that the uploaded file's MIME type is within the allowed set.
     * Throws {@link IllegalArgumentException} if the file type is not permitted.
     *
     * @param file         The multipart file to validate
     * @param allowedTypes Set of allowed MIME type strings (e.g. "application/pdf", "image/jpeg")
     */
    public void validateFileType(MultipartFile file, Set<String> allowedTypes) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be null or empty.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Unsupported file type: '" + contentType + "'. " +
                    "Allowed types: " + allowedTypes);
        }
    }

    /**
     * Deletes a file from the local storage.
     * Logs a warning if the file does not exist (non-fatal).
     *
     * @param folder   Sub-folder within the upload directory (e.g. "documents/nurses/7")
     * @param fileName File name as returned by {@link #storeFile}
     */
    public void deleteFile(String folder, String fileName) {
        try {
            Path filePath = Paths.get(uploadDir, folder, fileName).toAbsolutePath().normalize();
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("File deleted from storage: {}", filePath);
            } else {
                log.warn("Delete attempted but file not found on disk: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: folder={}, fileName={}", folder, fileName, e);
            // Non-fatal: log and continue; orphan cleanup can be handled by scheduled job
        }
    }
}
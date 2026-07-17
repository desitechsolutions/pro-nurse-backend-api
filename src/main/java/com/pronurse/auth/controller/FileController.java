package com.pronurse.auth.controller;

import com.pronurse.common.payload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
@Tag(name = "18. File Management", description = "Secure file download operations for authorized users")
@SecurityRequirement(name = "Bearer Authentication")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Value("${spring.file.upload.dir:static/uploads/}")
    private String uploadDir;

    /**
     * Securely stream file bytes behind authorization guards.
     * Prevents unauthenticated users from snooping medical logs or licensing documentation.
     */
    @Operation(
            summary = "Download file securely",
            description = "Securely stream file bytes behind authorization guards. Prevents unauthenticated users from snooping medical logs or licensing documentation.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File successfully streamed as binary resource", content = @Content(mediaType = "application/octet-stream")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid file path segments / traversal attempt", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access - invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - requires authenticated role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Requested file not found or unreadable", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "An internal server error occurred", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/download/{category}/{filename:.+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'PATIENT')")
    public ResponseEntity<?> downloadFile(
            @Parameter(description = "Category/folder of the file", example = "license") @PathVariable String category,
            @Parameter(description = "Name of the file to download", example = "license.pdf") @PathVariable String filename) {

        try {
            // Path Traversal Mitigation: Prevent attackers from passing '../' in paths
            if (category.contains("..") || filename.contains("..")) {
                logger.warn("Potential Path Traversal attack intercepted!");
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Malicious path segments detected.", null));
            }

            Path filePath = Paths.get(uploadDir).resolve(category).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                logger.warn("Requested file not found or unreadable: {}/{}", category, filename);
                return ResponseEntity.status(404)
                        .body(new ApiResponse<>(false, "The requested asset could not be located.", null));
            }

            // Probe the content type dynamically (e.g. image/png, application/pdf)
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            logger.info("Securely streaming file: {}/{}", category, filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            logger.error("Failed to stream requested secure file resource: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "An internal server error occurred while retrieving file.", null));
        }
    }
}
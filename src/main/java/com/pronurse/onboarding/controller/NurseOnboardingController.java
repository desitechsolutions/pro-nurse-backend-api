package com.pronurse.onboarding.controller;

import com.pronurse.common.exception.ApplicationException;
import com.pronurse.common.payload.ApiResponse;
import com.pronurse.nurse.entity.NurseProfile;
import com.pronurse.nurse.repository.NurseProfileRepository;
import com.pronurse.onboarding.dto.NurseDocumentResponse;
import com.pronurse.onboarding.dto.OnboardingStatusResponse;
import com.pronurse.onboarding.enums.DocumentType;
import com.pronurse.onboarding.service.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/nurse/onboarding")
@Tag(
        name = "14. Nurse Onboarding",
        description = "Nurse onboarding document upload and status APIs. " +
                      "Allows nurses to upload required documents, check review status, " +
                      "replace rejected documents, submit for admin review, and view uploaded documents."
)
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('NURSE')")
@RequiredArgsConstructor
@Slf4j
public class NurseOnboardingController {

    private final OnboardingService onboardingService;
    private final NurseProfileRepository nurseProfileRepository;

    @Value("${spring.file.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Upload a new onboarding document.
     * Each document type (e.g., NURSING_CERTIFICATE, AADHAAR_CARD) is uploaded separately.
     */
    @Operation(
            summary = "Upload onboarding document",
            description = "Upload a single onboarding document. Allowed file types: PDF, JPG, PNG (max 20MB). " +
                          "Each document type must be uploaded separately. " +
                          "Call /submit once all required documents are uploaded."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Document uploaded successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Invalid file type, size exceeded, or invalid document type"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Unauthorized - invalid token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden - NURSE role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "A non-rejected document of this type already exists")
    })
    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<NurseDocumentResponse>> uploadDocument(
            @Parameter(description = "Type of document being uploaded",
                       example = "NURSING_CERTIFICATE",
                       schema = @Schema(implementation = DocumentType.class))
            @RequestParam("documentType") DocumentType documentType,
            @Parameter(description = "Document file (PDF, JPG, PNG — max 20MB)")
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        NurseDocumentResponse response = onboardingService.uploadDocument(mobile, documentType, file);

        return ResponseEntity.ok(new ApiResponse<>(
                true, "Document uploaded successfully. Upload remaining documents and then call /submit.", response));
    }

    /**
     * Replace a previously rejected document with a new file.
     */
    @Operation(
            summary = "Replace rejected document",
            description = "Replace a specific REJECTED document with a new file. " +
                          "Creates a new document record (version + 1). " +
                          "The original rejected record is preserved in review history. " +
                          "After replacing all rejected documents, call /submit again."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Document replaced successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Document is not in REJECTED status or invalid file"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Document does not belong to this nurse"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Document not found")
    })
    @PostMapping(value = "/documents/{docId}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<NurseDocumentResponse>> replaceDocument(
            @Parameter(description = "ID of the REJECTED document to replace", example = "42")
            @PathVariable Long docId,
            @Parameter(description = "New file to replace the rejected one (PDF, JPG, PNG — max 20MB)")
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        NurseDocumentResponse response = onboardingService.replaceDocument(mobile, docId, file);

        return ResponseEntity.ok(new ApiResponse<>(
                true, "Document replaced successfully. Call /submit to re-submit for admin review.", response));
    }

    /**
     * Submit all uploaded documents for admin review.
     */
    @Operation(
            summary = "Submit documents for admin review",
            description = "Submit all uploaded documents for admin review. " +
                          "Transitions onboarding status to UNDER_REVIEW. " +
                          "At least one document must be uploaded before calling this endpoint. " +
                          "Only applicable when onboarding status is DRAFT, CHANGES_REQUESTED, or DOCUMENTS_RESUBMITTED."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Documents submitted for review"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "No documents uploaded, or invalid onboarding state for submission")
    })
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<Void>> submitForReview(Authentication authentication) {
        String mobile = (String) authentication.getPrincipal();
        onboardingService.submitForReview(mobile);

        return ResponseEntity.ok(new ApiResponse<>(
                true, "Documents submitted for admin review. You will be notified once reviewed.", null));
    }

    /**
     * Get the nurse's full onboarding status and document list.
     */
    @Operation(
            summary = "Get onboarding status",
            description = "Retrieve the nurse's complete onboarding status including all uploaded documents, " +
                          "admin review comments, approval status, and progress counters. " +
                          "Use this endpoint to build the mobile profile document upload screen."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Onboarding status retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Nurse profile not found")
    })
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<OnboardingStatusResponse>> getOnboardingStatus(
            Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        OnboardingStatusResponse response = onboardingService.getOnboardingStatus(mobile);

        return ResponseEntity.ok(new ApiResponse<>(
                true, "Onboarding status retrieved successfully.", response));
    }

    /**
     * View / download a specific uploaded document.
     * The nurse can only access documents that belong to their own profile.
     */
    @Operation(
            summary = "View uploaded document",
            description = "Stream and view a previously uploaded onboarding document. " +
                          "Only the nurse who uploaded the document can access it. " +
                          "PDF and image files will open inline in the mobile viewer. " +
                          "The document ID (docId) is found in the documents[] array from the /status response."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Document file streamed successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "This document does not belong to the authenticated nurse"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Document not found or file missing on disk")
    })
    @GetMapping("/documents/{docId}/view")
    public ResponseEntity<?> viewDocument(
            @Parameter(description = "Document database ID from the /status response", example = "42")
            @PathVariable Long docId,
            Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();

        try {
            // Load document metadata from DB
            NurseDocumentResponse doc = onboardingService.getDocumentById(docId);

            // Ownership check — nurse can only view their own documents
            NurseProfile profile = nurseProfileRepository.findByUserMobile(mobile)
                    .orElseThrow(() -> new ApplicationException("Nurse profile not found."));

            if (!doc.getNurseProfileId().equals(profile.getId())) {
                throw new ApplicationException("Access denied: This document does not belong to your profile.");
            }

            // Build path from DB values — no user-controlled input in path
            String folder = "documents/nurses/" + doc.getNurseProfileId();
            Path filePath = Paths.get(uploadDir, folder, doc.getFileName())
                    .toAbsolutePath()
                    .normalize();

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.warn("Document file not found on disk for docId={}: {}", docId, filePath);
                return ResponseEntity.notFound().build();
            }

            // Detect content type (PDF, image, etc.) for correct browser/app rendering
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            log.info("Nurse {} viewing document ID {} ({})", mobile, docId, doc.getDocumentType());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    // inline = open in viewer; attachment = force download
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + doc.getOriginalName() + "\"")
                    .body(resource);

        } catch (ApplicationException e) {
            log.warn("Document view denied for nurse {}: {}", mobile, e.getMessage());
            return ResponseEntity.status(403)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Failed to stream document ID {} for nurse {}: {}", docId, mobile, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "File retrieval failed: " + e.getMessage(), null));
        }
    }
}

package com.pronurse.onboarding.controller;

import com.pronurse.common.payload.ApiResponse;
import com.pronurse.onboarding.dto.*;
import com.pronurse.onboarding.service.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/admin/onboarding")
@Tag(
        name = "15. Admin Onboarding Management",
        description = "Admin endpoints for reviewing nurse onboarding documents. " +
                      "Supports per-document approval/rejection with comments, " +
                      "full audit history, secure file download, and final onboarding completion."
)
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminOnboardingController {

    private final OnboardingService onboardingService;

    @Value("${spring.file.upload.dir:uploads}")
    private String uploadDir;

    /**
     * List all nurses filtered by onboarding status (paginated).
     */
    @Operation(
            summary = "List nurses by onboarding status",
            description = "Retrieve a paginated list of nurses optionally filtered by onboarding status. " +
                          "Allowed status values: DRAFT, UNDER_REVIEW, CHANGES_REQUESTED, " +
                          "DOCUMENTS_RESUBMITTED, APPROVED, ONBOARDING_COMPLETED. " +
                          "Leave status blank to retrieve all nurses."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Nurse onboarding listing retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Invalid onboarding status filter value"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden - ADMIN role required")
    })
    @GetMapping("/nurses")
    public ResponseEntity<ApiResponse<Page<AdminOnboardingNurseSummary>>> listNurses(
            @Parameter(description = "Filter by onboarding status (optional)",
                       example = "UNDER_REVIEW")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<AdminOnboardingNurseSummary> result = onboardingService.listNursesByOnboardingStatus(status, pageable);

        return ResponseEntity.ok(new ApiResponse<>(
                true, "Nurse onboarding listing retrieved successfully.", result));
    }

    /**
     * Get full onboarding status and all documents for a specific nurse (admin view).
     */
    @Operation(
            summary = "Get nurse onboarding documents",
            description = "Retrieve the complete onboarding status and all submitted documents for a specific nurse, " +
                          "including document statuses, admin review comments, and file download URLs."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Nurse onboarding documents retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Nurse profile not found")
    })
    @GetMapping("/nurses/{nurseProfileId}/documents")
    public ResponseEntity<ApiResponse<OnboardingStatusResponse>> getNurseOnboardingDocuments(
            @Parameter(description = "Nurse profile database ID", example = "7")
            @PathVariable Long nurseProfileId) {

        OnboardingStatusResponse response = onboardingService.getOnboardingStatusByNurseProfileId(nurseProfileId);

        return ResponseEntity.ok(new ApiResponse<>(
                true, "Nurse onboarding documents retrieved successfully.", response));
    }

    /**
     * Securely stream/download a specific document file.
     *
     * Security approach: the file path is NEVER taken from the request URL.
     * Instead, the stored fileName is loaded from the database using the document ID,
     * which completely eliminates path traversal vulnerabilities.
     */
    @Operation(
            summary = "Download nurse document",
            description = "Securely stream and download a nurse's onboarding document file. " +
                          "File path is resolved entirely from the database — no path traversal risk. " +
                          "Supports inline preview (PDF, images) via Content-Disposition: inline."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "File streamed successfully as binary resource"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Document record not found in database, or file missing on disk"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500",
                    description = "Internal error while streaming file")
    })
    @GetMapping("/documents/{docId}/download")
    public ResponseEntity<?> downloadDocument(
            @Parameter(description = "Document database ID", example = "42")
            @PathVariable Long docId) {

        try {
            // Load document metadata from DB — file path NEVER comes from user input
            NurseDocumentResponse doc = onboardingService.getDocumentById(docId);

            // Build the storage folder path: documents/nurses/{nurseProfileId}/
            String folder = "documents/nurses/" + doc.getNurseProfileId();

            // Use the stored fileName from DB (includes timestamp, prevents collisions)
            Path filePath = Paths.get(uploadDir, folder, doc.getFileName())
                    .toAbsolutePath()
                    .normalize();

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.warn("File not found on disk for document ID {}: {}", docId, filePath);
                return ResponseEntity.notFound().build();
            }

            // Probe actual content type from the file bytes
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            log.info("Admin streaming document ID {} ({}): {}", docId, doc.getDocumentType(), filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + doc.getOriginalName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Failed to stream document ID {}: {}", docId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "File retrieval failed: " + e.getMessage(), null));
        }
    }

    /**
     * Review (approve, reject, or request additional) a single onboarding document.
     */
    @Operation(
            summary = "Review onboarding document",
            description = "Approve, reject, or request additional information for a specific nurse document. " +
                          "Actions: APPROVED, REJECTED (comment required), ADDITIONAL_REQUESTED (comment required). " +
                          "After the last pending document is approved, onboarding status auto-transitions to APPROVED. " +
                          "After any rejection, onboarding status transitions to CHANGES_REQUESTED. " +
                          "A push notification is sent to the nurse after each review action."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Document review action applied successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Missing comment for rejection, or invalid onboarding state for review"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Document not found")
    })
    @PostMapping("/documents/{docId}/review")
    public ResponseEntity<ApiResponse<Void>> reviewDocument(
            @Parameter(description = "Document database ID to review", example = "42")
            @PathVariable Long docId,
            @Valid @RequestBody DocumentReviewRequest request,
            Authentication authentication) {

        String adminMobile = (String) authentication.getPrincipal();
        onboardingService.reviewDocument(adminMobile, docId, request);

        return ResponseEntity.ok(new ApiResponse<>(
                true, "Document review action applied. Nurse has been notified via push notification.", null));
    }

    /**
     * Mark a nurse's onboarding as fully completed.
     */
    @Operation(
            summary = "Complete nurse onboarding",
            description = "Mark a nurse's onboarding as ONBOARDING_COMPLETED. " +
                          "All submitted documents must be in APPROVED status. " +
                          "This sets isVerified = true, verificationStatus = 'Approved', isOnDuty = true. " +
                          "The nurse becomes eligible for patient bookings immediately. " +
                          "A congratulatory push notification is sent to the nurse."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Nurse onboarding completed and verified successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Not all documents are approved, or onboarding is not in APPROVED state"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Nurse profile not found")
    })
    @PostMapping("/nurses/{nurseProfileId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeOnboarding(
            @Parameter(description = "Nurse profile database ID", example = "7")
            @PathVariable Long nurseProfileId,
            @Parameter(description = "Optional completion note from admin")
            @RequestParam(required = false) String comment,
            Authentication authentication) {

        String adminMobile = (String) authentication.getPrincipal();
        onboardingService.completeOnboarding(adminMobile, nurseProfileId, comment);

        return ResponseEntity.ok(new ApiResponse<>(
                true, "Nurse onboarding completed. The nurse is now verified and eligible for dispatch.", null));
    }

    /**
     * Retrieve the full review history for a specific document.
     */
    @Operation(
            summary = "Get document review history",
            description = "Retrieve the complete chronological audit trail for a specific onboarding document. " +
                          "Shows all SUBMITTED, APPROVED, REJECTED, and RESUBMITTED events " +
                          "with timestamps and the mobile numbers of the actors."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Document review history retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Document not found")
    })
    @GetMapping("/documents/{docId}/history")
    public ResponseEntity<ApiResponse<List<DocumentReviewHistoryResponse>>> getDocumentHistory(
            @Parameter(description = "Document database ID", example = "42")
            @PathVariable Long docId) {

        List<DocumentReviewHistoryResponse> history = onboardingService.getDocumentHistory(docId);

        return ResponseEntity.ok(new ApiResponse<>(
                true, "Document review history retrieved successfully.", history));
    }
}

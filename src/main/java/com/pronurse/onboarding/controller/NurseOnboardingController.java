package com.pronurse.onboarding.controller;

import com.pronurse.common.payload.ApiResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/nurse/onboarding")
@Tag(
        name = "14. Nurse Onboarding",
        description = "Nurse onboarding document upload and status APIs. " +
                      "Allows nurses to upload required documents, check review status, " +
                      "replace rejected documents, and submit for admin review."
)
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('NURSE')")
@RequiredArgsConstructor
public class NurseOnboardingController {

    private final OnboardingService onboardingService;

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
}

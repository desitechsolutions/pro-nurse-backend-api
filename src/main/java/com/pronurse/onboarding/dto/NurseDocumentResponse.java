package com.pronurse.onboarding.dto;

import com.pronurse.onboarding.enums.DocumentStatus;
import com.pronurse.onboarding.enums.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO representing a single nurse onboarding document.
 * Returned in both nurse-facing and admin-facing API responses.
 */
@Data
@Builder
@Schema(description = "Details of a single nurse onboarding document")
public class NurseDocumentResponse {

    @Schema(description = "Database ID of the document", example = "42")
    private Long id;

    @Schema(description = "Nurse profile ID this document belongs to", example = "7")
    private Long nurseProfileId;

    @Schema(description = "Category of the document", example = "NURSING_CERTIFICATE")
    private DocumentType documentType;

    @Schema(description = "Human-readable document type name", example = "Nursing Certificate")
    private String documentTypeName;

    @Schema(description = "Current review status of this document", example = "PENDING")
    private DocumentStatus documentStatus;

    @Schema(description = "Original filename provided at upload", example = "nursing_cert.pdf")
    private String originalName;

    @Schema(description = "Stored filename on disk (includes timestamp prefix)", example = "7_1722096000000_nursing_cert.pdf")
    private String fileName;

    @Schema(description = "File size in bytes", example = "204800")
    private Long fileSizeBytes;

    @Schema(description = "MIME type of the file", example = "application/pdf")
    private String mimeType;

    @Schema(description = "Version number — increments each time a rejected doc is replaced", example = "1")
    private Integer version;

    @Schema(description = "Admin review comment (rejection reason or approval note)")
    private String reviewComment;

    @Schema(description = "Mobile of the admin who last reviewed this document")
    private String reviewedBy;

    @Schema(description = "Timestamp of the last review action")
    private LocalDateTime reviewedAt;

    @Schema(description = "Timestamp when the document was uploaded")
    private LocalDateTime uploadedAt;

    /**
     * Pre-built download URL for this document.
     * Admin: /api/admin/onboarding/documents/{id}/download
     */
    @Schema(description = "Direct download URL for this document (admin-gated)")
    private String downloadUrl;
}

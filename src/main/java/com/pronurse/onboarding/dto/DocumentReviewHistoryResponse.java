package com.pronurse.onboarding.dto;

import com.pronurse.onboarding.enums.DocumentType;
import com.pronurse.onboarding.enums.ReviewAction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for a single document review history event.
 * Provides a complete chronological audit trail of all actions taken on a document.
 */
@Data
@Builder
@Schema(description = "A single audit event in the document review history")
public class DocumentReviewHistoryResponse {

    @Schema(description = "History record ID", example = "101")
    private Long id;

    @Schema(description = "ID of the document this event belongs to", example = "42")
    private Long documentId;

    @Schema(description = "Document type for context", example = "PAN_CARD")
    private DocumentType documentType;

    @Schema(description = "Human-readable document type name", example = "PAN Card")
    private String documentTypeName;

    @Schema(description = "The action that occurred", example = "REJECTED")
    private ReviewAction action;

    @Schema(description = "Comment associated with this action (e.g. rejection reason)")
    private String comment;

    @Schema(description = "Mobile number of the actor (nurse or admin)", example = "9876543210")
    private String performedBy;

    @Schema(description = "Timestamp when this action occurred")
    private LocalDateTime performedAt;
}

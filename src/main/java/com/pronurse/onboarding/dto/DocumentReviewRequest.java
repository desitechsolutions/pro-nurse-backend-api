package com.pronurse.onboarding.dto;

import com.pronurse.onboarding.enums.ReviewAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request payload for admin document review actions (approve, reject, or request additional).
 *
 * <p>Validation: {@code comment} is required when {@code action} is {@code REJECTED}
 * or {@code ADDITIONAL_REQUESTED}. This is enforced in the service layer since
 * conditional bean validation is not natively supported without custom annotations.</p>
 */
@Data
@Schema(description = "Request payload for reviewing an individual onboarding document")
public class DocumentReviewRequest {

    @NotNull(message = "Review action is required (APPROVED, REJECTED, or ADDITIONAL_REQUESTED)")
    @Schema(
            description = "The review action to take on this document",
            example = "APPROVED",
            allowableValues = {"APPROVED", "REJECTED", "ADDITIONAL_REQUESTED"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private ReviewAction action;

    @Schema(
            description = "Admin comment — required when action is REJECTED or ADDITIONAL_REQUESTED",
            example = "The document image is too blurry to read the registration number."
    )
    private String comment;
}

package com.pronurse.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request payload for moderating patient reviews")
public class ReviewModerationRequest {
    @NotNull(message = "Review target tracker identifier required")
    @Schema(description = "ID of the review to moderate", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long reviewId;

    @NotBlank(message = "Target operational action parameter required (APPROVE/DELETE)")
    @Schema(description = "Moderation action to perform (APPROVE or DELETE)", example = "APPROVE", allowableValues = {"APPROVE", "DELETE"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String action; // 'APPROVE' or 'DELETE'
}
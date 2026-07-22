package com.pronurse.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Payload to submit a feedback review and rating for a nurse")
public class AddReviewRequest {
    @NotBlank(message = "Booking identification string number reference cannot be blank")
    @Schema(description = "Unique booking number", example = "BK-12345", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bookingId;

    @NotNull(message = "Rating parameter score tracking variable required")
    @Min(value = 1, message = "Rating calculation score cannot fall below 1 star")
    @Max(value = 5, message = "Rating calculation score cannot exceed 5 stars")
    @Schema(description = "Rating score out of 5", example = "5", minimum = "1", maximum = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer rating;

    @Schema(description = "Optional written feedback review comments", example = "Excellent service, very professional and punctual.")
    private String review;
}
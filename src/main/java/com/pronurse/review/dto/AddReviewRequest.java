package com.pronurse.review.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddReviewRequest {
    @NotBlank(message = "Booking identification string number reference cannot be blank")
    private String bookingId;

    @NotNull(message = "Rating parameter score tracking variable required")
    @Min(value = 1, message = "Rating calculation score cannot fall below 1 star")
    @Max(value = 5, message = "Rating calculation score cannot exceed 5 stars")
    private Integer rating;

    private String review;
}
package com.pronurse.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request to mark a booking as completed")
public class CompleteBookingRequest {
    @NotBlank(message = "Booking identification tracker token number cannot be empty")
    @Schema(description = "The unique booking ID", example = "BK-2024-001")
    private String bookingId;

    @NotBlank(message = "Final operational treatment or tracking remarks required")
    @Schema(description = "Final operational treatment or tracking remarks", example = "Injection administered and blood pressure checked.")
    private String remarks;
}
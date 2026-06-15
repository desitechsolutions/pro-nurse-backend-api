package com.pronurse.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request to cancel a booking")
public class CancelBookingRequest {

    @NotBlank(message = "Booking ID is required")
    @Schema(description = "Booking number to cancel", example = "BOOK-A1B2C3D4")
    private String bookingId;

    @NotBlank(message = "Cancellation reason is required")
    @Schema(description = "Reason for cancellation", example = "Changed plans")
    private String reason;

    @Schema(description = "Who is cancelling", example = "PATIENT", allowableValues = {"PATIENT", "NURSE", "ADMIN"})
    private String cancelledBy;
}

// Made with Bob

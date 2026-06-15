package com.pronurse.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request to reschedule a booking")
public class RescheduleBookingRequest {

    @NotBlank(message = "Booking ID is required")
    @Schema(description = "Booking number to reschedule", example = "BOOK-A1B2C3D4")
    private String bookingId;

    @NotBlank(message = "New booking date is required")
    @Schema(description = "New booking date in YYYY-MM-DD format", example = "2024-12-26")
    private String newDate;

    @NotBlank(message = "New booking time is required")
    @Schema(description = "New booking time", example = "11:00 AM")
    private String newTime;

    @Schema(description = "Reason for rescheduling", example = "Emergency came up")
    private String reason;
}



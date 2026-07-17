package com.pronurse.booking.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "Request body to create a new booking")
public class CreateBookingRequest {
    @NotEmpty(message = "At least one service subcategory must be selected")
    @Schema(description = "List of service subcategory IDs selected for the booking", example = "[1, 2]")
    private List<Long> selectedServiceIds;

    @NotBlank(message = "Date choice cannot be empty")
    @Schema(description = "Booking date in YYYY-MM-DD format", example = "2024-01-20")
    private String bookingDate; // Expected Format: YYYY-MM-DD

    @NotBlank(message = "Time slot required")
    @Schema(description = "Booking time slot in HH:mm format", example = "14:30")
    private String bookingTime;

    @NotBlank(message = "Address target cannot be blank")
    @Schema(description = "Detailed service delivery address", example = "123 Main St, Apartment 4B")
    private String address;

    @Schema(description = "Patient coordinate latitude", example = "28.6139")
    private String latitude;

    @Schema(description = "Patient coordinate longitude", example = "77.2090")
    private String longitude;

    @Schema(description = "Special instructions or notes", example = "Patient requires injection care")
    private String remarks;

    @Schema(description = "Patient-written additional notes", example = "Has severe back pain")
    private String notes;

    @Schema(description = "Whether an injection is required during this booking", example = "true")
    private Boolean hasInjection;

    @NotNull(message = "Total transactional evaluation price required")
    @Schema(description = "Total booking base price", example = "499.00")
    private BigDecimal price;

    @NotBlank(message = "Payment strategy choice required")
    @Schema(description = "Payment mode (ONLINE or OFFLINE)", example = "ONLINE")
    private String paymentMode;
}
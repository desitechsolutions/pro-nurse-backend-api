package com.pronurse.booking.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateBookingRequest {
    @NotEmpty(message = "At least one service subcategory must be selected")
    private List<Long> selectedServiceIds;

    @NotBlank(message = "Date choice cannot be empty")
    private String bookingDate; // Expected Format: YYYY-MM-DD

    @NotBlank(message = "Time slot required")
    private String bookingTime;

    @NotBlank(message = "Address target cannot be blank")
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
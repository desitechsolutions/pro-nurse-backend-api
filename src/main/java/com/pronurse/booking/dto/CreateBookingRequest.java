package com.pronurse.booking.dto;

import jakarta.validation.constraints.*;
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

    @NotBlank(message = "Latitude coordinates are required")
    private String latitude;

    @NotBlank(message = "Longitude coordinates are required")
    private String longitude;

    private String remarks;

    @NotNull(message = "Total transactional evaluation price required")
    private BigDecimal price;

    @NotBlank(message = "Payment strategy choice required")
    private String paymentMode;
}
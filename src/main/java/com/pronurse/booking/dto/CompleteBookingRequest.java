package com.pronurse.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompleteBookingRequest {
    @NotBlank(message = "Booking identification tracker token number cannot be empty")
    private String bookingId;

    @NotBlank(message = "Final operational treatment or tracking remarks required")
    private String remarks;
}
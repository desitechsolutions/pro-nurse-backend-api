package com.pronurse.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@Schema(description = "Response payload representing a summary of a booking for the administrative portal")
public class AdminBookingSummaryResponse {
    @Schema(description = "Unique ID of the booking", example = "101")
    private Long id;

    @Schema(description = "Formatted booking registration number", example = "B-20260717-1002")
    private String bookingNo;

    @Schema(description = "Name of the patient", example = "John Doe")
    private String patientName;

    @Schema(description = "Mobile number of the patient", example = "9876543210")
    private String patientMobile;

    @Schema(description = "Name of the nurse assigned to the booking", example = "Jane Smith")
    private String assignedNurseName;

    @Schema(description = "Mobile number of the assigned nurse", example = "9876543211")
    private String assignedNurseMobile;

    @Schema(description = "Scheduled date of the booking appointment", example = "2026-07-20")
    private LocalDate bookingDate;

    @Schema(description = "Scheduled time window of the booking appointment", example = "10:00 - 12:00")
    private String bookingTime;

    @Schema(description = "Status of the booking", example = "CONFIRMED")
    private String bookingStatus;

    @Schema(description = "Status of the payment", example = "PAID")
    private String paymentStatus;

    @Schema(description = "Total billing amount for the booking", example = "1500.00")
    private BigDecimal totalAmount;
}
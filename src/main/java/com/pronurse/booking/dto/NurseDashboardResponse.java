package com.pronurse.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response containing booking details for the Nurse Dashboard")
public class NurseDashboardResponse {
    @Schema(description = "Unique booking identifier", example = "BK-2024-001")
    private String bookingId;

    @Schema(description = "Name of the patient", example = "John Doe")
    private String patientName;

    @Schema(description = "Mobile number of the patient", example = "9876543210")
    private String patientMobile;

    @Schema(description = "Booking date in YYYY-MM-DD format", example = "2024-01-20")
    private String bookingDate;

    @Schema(description = "Booking time slot", example = "14:30")
    private String bookingTime;

    @Schema(description = "Service address", example = "123 Main St, Apartment 4B")
    private String address;

    @Schema(description = "Additional remarks/instructions", example = "Patient requires injection care")
    private String remarks;

    @Schema(description = "Total booking price paid by patient", example = "499.00")
    private BigDecimal totalPrice;

    @Schema(description = "Status of the booking", example = "CONFIRMED")
    private String bookingStatus;

    @Schema(description = "Nurse assignment status (RINGING or ACCEPTED)", example = "RINGING")
    private String assignmentStatus; // 'RINGING' or 'ACCEPTED'

    @Schema(description = "List of selected service names", example = "[\"Injection\", \"Basic Assessment\"]")
    private List<String> selectedServices;
}
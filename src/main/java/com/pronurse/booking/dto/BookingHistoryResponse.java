package com.pronurse.booking.dto;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing detailed booking history information")
public class BookingHistoryResponse {
    @Schema(description = "Unique identifier of the booking", example = "BK-2024-001")
    private String bookingId;

    @Schema(description = "Date of the booking in YYYY-MM-DD format", example = "2024-01-20")
    private String bookingDate;

    @Schema(description = "Time slot of the booking", example = "14:30")
    private String bookingTime;

    @Schema(description = "Current status of the booking", example = "CONFIRMED")
    private String bookingStatus;

    @Schema(description = "Current payment status of the booking", example = "PAID")
    private String paymentStatus;

    @Schema(description = "Service delivery address", example = "123 Main St, Apartment 4B")
    private String address;

    @Schema(description = "Additional remarks/instructions for the booking", example = "Please bring necessary equipment")
    private String remarks;
    @Schema(description = "Prescription file download URL if present", example = "/api/files/prescription/1")
    private String prescriptionUrl;

    @Schema(description = "Counterparty name (Patient name for Nurse view, Nurse name for Patient view)", example = "Jane Smith")
    private String counterpartyName;

    @Schema(description = "Counterparty mobile number", example = "9876543210")
    private String counterpartyMobile;

    @Schema(description = "Assigned Nurse latitude coordinate", example = "28.6139")
    private Double nurseLatitude;

    @Schema(description = "Assigned Nurse longitude coordinate", example = "77.2090")
    private Double nurseLongitude;

    @Schema(description = "Total booking price paid by patient", example = "499.00")
    private BigDecimal totalAmount;

    @Schema(description = "List of service names selected in the booking", example = "[\"Injection\", \"Basic Assessment\"]")
    private List<String> serviceNames;
}
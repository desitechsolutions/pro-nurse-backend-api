package com.pronurse.booking.dto;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingHistoryResponse {
    private String bookingId;
    private String bookingDate;
    private String bookingTime;
    private String bookingStatus;
    private String paymentStatus;
    private String address;
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
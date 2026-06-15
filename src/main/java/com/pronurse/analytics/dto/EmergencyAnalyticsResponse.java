package com.pronurse.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Emergency response analytics")
public class EmergencyAnalyticsResponse {

    @Schema(description = "Booking number", example = "EMERGENCY-ABC123")
    private String bookingNo;

    @Schema(description = "Emergency created time")
    private LocalDateTime emergencyCreatedAt;

    @Schema(description = "First nurse notified time")
    private LocalDateTime firstNotificationTime;

    @Schema(description = "Nurse accepted time")
    private LocalDateTime acceptedAt;

    @Schema(description = "Response time in seconds", example = "45")
    private Long responseTimeSeconds;

    @Schema(description = "Number of nurses notified", example = "5")
    private Integer nursesNotified;

    @Schema(description = "Number of nurses who rejected", example = "2")
    private Integer nursesRejected;

    @Schema(description = "Accepted nurse name", example = "Jane Smith")
    private String acceptedNurseName;

    @Schema(description = "Emergency description")
    private String emergencyDescription;

    @Schema(description = "Patient location")
    private String patientAddress;

    @Schema(description = "Final status", example = "ACCEPTED")
    private String status;
}



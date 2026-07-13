package com.pronurse.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Emergency SOS booking request")
public class EmergencySOSRequest {

    @NotNull(message = "Service ID is required")
    @Schema(description = "Medical service ID", example = "1")
    private Long serviceId;

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address must not exceed 500 characters")
    @Schema(description = "Patient address", example = "123 Main St, Apartment 4B")
    private String address;

    @NotNull(message = "Latitude is required")
    @Schema(description = "Address latitude", example = "28.6139")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    @Schema(description = "Address longitude", example = "77.2090")
    private BigDecimal longitude;

    @NotBlank(message = "Emergency description is required")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Emergency situation description", example = "Patient fell and injured leg, bleeding heavily")
    private String emergencyDescription;

    @NotBlank(message = "Emergency contact name is required")
    @Size(max = 100, message = "Contact name must not exceed 100 characters")
    @Schema(description = "Emergency contact person name", example = "John Doe")
    private String emergencyContactName;

    @NotBlank(message = "Emergency contact mobile is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid mobile number")
    @Schema(description = "Emergency contact mobile number", example = "9876543210")
    private String emergencyContactMobile;

    @Schema(description = "Additional notes", example = "Patient is diabetic")
    private String notes;

    @Schema(description = "Preferred start time (defaults to immediate)", example = "2024-01-15T14:30:00")
    private LocalDateTime preferredStartTime;

    @Schema(description = "Estimated duration in hours", example = "2")
    private Integer estimatedDurationHours;
}



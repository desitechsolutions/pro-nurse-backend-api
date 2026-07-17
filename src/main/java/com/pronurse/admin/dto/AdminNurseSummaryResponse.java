package com.pronurse.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Response payload representing a summary of a nurse profile for the admin dashboard")
public class AdminNurseSummaryResponse {
    @Schema(description = "Unique user ID of the nurse", example = "15")
    private Long id;

    @Schema(description = "Business ID identifier of the nurse profile", example = "NURSE-2026-0034")
    private String nurseId;

    @Schema(description = "Full name of the nurse", example = "Jane Smith")
    private String name;

    @Schema(description = "Mobile number of the nurse", example = "9876543211")
    private String mobile;

    @Schema(description = "Email address of the nurse", example = "jane.smith@example.com")
    private String email;

    @Schema(description = "Educational qualification of the nurse", example = "B.Sc in Nursing")
    private String qualification;

    @Schema(description = "Specialization area of the nurse", example = "General Nursing / ICU Care")
    private String specialization;

    @Schema(description = "State council medical registration number", example = "RN-98765-KA")
    private String registrationNumber;

    @Schema(description = "Flag indicating whether the nurse's documents and credentials are verified", example = "true")
    private boolean isVerified;

    @Schema(description = "Flag indicating whether the nurse is currently active on duty/online", example = "true")
    private boolean isOnDuty;

    @Schema(description = "Verification approval status", example = "Approved")
    private String verificationStatus;

    @Schema(description = "Average rating received from patients", example = "4.8")
    private double averageRating;

    @Schema(description = "Timestamp when the nurse account was created", example = "2026-07-15T10:00:00")
    private LocalDateTime createdAt;
}
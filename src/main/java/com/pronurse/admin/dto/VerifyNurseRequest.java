package com.pronurse.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request payload for verified nurse credentials configuration status")
public class VerifyNurseRequest {
    @NotNull(message = "Nurse profile numeric ID context required")
    @Schema(description = "Database identifier of the nurse profile to verify", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long nurseProfileId;

    @NotBlank(message = "Verification status target action must be defined (Approved/Rejected)")
    @Schema(description = "Target status update (Approved or Rejected)", example = "Approved", allowableValues = {"Approved", "Rejected"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String status; // 'Approved' or 'Rejected'

    @Schema(description = "Reason for rejection if status is Rejected", example = "Registration license number is invalid.")
    private String rejectReason;
}
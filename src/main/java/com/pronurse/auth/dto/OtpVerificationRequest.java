package com.pronurse.auth.dto;

import com.pronurse.auth.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Request payload for OTP verification and authentication")
public class OtpVerificationRequest {
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number format")
    @Schema(description = "10-digit Indian mobile number", example = "9876543210", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobile;

    @NotBlank(message = "OTP is required")
    @Schema(description = "6-digit OTP code received by the user", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String otp;

    @NotNull(message = "Role type is required")
    @Schema(description = "Platform target role type associated with the authentication action", example = "PATIENT", requiredMode = Schema.RequiredMode.REQUIRED)
    private Role roleType; // PATIENT, NURSE

    @Schema(description = "Optional initial name entry for new profile initialization", example = "John Doe")
    private String name; // Optional initial name entry
}
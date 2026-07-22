package com.pronurse.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Request payload to initiate OTP dispatch")
public class OtpRequest {
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number format")
    @Schema(description = "10-digit Indian mobile number", example = "9876543210", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobile;
}
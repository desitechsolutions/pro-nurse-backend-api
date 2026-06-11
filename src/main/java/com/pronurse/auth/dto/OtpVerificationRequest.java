package com.pronurse.auth.dto;

import com.pronurse.auth.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OtpVerificationRequest {
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number format")
    private String mobile;

    @NotBlank(message = "OTP is required")
    private String otp;

    @NotNull(message = "Role type is required")
    private Role roleType; // PATIENT, NURSE

    private String name; // Optional initial name entry
}
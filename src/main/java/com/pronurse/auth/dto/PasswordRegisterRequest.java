package com.pronurse.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Request body for password-based registration compatible with Flutter mobile app")
public class PasswordRegisterRequest {

    @NotBlank(message = "Login type (role) is required")
    @Schema(description = "Login type / Role (Patient or Nurse)", example = "Patient")
    private String loginType;

    @NotBlank(message = "Name is required")
    @Schema(description = "Display name", example = "Rahul Sharma")
    private String name;

    @Schema(description = "Email address", example = "rahul@example.com")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number format")
    @Schema(description = "10-digit mobile number", example = "9876543210")
    private String mobile;

    @NotBlank(message = "Password is required")
    @Schema(description = "SHA-256 hashed password string from mobile client", example = "ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f")
    private String password;

    @Schema(description = "Gender (Male/Female)", example = "Male")
    private String gender;

    @Schema(description = "Date of Birth (YYYY-MM-DD)", example = "1995-08-15")
    private String dob;

    @Schema(description = "Full Address", example = "Flat 405, Green Glen Layout, Bangalore")
    private String address;

    @Schema(description = "City name", example = "Bangalore")
    private String city;

    @Schema(description = "State name", example = "Karnataka")
    private String state;

    @Schema(description = "Pincode", example = "560103")
    private String pincode;

    @Schema(description = "Qualification (Nurses only)", example = "B.Sc Nursing")
    private String qualification;

    @Schema(description = "Experience in years (Nurses only)", example = "5 Years")
    private String experience;

    @Schema(description = "Specialization area (Nurses only)", example = "Post Surgery Care")
    private String specialization;

    @Schema(description = "Hospital name affiliated with (Nurses only)", example = "Apollo Hospital")
    private String hospitalName;

    @Schema(description = "Professional registration license number (Nurses only)", example = "REG1234567")
    private String registrationNumber;

    @Schema(description = "Latitude of home/work location", example = "28.6139")
    private String latitude;

    @Schema(description = "Longitude of home/work location", example = "77.2090")
    private String longitude;
}

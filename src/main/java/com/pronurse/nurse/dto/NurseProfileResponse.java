package com.pronurse.nurse.dto;

import com.pronurse.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@Schema(description = "Response payload containing the details of a nurse profile")
public class NurseProfileResponse {
    @Schema(description = "Internal database ID of the user", example = "1")
    private Long id;

    @Schema(description = "Unique identifier code of the nurse", example = "NURSE-12345")
    private String nurseId;

    @Schema(description = "Full name of the nurse", example = "Jane Doe")
    private String name;

    @Schema(description = "Contact mobile number", example = "+919876543210")
    private String mobile;

    @Schema(description = "Email address", example = "jane.doe@example.com")
    private String email;

    @Schema(description = "Gender of the nurse", example = "FEMALE")
    private Gender gender;

    @Schema(description = "Date of birth", example = "1990-01-01")
    private LocalDate dob;

    @Schema(description = "Educational/professional qualification", example = "B.Sc. Nursing")
    private String qualification;

    @Schema(description = "Professional experience detail", example = "5 Years")
    private String experience;

    @Schema(description = "Area of specialization", example = "Critical Care")
    private String specialization;

    @Schema(description = "Comma-separated languages spoken", example = "English, Hindi")
    private String languages;

    @Schema(description = "Full address details", example = "123 Healthcare St, Lucknow")
    private String address;

    @Schema(description = "Path/URL to profile image", example = "/api/files/profile-image/1")
    private String profileImage;

    @Schema(description = "Medical council registration certificate number", example = "REG-998877")
    private String registrationNumber;

    @Schema(description = "Average star rating", example = "4.8")
    private double averageRating;

    @Schema(description = "Current active on-duty tracking toggle flag", example = "true")
    private boolean isOnDuty;

    @Schema(description = "Background verification clearance status", example = "APPROVED")
    private String verificationStatus;

    @Schema(description = "City where the nurse operates", example = "Lucknow")
    private String city;

    @Schema(description = "Base consultation fee per visit session", example = "500.00")
    private java.math.BigDecimal consultationFee;
}
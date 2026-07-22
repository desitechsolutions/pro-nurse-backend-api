package com.pronurse.nurse.dto;

import com.pronurse.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Payload to update or create a nurse's profile details")
public class NurseProfileUpdateRequest {
    @NotBlank(message = "Name field cannot be left blank")
    @Schema(description = "Full name of the nurse", example = "Jane Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Email(message = "Please provide a valid email format layout")
    @Schema(description = "Email address", example = "jane.doe@example.com")
    private String email;

    @Schema(description = "Gender of the nurse", example = "FEMALE")
    private Gender gender;

    @Schema(description = "Date of birth", example = "1990-01-01")
    private LocalDate dob;

    @Schema(description = "Years or details of professional experience", example = "5 Years")
    private String experience;

    @Schema(description = "Highest professional qualification details", example = "B.Sc. Nursing")
    private String qualification;

    @Schema(description = "Area of specialization", example = "Critical Care")
    private String specialization;

    @Schema(description = "Languages spoken (comma-separated)", example = "English, Hindi")
    private String languages;

    @Schema(description = "Full residential or workplace address", example = "123 Healthcare St, Lucknow")
    private String address;

    @Schema(description = "Base latitude coordinate of office or default location", example = "28.4595")
    private String latitude;

    @Schema(description = "Base longitude coordinate of office or default location", example = "77.0266")
    private String longitude;

    @Schema(description = "State or central medical council registration number", example = "REG-998877")
    private String registrationNumber;

    @Schema(description = "City where the nurse operates", example = "Lucknow")
    private String city;

    @Schema(description = "Consultation fee per visit", example = "500.00")
    private java.math.BigDecimal consultationFee;
}
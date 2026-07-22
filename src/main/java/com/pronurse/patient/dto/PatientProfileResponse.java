package com.pronurse.patient.dto;

import com.pronurse.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@Schema(description = "Response payload containing the details of a patient profile")
public class PatientProfileResponse {
    @Schema(description = "Internal database ID of the user", example = "2")
    private Long id;

    @Schema(description = "Unique identifier code of the patient", example = "PAT-12345")
    private String patientId;

    @Schema(description = "Full name of the patient", example = "John Doe")
    private String name;

    @Schema(description = "Contact mobile number", example = "+919876543211")
    private String mobile;

    @Schema(description = "Email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Gender of the patient", example = "MALE")
    private Gender gender;

    @Schema(description = "Date of birth", example = "1985-05-15")
    private LocalDate dob;

    @Schema(description = "Blood group", example = "O+")
    private String bloodGroup;

    @Schema(description = "Full home address details", example = "456 Wellness Ave, Lucknow")
    private String address;

    @Schema(description = "Path/URL to profile image", example = "/api/files/profile-image/2")
    private String profileImage;

    @Schema(description = "Path/URL to uploaded medical reports", example = "/api/files/download/report-123.pdf")
    private String medicalReport;

    @Schema(description = "Latitude of home address", example = "28.4595")
    private Double latitude;

    @Schema(description = "Longitude of home address", example = "77.0266")
    private Double longitude;

    @Schema(description = "Any documented chronic diseases or conditions", example = "Hypertension, Diabetes")
    private String chronicDiseases;
}
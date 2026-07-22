package com.pronurse.patient.dto;

import com.pronurse.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Payload to update or create a patient's profile details")
public class PatientProfileUpdateRequest {
    @NotBlank(message = "Name field cannot be left blank")
    @Schema(description = "Full name of the patient", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Email(message = "Please provide a valid email format layout")
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

    @Schema(description = "Latitude coordinates of home address", example = "28.4595")
    private String latitude;

    @Schema(description = "Longitude coordinates of home address", example = "77.0266")
    private String longitude;

    @Schema(description = "Chronic diseases or conditions (comma-separated)", example = "Hypertension, Diabetes")
    private String chronicDiseases;
}
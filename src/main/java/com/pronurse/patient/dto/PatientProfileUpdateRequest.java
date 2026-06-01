package com.pronurse.patient.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PatientProfileUpdateRequest {

    @NotBlank(message = "Patient ID is mandatory")
    private String patientId;

    @NotBlank(message = "Name field cannot be left blank")
    private String name;

    @Email(message = "Please provide a valid email format layout")
    private String email;

    @NotBlank(message = "Mobile number tracking is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number formatting")
    private String mobile;

    private String gender;
    private String dob;
    private String bloodGroup;
    private String address;
    private String latitude;
    private String longitude;
}
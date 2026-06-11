package com.pronurse.nurse.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class NurseProfileUpdateRequest {

    @NotBlank(message = "Nurse ID is mandatory")
    private String nurseId;

    @NotBlank(message = "Name field cannot be left blank")
    private String name;

    @Email(message = "Please provide a valid email format layout")
    private String email;

    @NotBlank(message = "Mobile number tracking is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number formatting")
    private String mobile;

    private String gender;
    private String dob;
    private String experience;
    private String qualification;
    private String specialization;
    private String languages;
    private String address;
    private String latitude;
    private String longitude;
}
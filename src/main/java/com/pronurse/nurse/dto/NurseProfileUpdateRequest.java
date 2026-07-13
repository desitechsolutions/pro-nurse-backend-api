package com.pronurse.nurse.dto;

import com.pronurse.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class NurseProfileUpdateRequest {
    @NotBlank(message = "Name field cannot be left blank")
    private String name;

    @Email(message = "Please provide a valid email format layout")
    private String email;

    private Gender gender;
    private LocalDate dob;
    private String experience;
    private String qualification;
    private String specialization;
    private String languages;
    private String address;
    private String latitude;
    private String longitude;
}
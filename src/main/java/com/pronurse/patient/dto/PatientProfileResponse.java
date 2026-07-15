package com.pronurse.patient.dto;

import com.pronurse.enums.Gender;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PatientProfileResponse {
    private Long id;
    private String patientId;
    private String name;
    private String mobile;
    private String email;
    private Gender gender;
    private LocalDate dob;
    private String bloodGroup;
    private String address;
    private String profileImage;
    private String medicalReport;
    private Double latitude;
    private Double longitude;
    private String chronicDiseases;
}
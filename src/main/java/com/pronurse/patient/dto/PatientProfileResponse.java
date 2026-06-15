package com.pronurse.patient.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PatientProfileResponse {
    private Long id;
    private String patientId;
    private String name;
    private String mobile;
    private String email;
    private String gender;
    private String dob;
    private String bloodGroup;
    private String address;
    private String profileImage;
    private Double latitude;
    private Double longitude;
}
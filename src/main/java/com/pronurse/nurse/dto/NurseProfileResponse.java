package com.pronurse.nurse.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NurseProfileResponse {
    private Long id;
    private String nurseId;
    private String name;
    private String mobile;
    private String email;
    private String gender;
    private String dob;
    private String qualification;
    private String experience;
    private String specialization;
    private String languages;
    private String address;
    private String profileImage;
    private double averageRating;
    private boolean isOnDuty;
    private String verificationStatus;
}
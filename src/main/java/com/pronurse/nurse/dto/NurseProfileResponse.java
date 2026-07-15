package com.pronurse.nurse.dto;

import com.pronurse.enums.Gender;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class NurseProfileResponse {
    private Long id;
    private String nurseId;
    private String name;
    private String mobile;
    private String email;
    private Gender gender;
    private LocalDate dob;
    private String qualification;
    private String experience;
    private String specialization;
    private String languages;
    private String address;
    private String profileImage;
    private String registrationNumber;
    private double averageRating;
    private boolean isOnDuty;
    private String verificationStatus;
    private String city;
    private java.math.BigDecimal consultationFee;
}
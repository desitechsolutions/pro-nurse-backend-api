package com.pronurse.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminNurseSummaryResponse {
    private Long id;
    private String nurseId;
    private String name;
    private String mobile;
    private String email;
    private String qualification;
    private String specialization;
    private String registrationNumber;
    private boolean isVerified;
    private boolean isOnDuty;
    private String verificationStatus;
    private double averageRating;
    private LocalDateTime createdAt;
}
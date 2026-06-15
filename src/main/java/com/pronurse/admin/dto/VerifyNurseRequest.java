package com.pronurse.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifyNurseRequest {
    @NotNull(message = "Nurse profile numeric ID context required")
    private Long nurseProfileId;

    @NotBlank(message = "Verification status target action must be defined (Approved/Rejected)")
    private String status; // 'Approved' or 'Rejected'

    private String rejectReason;
}
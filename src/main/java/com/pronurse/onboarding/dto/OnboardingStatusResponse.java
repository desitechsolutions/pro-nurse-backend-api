package com.pronurse.onboarding.dto;

import com.pronurse.onboarding.enums.OnboardingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full onboarding status snapshot for a nurse.
 * Returned to both nurses (own status) and admins (reviewing a nurse's submission).
 */
@Data
@Builder
@Schema(description = "Complete onboarding status including all submitted documents and progress counters")
public class OnboardingStatusResponse {

    @Schema(description = "Database ID of the nurse profile", example = "7")
    private Long nurseProfileId;

    @Schema(description = "Unique nurse identifier code", example = "NUR-7-123")
    private String nurseId;

    @Schema(description = "Nurse full name", example = "Priya Sharma")
    private String name;

    @Schema(description = "Nurse mobile number", example = "9876543210")
    private String mobile;

    @Schema(description = "Current onboarding lifecycle status", example = "UNDER_REVIEW")
    private OnboardingStatus onboardingStatus;

    @Schema(description = "Whether the nurse has been fully verified and is eligible for dispatch")
    private boolean isVerified;

    @Schema(description = "All onboarding documents submitted by this nurse")
    private List<NurseDocumentResponse> documents;

    @Schema(description = "Number of documents currently awaiting admin review", example = "2")
    private long pendingCount;

    @Schema(description = "Number of documents approved by admin", example = "2")
    private long approvedCount;

    @Schema(description = "Number of documents rejected by admin", example = "1")
    private long rejectedCount;

    @Schema(description = "Total number of documents submitted (all versions, all statuses)", example = "5")
    private long totalDocumentCount;

    @Schema(description = "Timestamp of the most recent onboarding update")
    private LocalDateTime lastUpdatedAt;
}

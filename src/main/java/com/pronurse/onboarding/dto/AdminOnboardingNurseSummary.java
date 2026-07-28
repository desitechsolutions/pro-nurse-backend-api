package com.pronurse.onboarding.dto;

import com.pronurse.onboarding.enums.OnboardingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Lightweight nurse summary shown in the admin onboarding management list.
 * Contains enough info for admin to prioritise which nurses to review next.
 */
@Data
@Builder
@Schema(description = "Summary view of a nurse's onboarding status for admin listing")
public class AdminOnboardingNurseSummary {

    @Schema(description = "Nurse profile database ID", example = "7")
    private Long nurseProfileId;

    @Schema(description = "Unique nurse identifier code", example = "NUR-7-123")
    private String nurseId;

    @Schema(description = "Nurse full name", example = "Priya Sharma")
    private String name;

    @Schema(description = "Nurse mobile number", example = "9876543210")
    private String mobile;

    @Schema(description = "Nurse email address")
    private String email;

    @Schema(description = "Current onboarding lifecycle status", example = "UNDER_REVIEW")
    private OnboardingStatus onboardingStatus;

    @Schema(description = "Total documents submitted (across all versions)", example = "4")
    private long totalDocuments;

    @Schema(description = "Documents awaiting review", example = "2")
    private long pendingCount;

    @Schema(description = "Documents approved", example = "1")
    private long approvedCount;

    @Schema(description = "Documents rejected", example = "1")
    private long rejectedCount;

    @Schema(description = "Timestamp of nurse profile creation")
    private LocalDateTime registeredAt;

    @Schema(description = "Timestamp of the most recent onboarding activity")
    private LocalDateTime lastUpdatedAt;
}

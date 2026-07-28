package com.pronurse.onboarding.enums;

/**
 * Overall onboarding lifecycle status for a nurse.
 * Stored on {@code NurseProfile.onboardingStatus}.
 *
 * <p>State machine:
 * <pre>
 *   DRAFT
 *     | (nurse submits with >= 1 document)
 *   UNDER_REVIEW
 *     |                        |
 *     | all docs APPROVED      | some docs REJECTED
 *   APPROVED             CHANGES_REQUESTED
 *     |                        | (nurse re-uploads rejected docs)
 *   ONBOARDING_COMPLETED DOCUMENTS_RESUBMITTED
 *                              | (admin reviews again)
 *                         UNDER_REVIEW  (loop)
 * </pre>
 * </p>
 *
 * <p>Terminal state: {@code ONBOARDING_COMPLETED} also sets
 * {@code NurseProfile.isVerified = true}, {@code verificationStatus = "Approved"},
 * and {@code isOnDuty = true} to make the nurse eligible for dispatch.</p>
 */
public enum OnboardingStatus {

    /** Nurse has registered but has not uploaded any documents yet. */
    DRAFT,

    /**
     * Nurse has uploaded at least one document and submitted for review.
     * Admin review is pending.
     */
    UNDER_REVIEW,

    /**
     * Admin has rejected one or more documents.
     * Nurse must re-upload the rejected documents.
     */
    CHANGES_REQUESTED,

    /**
     * Nurse has re-uploaded the rejected documents.
     * Admin review is pending again.
     */
    DOCUMENTS_RESUBMITTED,

    /**
     * All submitted documents have been approved by the admin.
     * Admin must call the complete endpoint to finalize onboarding.
     */
    APPROVED,

    /**
     * Terminal state. Nurse is fully onboarded and eligible for dispatch.
     * {@code NurseProfile.isVerified = true} and {@code isOnDuty = true}.
     */
    ONBOARDING_COMPLETED
}

package com.pronurse.onboarding.enums;

/**
 * Lifecycle status of an individual nurse onboarding document.
 *
 * <p>State transitions:
 * <pre>
 *   (upload)     (admin)      (nurse)        (admin)
 *   PENDING  ->  APPROVED
 *   PENDING  ->  REJECTED  ->  RESUBMITTED  ->  PENDING (new document record)
 * </pre>
 * </p>
 */
public enum DocumentStatus {

    /** Document has been uploaded by the nurse and is awaiting admin review. */
    PENDING,

    /** Document has been reviewed and approved by an admin. */
    APPROVED,

    /** Document has been reviewed and rejected by an admin. A new document must be submitted. */
    REJECTED,

    /**
     * The nurse has replaced a previously rejected document.
     * A new NurseDocument record is created for the replacement; the old one retains REJECTED status.
     */
    RESUBMITTED
}

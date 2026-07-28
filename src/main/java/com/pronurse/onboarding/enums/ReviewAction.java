package com.pronurse.onboarding.enums;

/**
 * The action taken during a document review event.
 * Recorded in {@code DocumentReviewHistory} to provide a complete audit trail.
 */
public enum ReviewAction {

    /** Nurse uploaded the document for the first time. */
    SUBMITTED,

    /** Admin approved the document. */
    APPROVED,

    /** Admin rejected the document (comment required). */
    REJECTED,

    /** Nurse re-uploaded a previously rejected document. */
    RESUBMITTED,

    /** Admin requested an additional document type not yet uploaded. */
    ADDITIONAL_REQUESTED
}

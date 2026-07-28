package com.pronurse.onboarding.service;

import com.pronurse.onboarding.dto.*;
import com.pronurse.onboarding.enums.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for the nurse onboarding document lifecycle.
 *
 * <p>Covers two actor perspectives:
 * <ul>
 *   <li><b>Nurse:</b> upload, replace, submit, view own status</li>
 *   <li><b>Admin:</b> list, review, complete, view history</li>
 * </ul>
 * </p>
 */
public interface OnboardingService {

    // =========================================================================
    // NURSE OPERATIONS
    // =========================================================================

    /**
     * Upload a new onboarding document for the authenticated nurse.
     *
     * <p>Allowed when onboardingStatus is DRAFT, CHANGES_REQUESTED, or DOCUMENTS_RESUBMITTED.
     * If a document of the same type already exists and is PENDING or APPROVED, an error is thrown.</p>
     *
     * @param mobile       Mobile number of the authenticated nurse
     * @param documentType Category of the document
     * @param file         The uploaded file (PDF/JPG/PNG, max 20MB)
     * @return             The created NurseDocument response
     */
    NurseDocumentResponse uploadDocument(String mobile, DocumentType documentType, MultipartFile file);

    /**
     * Replace a previously REJECTED document with a new file.
     *
     * <p>Creates a new NurseDocument record with version = old.version + 1.
     * The old rejected record is preserved in history.
     * Transitions onboardingStatus to DOCUMENTS_RESUBMITTED if all previously
     * rejected docs now have replacements pending.</p>
     *
     * @param mobile  Mobile number of the authenticated nurse
     * @param docId   ID of the REJECTED document to replace
     * @param file    The new file
     * @return        The newly created replacement NurseDocument response
     */
    NurseDocumentResponse replaceDocument(String mobile, Long docId, MultipartFile file);

    /**
     * Submit all uploaded documents for admin review.
     *
     * <p>Transitions onboardingStatus to UNDER_REVIEW.
     * Requires at least one document to be uploaded.
     * Can only be called when status is DRAFT, CHANGES_REQUESTED, or DOCUMENTS_RESUBMITTED.</p>
     *
     * @param mobile Mobile number of the authenticated nurse
     */
    void submitForReview(String mobile);

    /**
     * Get the full onboarding status snapshot for the authenticated nurse,
     * including all documents and progress counters.
     *
     * @param mobile Mobile number of the authenticated nurse
     * @return       Full onboarding status with document list
     */
    OnboardingStatusResponse getOnboardingStatus(String mobile);

    // =========================================================================
    // ADMIN OPERATIONS
    // =========================================================================

    /**
     * Review (approve, reject, or request additional) an individual document.
     *
     * <p>Can only be called when the nurse's onboardingStatus is UNDER_REVIEW.
     * After rejection, onboardingStatus transitions to CHANGES_REQUESTED.
     * After approving the last pending document, onboardingStatus transitions to APPROVED.
     * Triggers an FCM push notification to the nurse.</p>
     *
     * @param adminMobile Mobile number of the acting admin
     * @param docId       ID of the document to review
     * @param request     Review action and optional comment
     */
    void reviewDocument(String adminMobile, Long docId, DocumentReviewRequest request);

    /**
     * Mark a nurse's onboarding as fully COMPLETED.
     *
     * <p>Validates that all active documents are APPROVED.
     * Transitions onboardingStatus to ONBOARDING_COMPLETED.
     * Also sets isVerified = true, verificationStatus = "Approved", isOnDuty = true.
     * Triggers a congratulatory FCM push to the nurse.</p>
     *
     * @param adminMobile    Mobile number of the acting admin
     * @param nurseProfileId Nurse profile ID
     * @param comment        Optional completion note
     */
    void completeOnboarding(String adminMobile, Long nurseProfileId, String comment);

    /**
     * Get the full onboarding status for a nurse by profile ID (admin view).
     *
     * @param nurseProfileId Nurse profile database ID
     * @return               Full onboarding status with document list
     */
    OnboardingStatusResponse getOnboardingStatusByNurseProfileId(Long nurseProfileId);

    /**
     * Paginated list of nurses filtered by onboarding status (for admin management panel).
     *
     * @param status   OnboardingStatus name to filter by (or null for all)
     * @param pageable Pagination parameters
     * @return         Page of nurse summaries
     */
    Page<AdminOnboardingNurseSummary> listNursesByOnboardingStatus(String status, Pageable pageable);

    /**
     * Fetch the full review audit history for a specific document.
     *
     * @param docId Document ID
     * @return      Chronological list of review history events
     */
    List<DocumentReviewHistoryResponse> getDocumentHistory(Long docId);

    /**
     * Fetch document metadata needed for secure file download (admin use).
     * Returns the stored filename and the nurse profile ID (used to build the folder path).
     *
     * @param docId Document ID
     * @return      NurseDocumentResponse containing fileName and nurseProfileId
     */
    NurseDocumentResponse getDocumentById(Long docId);
}

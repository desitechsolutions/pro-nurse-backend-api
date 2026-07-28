package com.pronurse.onboarding.repository;

import com.pronurse.onboarding.entity.DocumentReviewHistory;
import com.pronurse.onboarding.enums.ReviewAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentReviewHistoryRepository extends JpaRepository<DocumentReviewHistory, Long> {

    /**
     * Fetch full audit history for a specific document, ordered chronologically.
     */
    List<DocumentReviewHistory> findByDocumentIdOrderByPerformedAtAsc(Long documentId);

    /**
     * Fetch all history events for a nurse profile, across all documents.
     * Useful for admin timeline view.
     */
    List<DocumentReviewHistory> findByNurseProfileIdOrderByPerformedAtDesc(Long nurseProfileId);

    /**
     * Fetch history for a document filtered by specific action.
     */
    List<DocumentReviewHistory> findByDocumentIdAndActionOrderByPerformedAtDesc(
            Long documentId, ReviewAction action);

    /**
     * Count how many times a specific document has been reviewed (APPROVED or REJECTED).
     */
    long countByDocumentIdAndActionIn(Long documentId, List<ReviewAction> actions);
}

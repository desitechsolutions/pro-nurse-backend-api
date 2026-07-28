package com.pronurse.onboarding.repository;

import com.pronurse.onboarding.entity.NurseDocument;
import com.pronurse.onboarding.enums.DocumentStatus;
import com.pronurse.onboarding.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NurseDocumentRepository extends JpaRepository<NurseDocument, Long> {

    /**
     * Fetch all documents for a nurse profile ordered by upload date descending.
     */
    List<NurseDocument> findByNurseProfileIdOrderByUploadedAtDesc(Long nurseProfileId);

    /**
     * Fetch all documents for a nurse with a specific status.
     */
    List<NurseDocument> findByNurseProfileIdAndDocumentStatus(Long nurseProfileId, DocumentStatus status);

    /**
     * Find the latest active (highest version) document of a specific type for a nurse.
     * Used to prevent duplicate uploads of the same document type.
     */
    Optional<NurseDocument> findTopByNurseProfileIdAndDocumentTypeOrderByVersionDesc(
            Long nurseProfileId, DocumentType documentType);

    /**
     * Count documents for a nurse with a specific status.
     * Used for onboarding progress calculation.
     */
    long countByNurseProfileIdAndDocumentStatus(Long nurseProfileId, DocumentStatus status);

    /**
     * Count all non-rejected documents for a nurse (PENDING + APPROVED).
     * Used to determine if submission is possible.
     */
    @Query("SELECT COUNT(d) FROM NurseDocument d " +
           "WHERE d.nurseProfile.id = :nurseProfileId " +
           "AND d.documentStatus <> com.pronurse.onboarding.enums.DocumentStatus.REJECTED")
    long countActiveDocuments(@Param("nurseProfileId") Long nurseProfileId);

    /**
     * Check if all active documents (latest version of each type) for a nurse are APPROVED.
     * Returns true only if every document has status APPROVED.
     */
    @Query("SELECT CASE WHEN COUNT(d) = 0 THEN false " +
           "ELSE (COUNT(d) = SUM(CASE WHEN d.documentStatus = com.pronurse.onboarding.enums.DocumentStatus.APPROVED THEN 1 ELSE 0 END)) " +
           "END FROM NurseDocument d " +
           "WHERE d.nurseProfile.id = :nurseProfileId " +
           "AND d.documentStatus <> com.pronurse.onboarding.enums.DocumentStatus.REJECTED")
    boolean allActiveDocumentsApproved(@Param("nurseProfileId") Long nurseProfileId);

    /**
     * Fetch all REJECTED documents for a nurse — shown to nurse for resubmission.
     */
    @Query("SELECT d FROM NurseDocument d " +
           "WHERE d.nurseProfile.id = :nurseProfileId " +
           "AND d.documentStatus = com.pronurse.onboarding.enums.DocumentStatus.REJECTED " +
           "ORDER BY d.uploadedAt DESC")
    List<NurseDocument> findRejectedDocuments(@Param("nurseProfileId") Long nurseProfileId);

    /**
     * Find the maximum version for a given nurse and document type.
     * Returns 0 if no document of that type exists yet.
     */
    @Query("SELECT COALESCE(MAX(d.version), 0) FROM NurseDocument d " +
           "WHERE d.nurseProfile.id = :nurseProfileId " +
           "AND d.documentType = :documentType")
    int findMaxVersion(@Param("nurseProfileId") Long nurseProfileId,
                       @Param("documentType") DocumentType documentType);
}

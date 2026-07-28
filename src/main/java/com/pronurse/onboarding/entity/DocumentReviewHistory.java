package com.pronurse.onboarding.entity;

import com.pronurse.onboarding.enums.ReviewAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Immutable audit record for every action taken on a {@link NurseDocument}.
 *
 * <p>Events are appended (never updated) to maintain a complete, tamper-evident
 * review history that can be inspected by both admins and nurses.</p>
 *
 * <p>A new record is created for each of the following events:
 * <ul>
 *   <li>Nurse uploads a document (SUBMITTED)</li>
 *   <li>Admin approves a document (APPROVED)</li>
 *   <li>Admin rejects a document (REJECTED)</li>
 *   <li>Nurse re-uploads a rejected document (RESUBMITTED)</li>
 *   <li>Admin requests an additional document type (ADDITIONAL_REQUESTED)</li>
 * </ul>
 * </p>
 */
@Entity
@Table(
        name = "document_review_history",
        indexes = {
                @Index(name = "idx_doc_review_hist_doc",     columnList = "document_id"),
                @Index(name = "idx_doc_review_hist_profile", columnList = "nurse_profile_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentReviewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The document this history record is associated with.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private NurseDocument document;

    /**
     * Denormalized nurse profile ID for fast filtering without joining nurse_documents.
     */
    @Column(name = "nurse_profile_id", nullable = false)
    private Long nurseProfileId;

    /**
     * The review event action (SUBMITTED, APPROVED, REJECTED, RESUBMITTED, ADDITIONAL_REQUESTED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private ReviewAction action;

    /**
     * Optional comment associated with this review action.
     * Required for REJECTED actions.
     */
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    /**
     * Mobile number of the actor who performed this action.
     * Can be the nurse (on SUBMITTED/RESUBMITTED) or an admin (on APPROVED/REJECTED).
     */
    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "performed_at", updatable = false, nullable = false)
    private LocalDateTime performedAt;

    @PrePersist
    protected void onCreate() {
        this.performedAt = LocalDateTime.now();
    }
}

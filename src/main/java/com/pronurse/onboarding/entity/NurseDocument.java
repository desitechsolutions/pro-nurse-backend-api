package com.pronurse.onboarding.entity;

import com.pronurse.nurse.entity.NurseProfile;
import com.pronurse.onboarding.enums.DocumentStatus;
import com.pronurse.onboarding.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a single onboarding document submitted by a nurse.
 *
 * <p>Each upload creates one record. When a nurse replaces a rejected document,
 * a NEW record is created with an incremented {@code version}, while the old
 * rejected record is preserved for audit purposes.</p>
 *
 * <p>File is stored on disk at:
 * {@code uploads/documents/nurses/{nurseProfileId}/{id}_{version}_{timestamp}_{originalName}}</p>
 */
@Entity
@Table(
        name = "nurse_documents",
        indexes = {
                @Index(name = "idx_nurse_doc_profile", columnList = "nurse_profile_id"),
                @Index(name = "idx_nurse_doc_status",  columnList = "document_status")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The nurse profile this document belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nurse_profile_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private NurseProfile nurseProfile;

    /**
     * Category of the document (e.g. NURSING_CERTIFICATE, AADHAAR_CARD).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    /**
     * Current review status of this document.
     * Defaults to PENDING on upload.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_status", nullable = false, length = 30)
    @Builder.Default
    private DocumentStatus documentStatus = DocumentStatus.PENDING;

    /**
     * Filename as stored on disk (unique per file, includes timestamp).
     * Used to construct the download path.
     */
    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    /**
     * Original filename provided by the user at upload time.
     * Used for display purposes only.
     */
    @Column(name = "original_name", length = 255)
    private String originalName;

    /**
     * Size of the uploaded file in bytes.
     */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    /**
     * MIME type of the uploaded file (e.g. application/pdf, image/jpeg).
     */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /**
     * Upload version — starts at 1 and increments each time a rejected document
     * is replaced. Allows multiple versions of the same document type to coexist.
     */
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    /**
     * Admin's review comment for this document (rejection reason or approval note).
     * Required when status is REJECTED.
     */
    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    /**
     * Mobile number of the admin who last reviewed this document.
     */
    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    /**
     * Timestamp when the last review action was taken.
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

package com.pronurse.onboarding.service;

import com.pronurse.auth.repository.UserRepository;
import com.pronurse.common.exception.ApplicationException;
import com.pronurse.common.util.LocalFileStorageServiceUtil;
import com.pronurse.notification.service.FCMService;
import com.pronurse.nurse.entity.NurseProfile;
import com.pronurse.nurse.repository.NurseProfileRepository;
import com.pronurse.onboarding.dto.*;
import com.pronurse.onboarding.entity.DocumentReviewHistory;
import com.pronurse.onboarding.entity.NurseDocument;
import com.pronurse.onboarding.enums.DocumentStatus;
import com.pronurse.onboarding.enums.DocumentType;
import com.pronurse.onboarding.enums.OnboardingStatus;
import com.pronurse.onboarding.enums.ReviewAction;
import com.pronurse.onboarding.repository.DocumentReviewHistoryRepository;
import com.pronurse.onboarding.repository.NurseDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingServiceImpl implements OnboardingService {

    private static final String DOCUMENT_STORAGE_FOLDER_PREFIX = "documents/nurses/";
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/jpg", "image/png"
    );

    private final NurseProfileRepository nurseProfileRepository;
    private final NurseDocumentRepository nurseDocumentRepository;
    private final DocumentReviewHistoryRepository reviewHistoryRepository;
    private final UserRepository userRepository;
    private final LocalFileStorageServiceUtil fileStorageUtil;
    private final FCMService fcmService;

    // =========================================================================
    // NURSE OPERATIONS
    // =========================================================================

    @Override
    @Transactional
    public NurseDocumentResponse uploadDocument(String mobile, DocumentType documentType, MultipartFile file) {
        NurseProfile profile = getNurseProfileByMobile(mobile);

        // State gate: nurse can only upload in specific states
        assertNurseCanUpload(profile);

        // File validation
        fileStorageUtil.validateFileType(file, ALLOWED_MIME_TYPES);

        // Business rule: prevent duplicate active documents of the same type
        // (PENDING or APPROVED documents of the same type already exist)
        nurseDocumentRepository
                .findTopByNurseProfileIdAndDocumentTypeOrderByVersionDesc(profile.getId(), documentType)
                .ifPresent(existing -> {
                    if (existing.getDocumentStatus() == DocumentStatus.PENDING) {
                        throw new ApplicationException(
                                "Your '" + documentType.getReadableName() + "' document is already uploaded and is " +
                                "currently awaiting admin review. No action needed — you will be notified once it is reviewed.");
                    }
                    if (existing.getDocumentStatus() == DocumentStatus.APPROVED) {
                        throw new ApplicationException(
                                "Your '" + documentType.getReadableName() + "' document has already been approved. " +
                                "No further action is required for this document type.");
                    }
                });

        // Store the file
        String folder = DOCUMENT_STORAGE_FOLDER_PREFIX + profile.getId();
        String storedFileName;
        try {
            storedFileName = fileStorageUtil.storeFile(file, folder, profile.getId());
        } catch (Exception e) {
            log.error("Failed to store onboarding document for nurse: {}", mobile, e);
            throw new ApplicationException("File storage failed: " + e.getMessage());
        }

        // Determine version (always 1 for fresh uploads)
        int version = 1;

        // Persist the document record
        NurseDocument document = NurseDocument.builder()
                .nurseProfile(profile)
                .documentType(documentType)
                .documentStatus(DocumentStatus.PENDING)
                .fileName(storedFileName)
                .originalName(file.getOriginalFilename())
                .fileSizeBytes(file.getSize())
                .mimeType(file.getContentType())
                .version(version)
                .build();
        document = nurseDocumentRepository.save(document);

        // Append audit history
        appendHistory(document, profile.getId(), ReviewAction.SUBMITTED, null, mobile);

        log.info("Nurse {} uploaded {} document (ID: {})", mobile, documentType, document.getId());
        return toDocumentResponse(document);
    }

    @Override
    @Transactional
    public NurseDocumentResponse replaceDocument(String mobile, Long docId, MultipartFile file) {
        NurseProfile profile = getNurseProfileByMobile(mobile);

        // State gate
        assertNurseCanUpload(profile);

        // Verify document exists and belongs to this nurse
        NurseDocument rejectedDoc = nurseDocumentRepository.findById(docId)
                .orElseThrow(() -> new ApplicationException("Document not found with ID: " + docId));

        if (!rejectedDoc.getNurseProfile().getId().equals(profile.getId())) {
            throw new ApplicationException("Access denied: This document does not belong to your profile.");
        }

        if (rejectedDoc.getDocumentStatus() != DocumentStatus.REJECTED) {
            throw new ApplicationException(
                    "Only REJECTED documents can be replaced. Current status: " + rejectedDoc.getDocumentStatus());
        }

        // File validation
        fileStorageUtil.validateFileType(file, ALLOWED_MIME_TYPES);

        // Store the replacement file
        String folder = DOCUMENT_STORAGE_FOLDER_PREFIX + profile.getId();
        String storedFileName;
        try {
            storedFileName = fileStorageUtil.storeFile(file, folder, profile.getId());
        } catch (Exception e) {
            log.error("Failed to store replacement document for nurse: {}", mobile, e);
            throw new ApplicationException("File storage failed: " + e.getMessage());
        }

        // Find the next version number for this document type
        int nextVersion = nurseDocumentRepository.findMaxVersion(profile.getId(), rejectedDoc.getDocumentType()) + 1;

        // Create a NEW document record for the replacement (old record preserved for history)
        NurseDocument replacement = NurseDocument.builder()
                .nurseProfile(profile)
                .documentType(rejectedDoc.getDocumentType())
                .documentStatus(DocumentStatus.PENDING)
                .fileName(storedFileName)
                .originalName(file.getOriginalFilename())
                .fileSizeBytes(file.getSize())
                .mimeType(file.getContentType())
                .version(nextVersion)
                .build();
        replacement = nurseDocumentRepository.save(replacement);

        // Append audit history on replacement
        appendHistory(replacement, profile.getId(), ReviewAction.RESUBMITTED,
                "Replaced rejected document (previous ID: " + docId + ")", mobile);

        // Transition profile onboarding status to DOCUMENTS_RESUBMITTED
        profile.setOnboardingStatus(OnboardingStatus.DOCUMENTS_RESUBMITTED);
        profile.setUpdatedAt(LocalDateTime.now());
        nurseProfileRepository.save(profile);

        log.info("Nurse {} replaced rejected document ID {} with new document ID {}",
                mobile, docId, replacement.getId());
        return toDocumentResponse(replacement);
    }

    @Override
    @Transactional
    public void submitForReview(String mobile) {
        NurseProfile profile = getNurseProfileByMobile(mobile);

        // State gate: can only submit when in an uploadable state
        OnboardingStatus currentStatus = profile.getOnboardingStatus();
        if (currentStatus != OnboardingStatus.DRAFT &&
                currentStatus != OnboardingStatus.CHANGES_REQUESTED &&
                currentStatus != OnboardingStatus.DOCUMENTS_RESUBMITTED) {
            throw new ApplicationException(
                    "Cannot submit for review. Current onboarding status: " + currentStatus +
                    ". Submission is only allowed in DRAFT, CHANGES_REQUESTED, or DOCUMENTS_RESUBMITTED states.");
        }

        // Validate at least one document exists
        long activeDocCount = nurseDocumentRepository.countActiveDocuments(profile.getId());
        if (activeDocCount == 0) {
            throw new ApplicationException(
                    "You must upload at least one document before submitting for review.");
        }

        // Transition to UNDER_REVIEW
        profile.setOnboardingStatus(OnboardingStatus.UNDER_REVIEW);
        profile.setUpdatedAt(LocalDateTime.now());
        nurseProfileRepository.save(profile);

        log.info("Nurse {} submitted documents for review. Active document count: {}", mobile, activeDocCount);
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingStatusResponse getOnboardingStatus(String mobile) {
        NurseProfile profile = getNurseProfileByMobile(mobile);
        return buildOnboardingStatusResponse(profile);
    }

    // =========================================================================
    // ADMIN OPERATIONS
    // =========================================================================

    @Override
    @Transactional
    public void reviewDocument(String adminMobile, Long docId, DocumentReviewRequest request) {
        NurseDocument document = nurseDocumentRepository.findById(docId)
                .orElseThrow(() -> new ApplicationException("Document not found with ID: " + docId));

        NurseProfile profile = document.getNurseProfile();

        // State gate: admin can only review when profile is UNDER_REVIEW
        if (profile.getOnboardingStatus() != OnboardingStatus.UNDER_REVIEW) {
            throw new ApplicationException(
                    "Documents can only be reviewed when onboarding status is UNDER_REVIEW. " +
                    "Current status: " + profile.getOnboardingStatus());
        }

        // Document state gate: only PENDING docs can be reviewed
        if (document.getDocumentStatus() != DocumentStatus.PENDING) {
            throw new ApplicationException(
                    "Only PENDING documents can be reviewed. Current document status: " + document.getDocumentStatus());
        }

        // Validate comment is provided for REJECTED and ADDITIONAL_REQUESTED actions
        if ((request.getAction() == ReviewAction.REJECTED || request.getAction() == ReviewAction.ADDITIONAL_REQUESTED)
                && (request.getComment() == null || request.getComment().isBlank())) {
            throw new ApplicationException(
                    "A comment/reason is required when rejecting a document or requesting additional documents.");
        }

        switch (request.getAction()) {
            case APPROVED -> {
                document.setDocumentStatus(DocumentStatus.APPROVED);
                document.setReviewComment(request.getComment());
                document.setReviewedBy(adminMobile);
                document.setReviewedAt(LocalDateTime.now());
                nurseDocumentRepository.save(document);

                appendHistory(document, profile.getId(), ReviewAction.APPROVED, request.getComment(), adminMobile);

                // Check if all active documents are now approved
                boolean allApproved = nurseDocumentRepository.allActiveDocumentsApproved(profile.getId());
                if (allApproved) {
                    profile.setOnboardingStatus(OnboardingStatus.APPROVED);
                    profile.setUpdatedAt(LocalDateTime.now());
                    nurseProfileRepository.save(profile);
                    log.info("All documents approved for nurse profile ID {}. Status -> APPROVED", profile.getId());
                }

                // Send FCM notification
                sendOnboardingNotification(profile,
                        "Document Approved ✅",
                        "Your " + document.getDocumentType().getReadableName() + " has been approved.");
            }

            case REJECTED -> {
                document.setDocumentStatus(DocumentStatus.REJECTED);
                document.setReviewComment(request.getComment());
                document.setReviewedBy(adminMobile);
                document.setReviewedAt(LocalDateTime.now());
                nurseDocumentRepository.save(document);

                appendHistory(document, profile.getId(), ReviewAction.REJECTED, request.getComment(), adminMobile);

                // Transition overall onboarding to CHANGES_REQUESTED
                profile.setOnboardingStatus(OnboardingStatus.CHANGES_REQUESTED);
                profile.setUpdatedAt(LocalDateTime.now());
                nurseProfileRepository.save(profile);

                // Send FCM notification
                sendOnboardingNotification(profile,
                        "Action Required ⚠️",
                        "Your " + document.getDocumentType().getReadableName() +
                        " was rejected: " + request.getComment());

                log.info("Admin {} rejected document ID {} for nurse profile ID {}. Reason: {}",
                        adminMobile, docId, profile.getId(), request.getComment());
            }

            case ADDITIONAL_REQUESTED -> {
                appendHistory(document, profile.getId(), ReviewAction.ADDITIONAL_REQUESTED,
                        request.getComment(), adminMobile);

                // Keep status as CHANGES_REQUESTED so nurse knows to act
                profile.setOnboardingStatus(OnboardingStatus.CHANGES_REQUESTED);
                profile.setUpdatedAt(LocalDateTime.now());
                nurseProfileRepository.save(profile);

                sendOnboardingNotification(profile,
                        "Additional Document Required 📋",
                        "Admin has requested additional information: " + request.getComment());

                log.info("Admin {} requested additional document for nurse profile ID {}",
                        adminMobile, profile.getId());
            }

            default -> throw new ApplicationException("Unsupported review action: " + request.getAction());
        }
    }

    @Override
    @Transactional
    public void completeOnboarding(String adminMobile, Long nurseProfileId, String comment) {
        NurseProfile profile = nurseProfileRepository.findById(nurseProfileId)
                .orElseThrow(() -> new ApplicationException("Nurse profile not found with ID: " + nurseProfileId));

        // State gate: must be in APPROVED state before completing
        if (profile.getOnboardingStatus() != OnboardingStatus.APPROVED) {
            throw new ApplicationException(
                    "Onboarding can only be completed when status is APPROVED. " +
                    "Current status: " + profile.getOnboardingStatus() +
                    ". Ensure all documents have been reviewed and approved first.");
        }

        // Double-check all documents are actually approved
        if (!nurseDocumentRepository.allActiveDocumentsApproved(nurseProfileId)) {
            throw new ApplicationException(
                    "Cannot complete onboarding: one or more documents are still pending review or rejected.");
        }

        // Mark onboarding as completed — sync legacy fields for backward compatibility
        profile.setOnboardingStatus(OnboardingStatus.ONBOARDING_COMPLETED);
        profile.setVerified(true);
        profile.setVerificationStatus("Approved");  // Keep legacy field in sync
        profile.setVerifiedBy(adminMobile);
        profile.setVerifiedDate(LocalDateTime.now());
        profile.setOnDuty(true);                    // Nurse is now eligible for dispatch
        profile.setUpdatedAt(LocalDateTime.now());
        nurseProfileRepository.save(profile);

        log.info("Admin {} completed onboarding for nurse profile ID {}. Nurse is now VERIFIED and ON DUTY.",
                adminMobile, nurseProfileId);

        // Send congratulatory FCM push notification
        sendOnboardingNotification(profile,
                "Onboarding Complete! 🎉",
                "Congratulations! Your onboarding has been completed. You are now eligible to receive bookings.");
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingStatusResponse getOnboardingStatusByNurseProfileId(Long nurseProfileId) {
        NurseProfile profile = nurseProfileRepository.findById(nurseProfileId)
                .orElseThrow(() -> new ApplicationException("Nurse profile not found with ID: " + nurseProfileId));
        return buildOnboardingStatusResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminOnboardingNurseSummary> listNursesByOnboardingStatus(String status, Pageable pageable) {
        Page<NurseProfile> profiles;

        if (status != null && !status.isBlank()) {
            OnboardingStatus targetStatus;
            try {
                targetStatus = OnboardingStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApplicationException("Invalid onboarding status filter: '" + status + "'.");
            }
            profiles = nurseProfileRepository.findByOnboardingStatus(targetStatus, pageable);
        } else {
            profiles = nurseProfileRepository.findAll(pageable);
        }

        return profiles.map(profile -> {
            long pending  = nurseDocumentRepository.countByNurseProfileIdAndDocumentStatus(profile.getId(), DocumentStatus.PENDING);
            long approved = nurseDocumentRepository.countByNurseProfileIdAndDocumentStatus(profile.getId(), DocumentStatus.APPROVED);
            long rejected = nurseDocumentRepository.countByNurseProfileIdAndDocumentStatus(profile.getId(), DocumentStatus.REJECTED);
            long total    = nurseDocumentRepository.countByNurseProfileIdAndDocumentStatus(profile.getId(), DocumentStatus.PENDING)
                          + approved + rejected
                          + nurseDocumentRepository.countByNurseProfileIdAndDocumentStatus(profile.getId(), DocumentStatus.RESUBMITTED);

            return AdminOnboardingNurseSummary.builder()
                    .nurseProfileId(profile.getId())
                    .nurseId(profile.getNurseId())
                    .name(profile.getUser().getName())
                    .mobile(profile.getUser().getMobile())
                    .email(profile.getUser().getEmail())
                    .onboardingStatus(profile.getOnboardingStatus())
                    .totalDocuments(total)
                    .pendingCount(pending)
                    .approvedCount(approved)
                    .rejectedCount(rejected)
                    .registeredAt(profile.getCreatedAt())
                    .lastUpdatedAt(profile.getUpdatedAt())
                    .build();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentReviewHistoryResponse> getDocumentHistory(Long docId) {
        NurseDocument document = nurseDocumentRepository.findById(docId)
                .orElseThrow(() -> new ApplicationException("Document not found with ID: " + docId));

        return reviewHistoryRepository.findByDocumentIdOrderByPerformedAtAsc(docId).stream()
                .map(h -> DocumentReviewHistoryResponse.builder()
                        .id(h.getId())
                        .documentId(docId)
                        .documentType(document.getDocumentType())
                        .documentTypeName(document.getDocumentType().getReadableName())
                        .action(h.getAction())
                        .comment(h.getComment())
                        .performedBy(h.getPerformedBy())
                        .performedAt(h.getPerformedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public NurseDocumentResponse getDocumentById(Long docId) {
        NurseDocument document = nurseDocumentRepository.findById(docId)
                .orElseThrow(() -> new ApplicationException("Document not found with ID: " + docId));
        return toDocumentResponse(document);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private NurseProfile getNurseProfileByMobile(String mobile) {
        return nurseProfileRepository.findByUserMobile(mobile)
                .orElseThrow(() -> new ApplicationException("Nurse profile not found for mobile: " + mobile));
    }

    private void assertNurseCanUpload(NurseProfile profile) {
        OnboardingStatus status = profile.getOnboardingStatus();
        if (status != OnboardingStatus.DRAFT &&
                status != OnboardingStatus.CHANGES_REQUESTED &&
                status != OnboardingStatus.DOCUMENTS_RESUBMITTED) {
            throw new ApplicationException(
                    "Document upload is not permitted in the current onboarding state: " + status +
                    ". Upload is only allowed when status is DRAFT, CHANGES_REQUESTED, or DOCUMENTS_RESUBMITTED.");
        }
    }

    private void appendHistory(NurseDocument document, Long nurseProfileId,
                               ReviewAction action, String comment, String performedBy) {
        DocumentReviewHistory history = DocumentReviewHistory.builder()
                .document(document)
                .nurseProfileId(nurseProfileId)
                .action(action)
                .comment(comment)
                .performedBy(performedBy)
                .build();
        reviewHistoryRepository.save(history);
    }

    private void sendOnboardingNotification(NurseProfile profile, String title, String body) {
        try {
            String nurseMobile = profile.getUser().getMobile();
            fcmService.sendNotification(nurseMobile, title, body,
                    Map.of("type", "ONBOARDING", "nurseProfileId", String.valueOf(profile.getId())));
        } catch (Exception e) {
            // Notifications are non-critical; log but do not fail the transaction
            log.warn("Failed to send onboarding FCM notification to nurse profile ID {}: {}",
                    profile.getId(), e.getMessage());
        }
    }

    private OnboardingStatusResponse buildOnboardingStatusResponse(NurseProfile profile) {
        List<NurseDocument> documents = nurseDocumentRepository
                .findByNurseProfileIdOrderByUploadedAtDesc(profile.getId());

        List<NurseDocumentResponse> docResponses = documents.stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());

        long pendingCount  = documents.stream().filter(d -> d.getDocumentStatus() == DocumentStatus.PENDING).count();
        long approvedCount = documents.stream().filter(d -> d.getDocumentStatus() == DocumentStatus.APPROVED).count();
        long rejectedCount = documents.stream().filter(d -> d.getDocumentStatus() == DocumentStatus.REJECTED).count();

        return OnboardingStatusResponse.builder()
                .nurseProfileId(profile.getId())
                .nurseId(profile.getNurseId())
                .name(profile.getUser().getName())
                .mobile(profile.getUser().getMobile())
                .onboardingStatus(profile.getOnboardingStatus())
                .isVerified(profile.isVerified())
                .documents(docResponses)
                .pendingCount(pendingCount)
                .approvedCount(approvedCount)
                .rejectedCount(rejectedCount)
                .totalDocumentCount(documents.size())
                .lastUpdatedAt(profile.getUpdatedAt())
                .build();
    }

    private NurseDocumentResponse toDocumentResponse(NurseDocument doc) {
        return NurseDocumentResponse.builder()
                .id(doc.getId())
                .nurseProfileId(doc.getNurseProfile().getId())
                .documentType(doc.getDocumentType())
                .documentTypeName(doc.getDocumentType().getReadableName())
                .documentStatus(doc.getDocumentStatus())
                .originalName(doc.getOriginalName())
                .fileName(doc.getFileName())
                .fileSizeBytes(doc.getFileSizeBytes())
                .mimeType(doc.getMimeType())
                .version(doc.getVersion())
                .reviewComment(doc.getReviewComment())
                .reviewedBy(doc.getReviewedBy())
                .reviewedAt(doc.getReviewedAt())
                .uploadedAt(doc.getUploadedAt())
                .downloadUrl("/api/nurse/onboarding/documents/" + doc.getId() + "/view")
                .build();
    }
}

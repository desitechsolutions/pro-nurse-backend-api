package com.pronurse.admin.service;

import com.pronurse.admin.dto.*;
import com.pronurse.admin.repository.AdminAnalyticsRepository;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.repository.BookingRepository;
import com.pronurse.catalog.entity.MedicalService;
import com.pronurse.catalog.repository.MedicalServiceRepository;
import com.pronurse.common.exception.ApplicationException;
import com.pronurse.nurse.entity.NurseProfile;
import com.pronurse.nurse.repository.NurseProfileRepository;
import com.pronurse.review.repository.NurseReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final AdminAnalyticsRepository analyticsRepository;
    private final NurseProfileRepository nurseProfileRepository;
    private final BookingRepository bookingRepository;
    private final MedicalServiceRepository medicalServiceRepository;
    private final NurseReviewRepository reviewRepository;
    private final UserRepository userRepository;

    // --- FIXED: Added missing implementation method to support /api/admin/me dashboard hooks ---
    @Override
    @Transactional(readOnly = true)
    public Object getAdminProfileDetails(String mobile) {
        var user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ApplicationException("Admin account record missing for identity context tracking: " + mobile));

        return Map.of(
                "id", user.getId(),
                "name", user.getName() != null ? user.getName() : "System Administrator",
                "mobile", user.getMobile(),
                "email", user.getEmail() != null ? user.getEmail() : "",
                "role", user.getRole(),
                "isActive", user.isActive(),
                "createdAt", user.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardMetrics getDashboardMetrics() {
        long generalPatientsCount = analyticsRepository.countTotalPatients();
        long generalNursesCount = analyticsRepository.countTotalNurses();
        long registrationPendingCount = analyticsRepository.countPendingNurseApprovals();
        long cumulativeBookingsCount = bookingRepository.count();
        long activeLiveCount = analyticsRepository.countLiveActiveBookings();
        BigDecimal revenueTotal = analyticsRepository.calculateGrossRevenue();

        if (revenueTotal == null) {
            revenueTotal = BigDecimal.ZERO;
        }

        return AdminDashboardMetrics.builder()
                .totalPatientsCount(generalPatientsCount)
                .totalNursesCount(generalNursesCount)
                .pendingApprovalsCount(registrationPendingCount)
                .totalBookingsCount(cumulativeBookingsCount)
                .activeLiveBookingsCount(activeLiveCount)
                .totalGrossRevenue(revenueTotal)
                .build();
    }

    @Override
    @Transactional
    public void verifyNurseProfile(String adminUsername, VerifyNurseRequest request) {
        NurseProfile profile = nurseProfileRepository.findById(request.getNurseProfileId())
                .orElseThrow(() -> new ApplicationException("Nurse profile not found for registration ID verification."));

        if (!"Pending".equalsIgnoreCase(profile.getVerificationStatus())) {
            throw new ApplicationException("Validation collision: Profile evaluation state has already been processed.");
        }

        if ("Approved".equalsIgnoreCase(request.getStatus())) {
            profile.setVerified(true);
            profile.setVerificationStatus("Approved");
            profile.setOnDuty(true); // Put them into the active dispatch matching pool right away
        } else if ("Rejected".equalsIgnoreCase(request.getStatus())) {
            profile.setVerified(false);
            profile.setVerificationStatus("Rejected");
            profile.setRejectReason(request.getRejectReason());
        } else {
            throw new ApplicationException("Malformed request constraint value: Target status action unrecognized.");
        }

        profile.setVerifiedBy(adminUsername);
        profile.setVerifiedDate(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());

        nurseProfileRepository.save(profile);
        log.info("Nurse profile ID {} evaluation closed as: [{}] by Admin: {}", profile.getId(), request.getStatus(), adminUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminNurseSummaryResponse> getAllNurseProfiles(String status, Pageable pageable) {
        Page<NurseProfile> entities = (status != null && !status.isBlank())
                ? nurseProfileRepository.findByVerificationStatus(status, pageable)
                : nurseProfileRepository.findAll(pageable);

        return entities.map(profile -> AdminNurseSummaryResponse.builder()
                .id(profile.getId())
                .nurseId(profile.getNurseId())
                .name(profile.getUser().getName())
                .mobile(profile.getUser().getMobile())
                .email(profile.getUser().getEmail())
                .qualification(profile.getQualification())
                .specialization(profile.getSpecialization())
                .registrationNumber(profile.getRegistrationNumber())
                .isVerified(profile.isVerified())
                .isOnDuty(profile.isOnDuty())
                .verificationStatus(profile.getVerificationStatus())
                .averageRating(profile.getAverageRating())
                .createdAt(profile.getCreatedAt())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminBookingSummaryResponse> getAllBookingsHistoryView(String status, Pageable pageable) {
        Page<Booking> entities = (status != null && !status.isBlank())
                ? bookingRepository.findByBookingStatus(status, pageable)
                : bookingRepository.findAll(pageable);

        return entities.map(b -> {
            BigDecimal grandTotal = b.getSelectedItems().stream()
                    .map(com.pronurse.booking.entity.BookingItem::getPriceCharged)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String nurseName = (b.getAssignedNurseUser() != null) ? b.getAssignedNurseUser().getName() : "UNASSIGNED / CHAIN DISPATCHING";
            String nurseMobile = (b.getAssignedNurseUser() != null) ? b.getAssignedNurseUser().getMobile() : "N/A";

            return AdminBookingSummaryResponse.builder()
                    .id(b.getId())
                    .bookingNo(b.getBookingNo())
                    .patientName(b.getPatientUser().getName())
                    .patientMobile(b.getPatientUser().getMobile())
                    .assignedNurseName(nurseName)
                    .assignedNurseMobile(nurseMobile)
                    .bookingDate(b.getBookingDate())
                    .bookingTime(b.getBookingTime())
                    .bookingStatus(b.getBookingStatus())
                    .paymentStatus(b.getPaymentStatus())
                    .totalAmount(grandTotal)
                    .build();
        });
    }

    @Override
    @Transactional
    public void createOrUpdateMedicalService(Long serviceId, UpdateServiceRequest request) {

        MedicalService targetNode;

        if (serviceId != null && serviceId > 0) {
            targetNode = medicalServiceRepository.findById(serviceId)
                    .orElseThrow(() -> new ApplicationException(
                            "Target healthcare catalog service item not located for ID: " + serviceId));

            // Prevent self-parenting
            if (request.getParentServiceId() != null &&
                    serviceId.equals(request.getParentServiceId())) {
                throw new ApplicationException("Service cannot be assigned as its own parent.");
            }

        } else {
            targetNode = new MedicalService();
        }

        boolean isSubService = request.getParentServiceId() != null;

        // Sub-services must have pricing and duration
        if (isSubService) {

            if (request.getBasePrice() == null) {
                throw new ApplicationException("Sub-service price is required.");
            }

            if (request.getEstimatedDurationMinutes() == null) {
                throw new ApplicationException("Sub-service duration is required.");
            }
        }

        targetNode.setName(request.getName());
        targetNode.setDescription(request.getDescription());
        targetNode.setBasePrice(request.getBasePrice());
        targetNode.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        targetNode.setIsActive(request.getIsActive());

        // Parent Service Mapping
        if (isSubService) {
            MedicalService parentService = medicalServiceRepository.findById(request.getParentServiceId())
                    .orElseThrow(() -> new ApplicationException(
                            "Parent service not found for ID: " + request.getParentServiceId()));

            targetNode.setParentService(parentService);
        } else {
            targetNode.setParentService(null);
        }

        medicalServiceRepository.save(targetNode);

        log.info(
                "Medical catalog modification executed successfully. Service: [{}], Parent Service ID: [{}]",
                request.getName(),
                request.getParentServiceId()
        );
    }

    @Override
    @Transactional
    public void moderatePatientReview(ReviewModerationRequest request) {

        var targetedReview = reviewRepository.findById(request.getReviewId())
                .orElseThrow(() -> new ApplicationException(
                        "Target feedback post entry index not found for ID: " + request.getReviewId()));

        if ("DELETE".equalsIgnoreCase(request.getAction())) {

            reviewRepository.delete(targetedReview);

            log.warn(
                    "Admin hard-purged review post index [{}] from database cluster records.",
                    request.getReviewId());

        } else if ("APPROVE".equalsIgnoreCase(request.getAction())) {

            log.info(
                    "Admin approved review comment block [{}] successfully.",
                    request.getReviewId());

        } else {

            throw new ApplicationException(
                    "Invalid moderation action. Allowed values are APPROVE or DELETE.");
        }
    }

    @Override
    @Transactional
    public void toggleUserAccountStatus(Long userId, boolean enableAccount) {
        var individualUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException("System user record not matched for profile index: " + userId));

        individualUser.setActive(enableAccount);
        userRepository.save(individualUser);
        log.info("Administrative security profile alteration: Set account active flag to [{}] for user: {}", enableAccount, individualUser.getMobile());
    }

    @Override
    @Transactional
    public void deleteMedicalService(Long serviceId) {

        MedicalService service = medicalServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ApplicationException(
                        "Medical service not found."));

        if (!service.getSubServices().isEmpty()) {
            throw new ApplicationException(
                    "Cannot delete service having sub-services.");
        }

        medicalServiceRepository.delete(service);
    }
}
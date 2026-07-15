package com.pronurse.admin.service;

import com.pronurse.admin.dto.AdminDashboardMetrics;
import com.pronurse.admin.dto.VerifyNurseRequest;
import com.pronurse.booking.entity.Booking;
import com.pronurse.nurse.entity.NurseProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {
    AdminDashboardMetrics getDashboardMetrics();
    void verifyNurseProfile(String adminUsername, VerifyNurseRequest request);
    Page<com.pronurse.admin.dto.AdminNurseSummaryResponse> getAllNurseProfiles(String status, Pageable pageable);
    Page<com.pronurse.admin.dto.AdminBookingSummaryResponse> getAllBookingsHistoryView(String status, Pageable pageable);

    void createOrUpdateMedicalService(Long serviceId, com.pronurse.admin.dto.UpdateServiceRequest request);
    void moderatePatientReview(com.pronurse.admin.dto.ReviewModerationRequest request);
    void toggleUserAccountStatus(Long userId, boolean enableAccount);
    Object getAdminProfileDetails(String mobile);
    void deleteMedicalService(Long serviceId);
}
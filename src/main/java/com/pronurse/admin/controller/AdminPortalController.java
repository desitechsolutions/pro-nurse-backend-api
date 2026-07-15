package com.pronurse.admin.controller;

import com.pronurse.admin.dto.AdminDashboardMetrics;
import com.pronurse.admin.dto.ReviewModerationRequest;
import com.pronurse.admin.dto.UpdateServiceRequest;
import com.pronurse.admin.dto.VerifyNurseRequest;
import com.pronurse.admin.dto.AdminNurseSummaryResponse;
import com.pronurse.admin.dto.AdminBookingSummaryResponse;
import com.pronurse.admin.service.AdminService;
import com.pronurse.catalog.dto.ServiceCatalogResponse;
import com.pronurse.catalog.service.CatalogService;
import com.pronurse.common.payload.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "13. Admin Portal",
        description = "Administrative dashboard and management APIs for system oversight, user moderation, and service catalog control")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPortalController {

    private final AdminService adminService;
    private final CatalogService catalogService;

    /**
     * Web Dashboard Profile Hook: Pull administrative user details for session persistence views
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Object>> getAdminProfile(Authentication authentication) {
        String identityContext = (String) authentication.getPrincipal();
        Object adminProfile = adminService.getAdminProfileDetails(identityContext);
        return ResponseEntity.ok(new ApiResponse<>(
                true, "Administrative session profile data mapped successfully.", adminProfile
        ));
    }

    /**
     * Web Dashboard: Fetch operational live system KPI summaries and stats
     */
    @GetMapping("/analytics/metrics")
    public ResponseEntity<ApiResponse<AdminDashboardMetrics>> getSystemMetrics() {
        return ResponseEntity.ok(new ApiResponse<>(
                true, "Aggregated KPIs extracted successfully", adminService.getDashboardMetrics()
        ));
    }

    /**
     * Nurse Approvals: Approve or Reject onboarding credentials
     */
    @PostMapping("/nurse/verify")
    public ResponseEntity<ApiResponse<Void>> executeVerification(
            @Valid @RequestBody VerifyNurseRequest request, Authentication authentication) {

        String adminUsername = authentication.getName();
        adminService.verifyNurseProfile(adminUsername, request);
        return ResponseEntity.ok(new ApiResponse<>(
                true, "Practitioner verification status successfully committed.", null
        ));
    }

    /**
     * Practitioner Ledger: Paginated tracking view across profiles
     */
    @GetMapping("/nurses")
    public ResponseEntity<ApiResponse<Page<AdminNurseSummaryResponse>>> getPractitioners(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(
                true, "Practitioner listing chunk compiled", adminService.getAllNurseProfiles(status, pageable)
        ));
    }

    /**
     * Operations Logs: View all platform order transaction histories.
     */
    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<Page<AdminBookingSummaryResponse>>> getHistoricalLedger(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(
                true, "Platform transaction booking history synced", adminService.getAllBookingsHistoryView(status, pageable)
        ));
    }

    /**
     * Service Management: Create or update available medical procedures and pricing
     */
    @PostMapping("/services/save")
    public ResponseEntity<ApiResponse<Void>> commitServiceModification(
            @RequestParam(required = false) Long serviceId,
            @Valid @RequestBody UpdateServiceRequest request) {

        adminService.createOrUpdateMedicalService(serviceId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Medical catalog matrix modified successfully.", null));
    }

    /**
     * Review Moderation: Filter or remove inappropriate feedback comments
     */
    @PostMapping("/reviews/moderate")
    public ResponseEntity<ApiResponse<Void>> balanceCustomerFeedback(
            @Valid @RequestBody ReviewModerationRequest request) {

        adminService.moderatePatientReview(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Moderation action committed successfully.", null));
    }

    /**
     * Patient & Nurse Management: Freeze or activate user access credentials instantly
     */
    @PostMapping("/users/status-toggle")
    public ResponseEntity<ApiResponse<Void>> administrativeLockoutControl(
            @RequestParam Long userId,
            @RequestParam boolean enableAccount) {

        adminService.toggleUserAccountStatus(userId, enableAccount);
        return ResponseEntity.ok(new ApiResponse<>(true, "User authorization state updated cleanly.", null));
    }

    @GetMapping("/services/tree")
    public ResponseEntity<ApiResponse<List<ServiceCatalogResponse>>> getServiceHierarchy() {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Medical service hierarchy fetched successfully.",
                        catalogService.getActiveHierarchyTree()
                )
        );
    }

    @DeleteMapping("/services/{serviceId}")
    public ResponseEntity<ApiResponse<Void>> deleteService(
            @PathVariable Long serviceId) {

        adminService.deleteMedicalService(serviceId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Medical service deleted successfully.",
                        null
                )
        );
    }
}
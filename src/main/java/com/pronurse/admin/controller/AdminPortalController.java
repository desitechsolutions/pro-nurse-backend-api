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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class AdminPortalController {

    private final AdminService adminService;
    private final CatalogService catalogService;

    /**
     * Web Dashboard Profile Hook: Pull administrative user details for session persistence views
     */
    @Operation(
            summary = "Get admin profile",
            description = "Pull administrative user details for session persistence views.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Administrative session profile data mapped successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access - invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Admin profile not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error occurred", content = @Content)
    })
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
    @Operation(
            summary = "Get system metrics",
            description = "Fetch operational live system KPI summaries and stats for the admin dashboard.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Aggregated KPIs extracted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access - invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error occurred", content = @Content)
    })
    @GetMapping("/analytics/metrics")
    public ResponseEntity<ApiResponse<AdminDashboardMetrics>> getSystemMetrics() {
        return ResponseEntity.ok(new ApiResponse<>(
                true, "Aggregated KPIs extracted successfully", adminService.getDashboardMetrics()
        ));
    }

    /**
     * Nurse Approvals: Approve or Reject onboarding credentials
     */
    @Operation(
            summary = "Execute nurse verification",
            description = "Approve or Reject onboarding credentials for a practitioner.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Practitioner verification status successfully committed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request attributes", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access - invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Practitioner profile not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error occurred", content = @Content)
    })
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
    @Operation(
            summary = "Get practitioners ledger",
            description = "Retrieve a paginated tracking view across practitioner profiles, optionally filtered by status.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Practitioner listing chunk compiled", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access - invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error occurred", content = @Content)
    })
    @GetMapping("/nurses")
    public ResponseEntity<ApiResponse<Page<AdminNurseSummaryResponse>>> getPractitioners(
            @Parameter(description = "Verification status filter (e.g. Pending, Approved, Rejected)") @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(
                true, "Practitioner listing chunk compiled", adminService.getAllNurseProfiles(status, pageable)
        ));
    }

    /**
     * Operations Logs: View all platform order transaction histories.
     */
    @Operation(
            summary = "Get historical bookings ledger",
            description = "Retrieve all platform booking transaction histories, paginated and optionally filtered by status.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Platform transaction booking history synced", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access - invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error occurred", content = @Content)
    })
    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<Page<AdminBookingSummaryResponse>>> getHistoricalLedger(
            @Parameter(description = "Booking status filter (e.g. CONFIRMED, COMPLETED, CANCELLED)") @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(
                true, "Platform transaction booking history synced", adminService.getAllBookingsHistoryView(status, pageable)
        ));
    }

    /**
     * Service Management: Create or update available medical procedures and pricing
     */
    @Operation(
            summary = "Commit medical service modification",
            description = "Create a new medical service or update an existing medical procedure and its pricing catalog configuration.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Medical catalog matrix modified successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload format", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access - invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error occurred", content = @Content)
    })
    @PostMapping("/services/save")
    public ResponseEntity<ApiResponse<Void>> commitServiceModification(
            @Parameter(description = "Optional ID of the existing medical service to update") @RequestParam(required = false) Long serviceId,
            @Valid @RequestBody UpdateServiceRequest request) {

        adminService.createOrUpdateMedicalService(serviceId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Medical catalog matrix modified successfully.", null));
    }

    /**
     * Review Moderation: Filter or remove inappropriate feedback comments
     */
    @Operation(
            summary = "Moderate review",
            description = "Filter, approve, or delete patient review feedback comments based on platform policies.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Moderation action committed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access - invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error occurred", content = @Content)
    })
    @PostMapping("/reviews/moderate")
    public ResponseEntity<ApiResponse<Void>> balanceCustomerFeedback(
            @Valid @RequestBody ReviewModerationRequest request) {

        adminService.moderatePatientReview(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Moderation action committed successfully.", null));
    }

    /**
     * Patient & Nurse Management: Freeze or activate user access credentials instantly
     */
    @Operation(
            summary = "Administrative account lockout control",
            description = "Freeze, disable, or instantly reactivate user (patient/nurse) access credentials.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User authorization state updated cleanly", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access - invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User profile context not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error occurred", content = @Content)
    })
    @PostMapping("/users/status-toggle")
    public ResponseEntity<ApiResponse<Void>> administrativeLockoutControl(
            @Parameter(description = "ID of the target user to manage status") @RequestParam Long userId,
            @Parameter(description = "Boolean value to enable (true) or disable/lockout (false) the user account") @RequestParam boolean enableAccount) {

        adminService.toggleUserAccountStatus(userId, enableAccount);
        return ResponseEntity.ok(new ApiResponse<>(true, "User authorization state updated cleanly.", null));
    }

    @Operation(
            summary = "Get active services tree",
            description = "Retrieve hierarchical structure of all active medical procedures/services categorized.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Medical service hierarchy fetched successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access - invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error occurred", content = @Content)
    })
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

    @Operation(
            summary = "Delete medical service",
            description = "Permanently remove a medical service procedure configuration from the catalog by ID.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Medical service deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access - invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Medical service procedure not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error occurred", content = @Content)
    })
    @DeleteMapping("/services/{serviceId}")
    public ResponseEntity<ApiResponse<Void>> deleteService(
            @Parameter(description = "ID of the medical service to delete") @PathVariable Long serviceId) {

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
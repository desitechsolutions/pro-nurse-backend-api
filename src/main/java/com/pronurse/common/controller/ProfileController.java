package com.pronurse.common.controller;

import com.pronurse.nurse.dto.NurseProfileUpdateRequest;
import com.pronurse.patient.dto.PatientProfileUpdateRequest;
import com.pronurse.common.payload.ApiResponse;
import com.pronurse.nurse.service.NurseProfileService;
import com.pronurse.patient.service.PatientProfileService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    private final NurseProfileService nurseProfileService;
    private final PatientProfileService patientProfileService;

    public ProfileController(NurseProfileService nurseProfileService, PatientProfileService patientProfileService) {
        this.nurseProfileService = nurseProfileService;
        this.patientProfileService = patientProfileService;
    }

    /**
     * Authenticated endpoint for updating detailed nurse profile tracks
     */
    @PutMapping(value = "/nurse/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<ApiResponse<String>> updateNurseProfile(
            @Valid @RequestPart("data") NurseProfileUpdateRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        logger.info("Intercepted inbound request to process nurse profile records mapping for: {}", mobile);

        nurseProfileService.updateProfile(mobile, request, profileImage);
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile updated successfully", null));
    }

    /**
     * Authenticated endpoint for updating baseline patient profile tracks
     */
    @PutMapping(value = "/patient/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<String>> updatePatientProfile(
            @Valid @RequestPart("data") PatientProfileUpdateRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        logger.info("Intercepted inbound request to process patient profile records mapping for: {}", mobile);

        patientProfileService.updateProfile(mobile, request, profileImage);
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile updated successfully", null));
    }

    /**
     * Authenticated endpoint to fetch the current user's profile details dynamically based on role
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('PATIENT', 'NURSE', 'ADMIN')") // <-- Added 'ADMIN' here
    public ResponseEntity<ApiResponse<Object>> getMyProfile(Authentication authentication) {
        String mobile = (String) authentication.getPrincipal();
        logger.info("Fetching profile data track for authenticated identity: {}", mobile);

        // Extract role flags from authorities
        boolean isNurse = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_NURSE"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Object profileDetails;
        if (isAdmin) {
            // Fetch direct administrative user tracking details
            profileDetails = nurseProfileService.getAdminProfileByMobile(mobile);
        } else if (isNurse) {
            profileDetails = nurseProfileService.getProfileByMobile(mobile);
        } else {
            profileDetails = patientProfileService.getProfileByMobile(mobile);
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "Profile details retrieved successfully", profileDetails));
    }
}
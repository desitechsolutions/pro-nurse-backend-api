package com.pronurse.patient.controller;

import com.pronurse.patient.dto.PatientProfileUpdateRequest;
import com.pronurse.patient.dto.PatientProfileResponse;
import com.pronurse.common.payload.ApiResponse;
import com.pronurse.patient.service.PatientProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/patient/profile")
@Tag(name = "02. Patient Profile",
        description = "Patient profile management APIs")
@RequiredArgsConstructor
public class PatientProfileController {

    private final PatientProfileService patientProfileService;

    /**
     * Flutter App Hook: Patient Profile Update Operations
     */
    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Void>> syncPatientProfile(
            @Valid @RequestPart("data") PatientProfileUpdateRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        patientProfileService.updateProfile(mobile, request, profileImage);
        return ResponseEntity.ok(new ApiResponse<>(true, "Patient profile records updated successfully", null));
    }

    /**
     * Flutter App Hook: Pull Profile Details for Active Session Profile Views
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<PatientProfileResponse>> getMyProfile(Authentication authentication) {
        String mobile = (String) authentication.getPrincipal();
        PatientProfileResponse dataCard = patientProfileService.getProfileByMobile(mobile);
        return ResponseEntity.ok(new ApiResponse<>(true, "Patient profile records retrieved.", dataCard));
    }
}
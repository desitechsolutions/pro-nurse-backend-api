package com.pronurse.nurse.controller;

import com.pronurse.common.payload.ApiResponse;
import com.pronurse.nurse.dto.NurseProfileUpdateRequest;
import com.pronurse.nurse.dto.NurseProfileResponse;
import com.pronurse.nurse.service.NurseProfileService;
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
@RequestMapping("/api/nurse/profile")
@Tag(
        name = "03. Nurse Profile",
        description = "Nurse profile management APIs"
)
@RequiredArgsConstructor
public class NurseProfileController {

    private final NurseProfileService nurseProfileService;

    /**
     * Flutter App Hook: Nurse Profile Update Endpoint
     */
    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<ApiResponse<Void>> syncNurseProfile(
            @Valid @RequestPart("data") NurseProfileUpdateRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            Authentication authentication) {

        String mobile = (String) authentication.getPrincipal();
        nurseProfileService.updateProfile(mobile, request, profileImage);
        return ResponseEntity.ok(new ApiResponse<>(true, "Nurse profile records updated successfully", null));
    }

    /**
     * Flutter App Hook: Pull Profile Details for Active Session Profile Views
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<ApiResponse<NurseProfileResponse>> getMyProfile(Authentication authentication) {
        String mobile = (String) authentication.getPrincipal();
        NurseProfileResponse dataCard = nurseProfileService.getProfileByMobile(mobile);
        return ResponseEntity.ok(new ApiResponse<>(true, "Nurse profile metrics synchronized.", dataCard));
    }
}
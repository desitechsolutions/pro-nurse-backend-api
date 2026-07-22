package com.pronurse.nurse.controller;

import com.pronurse.common.payload.ApiResponse;
import com.pronurse.nurse.dto.NurseProfileUpdateRequest;
import com.pronurse.nurse.dto.NurseProfileResponse;
import com.pronurse.nurse.service.NurseProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class NurseProfileController {

    private final NurseProfileService nurseProfileService;

    /**
     * Flutter App Hook: Nurse Profile Update Endpoint
     */
    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('NURSE')")
    @Operation(summary = "Update nurse profile", description = "Updates the nurse's profile details and optional profile image.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Nurse profile records updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden access"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
    @Operation(summary = "Get current nurse profile", description = "Retrieves profile details and metrics for the currently authenticated nurse.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Nurse profile metrics synchronized.",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden access"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Nurse profile not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<NurseProfileResponse>> getMyProfile(Authentication authentication) {
        String mobile = (String) authentication.getPrincipal();
        NurseProfileResponse dataCard = nurseProfileService.getProfileByMobile(mobile);
        return ResponseEntity.ok(new ApiResponse<>(true, "Nurse profile metrics synchronized.", dataCard));
    }
}
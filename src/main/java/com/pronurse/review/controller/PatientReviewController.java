package com.pronurse.review.controller;

import com.pronurse.common.payload.ApiResponse;
import com.pronurse.review.dto.AddReviewRequest;
import com.pronurse.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/review")
@Tag(name = "14. Patient Reviews", description = "Submit service feedback reviews post execution tracking")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class PatientReviewController {

    private final ReviewService reviewService;

    /**
     * Mobile Workflow Step: Submit service feedback reviews post execution tracking
     */
    @PostMapping("/add")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Add nurse review", description = "Submit review and rating for a nurse post booking completion.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Patient evaluation review compiled and submitted successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden access"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Void>> addNurseReview(
            @Valid @RequestBody AddReviewRequest request,
            Authentication authentication) {

        String patientMobile = (String) authentication.getPrincipal();
        reviewService.submitNurseReview(patientMobile, request);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Patient evaluation review compiled and submitted successfully.",
                null
        ));
    }
}
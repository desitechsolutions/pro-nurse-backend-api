package com.pronurse.review.controller;

import com.pronurse.common.payload.ApiResponse;
import com.pronurse.review.dto.AddReviewRequest;
import com.pronurse.review.service.ReviewService;
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
@RequiredArgsConstructor
public class PatientReviewController {

    private final ReviewService reviewService;

    /**
     * Mobile Workflow Step: Submit service feedback reviews post execution tracking
     */
    @PostMapping("/add")
    @PreAuthorize("hasRole('PATIENT')")
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
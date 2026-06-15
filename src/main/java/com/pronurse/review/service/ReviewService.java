package com.pronurse.review.service;

import com.pronurse.review.dto.AddReviewRequest;

public interface ReviewService {
    /**
     * Confirms the ownership and completion status of a booking,
     * logs patient feedback, and updates the nurse's average score.
     *
     * @param patientMobile Authenticated mobile identity context of the patient reviewer.
     * @param request Input DTO holding star score evaluations and text feedback.
     */
    void submitNurseReview(String patientMobile, AddReviewRequest request);
}
package com.pronurse.review.service;

import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.repository.BookingRepository;
import com.pronurse.common.exception.ApplicationException;
import com.pronurse.nurse.repository.NurseProfileRepository;
import com.pronurse.review.dto.AddReviewRequest;
import com.pronurse.review.entity.NurseReview;
import com.pronurse.review.repository.NurseReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final NurseReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final NurseProfileRepository nurseProfileRepository;

    @Override
    @Transactional
    public void submitNurseReview(String patientMobile, AddReviewRequest request) {
        // 1. Locate target service booking order record
        Booking booking = bookingRepository.findByBookingNo(request.getBookingId())
                .orElseThrow(() -> new ApplicationException("Booking not found with index ID: " + request.getBookingId()));

        // 2. Guard: Ensure the user submitting the review is the patient who booked the service
        if (!booking.getPatientUser().getMobile().equals(patientMobile)) {
            throw new ApplicationException("Security validation exception: Unauthorized user review submission attempt.");
        }

        // 3. Guard: Ensure reviews are only submitted for completed bookings
        if (!"COMPLETED".equalsIgnoreCase(booking.getBookingStatus())) {
            throw new ApplicationException("Validation Constraint: Reviews can only be left for COMPLETED medical items.");
        }

        // 4. Guard: Prevent duplicate review spamming for a single booking instance
        if (reviewRepository.existsByBookingBookingNo(request.getBookingId())) {
            throw new ApplicationException("Collision error: Feedback has already been recorded for this specific booking ticket.");
        }

        if (booking.getAssignedNurseUser() == null) {
            throw new ApplicationException("System integrity fault: No clinical provider assigned to historical reference record.");
        }

        // 5. Build and save the review log entry
        NurseReview reviewLog = NurseReview.builder()
                .booking(booking)
                .patientUser(booking.getPatientUser())
                .nurseUser(booking.getAssignedNurseUser())
                .ratingScore(request.getRating())
                .reviewText(request.getReview())
                .createdAt(LocalDateTime.now())
                .build();

        reviewRepository.save(reviewLog);

        // 6. Automatically update the nurse's average rating in the database cache layer
        nurseProfileRepository.recalculateAndCacheNurseRating(
                booking.getAssignedNurseUser().getId(),
                request.getRating().doubleValue()
        );

        log.info("Feedback loop saved for booking ticket: [{}]. Nurse User identity tracking score updated.", booking.getBookingNo());
    }
}
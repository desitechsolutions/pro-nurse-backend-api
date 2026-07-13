package com.pronurse.review.repository;

import com.pronurse.review.entity.NurseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NurseReviewRepository extends JpaRepository<NurseReview, Long> {
    boolean existsByBookingBookingNo(String bookingNo);
}
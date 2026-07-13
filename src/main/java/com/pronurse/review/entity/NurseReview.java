package com.pronurse.review.entity;

import com.pronurse.booking.entity.Booking;
import com.pronurse.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "nurse_reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_user_id", nullable = false)
    private User patientUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nurse_user_id", nullable = false)
    private User nurseUser;

    @Column(name = "rating_score", nullable = false)
    private Integer ratingScore;

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
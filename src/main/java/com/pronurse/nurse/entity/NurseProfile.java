package com.pronurse.nurse.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pronurse.auth.entity.User;
import com.pronurse.enums.Gender;
import com.pronurse.onboarding.enums.OnboardingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "nurse_profiles", indexes = {
        @Index(name = "idx_nurse_spatial_coords", columnList = "latitude, longitude"),
        @Index(name = "idx_nurse_user_id", columnList = "user_id")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nurse_id", unique = true, nullable = false, length = 50)
    private String nurseId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(length = 20)
    private LocalDate dob;

    private String address;
    private String qualification;
    private String experience;
    private String specialization;

    @Column(name = "languages_spoken")
    private String languages;

    private Double latitude;
    private Double longitude;

    @Column(name = "profile_image_path")
    private String profileImage;

    @Column(unique = true, length = 100)
    private String registrationNumber;

    private String city;

    @Column(name = "consultation_fee")
    @Builder.Default
    private BigDecimal consultationFee = BigDecimal.ZERO;

    private boolean isVerified = false;
    private boolean isOnDuty = false;

    @Column(length = 30)
    @Builder.Default
    private String verificationStatus = "Pending";

    /**
     * Structured onboarding lifecycle status backed by an enum.
     * Drives the document upload and review state machine.
     *
     * NOTE: This coexists with {@code verificationStatus} (plain String) for backward
     * compatibility. The service layer syncs both fields only at ONBOARDING_COMPLETED:
     *   verificationStatus = "Approved", isVerified = true, isOnDuty = true.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", length = 50)
    @Builder.Default
    private OnboardingStatus onboardingStatus = OnboardingStatus.DRAFT;

    private String rejectReason;
    private String verifiedBy;
    private LocalDateTime verifiedDate;

    // --- FIXED: Rating Cache Tracks for Repository Query Computations ---
    @Column(name = "average_rating")
    @Builder.Default
    private double averageRating = 0.0;

    @Column(name = "total_reviews_count")
    @Builder.Default
    private int totalReviewsCount = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
package com.pronurse.nurse.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pronurse.auth.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "nurse_profiles")
public class NurseProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nurse_id", unique = true, nullable = false)
    private String nurseId; // e.g., "NUR001"

    // Maps directly back to the core security credential entity
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    private String gender;
    private String dob;
    private String address;
    private String qualification; // e.g., "GNM", "B.Sc Nursing"
    private String experience;    // e.g., "5 Years"
    private String specialization; // e.g., "Post Surgery Care", "ICU"

    @Column(name = "languages_spoken")
    private String languages;

    // Spatial coordinates for shift routing metrics
    private Double latitude;
    private Double longitude;

    @Column(name = "profile_image_path")
    private String profileImage;

    @Column(unique = true)
    private String registrationNumber;

    // Administrative operational metrics
    private boolean isVerified = false;
    private boolean isOnDuty = false;

    @Column
    private String verificationStatus = "Pending"; // Pending, Approved, Rejected

    private String rejectReason;
    private String verifiedBy;
    private LocalDateTime verifiedDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
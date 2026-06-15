package com.pronurse.patient.entity;

import com.pronurse.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_profiles", indexes = {
        // Optimizes spatial coordinate calculations for nearby nurse tracking
        @Index(name = "idx_patient_spatial_coords", columnList = "latitude, longitude"),
        @Index(name = "idx_patient_user_id", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", unique = true, nullable = false, length = 50)
    private String patientId; // e.g., "PAT001"

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude          // Prevents circular dependency StackOverflow errors in logging
    @EqualsAndHashCode.Exclude // Prevents circular reference loops in collections
    private User user;

    @Column(length = 20)
    private String gender;

    @Column(length = 20)
    private String dob;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    private String address;

    @Column(name = "profile_image_path", length = 255)
    private String profileImage;

    @Column(name = "chronic_diseases", columnDefinition = "TEXT")
    private String chronicDiseases;

    @Column(name = "medical_report_path", length = 255)
    private String medicalReportPath;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private Double latitude;
    private Double longitude;

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
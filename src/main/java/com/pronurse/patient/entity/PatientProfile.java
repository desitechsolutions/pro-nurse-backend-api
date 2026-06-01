package com.pronurse.patient.entity;

import com.pronurse.auth.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "patient_profiles")
public class PatientProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", unique = true, nullable = false)
    private String patientId; // e.g., "PAT001"

    // Maps directly back to the core security credential entity
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    private String gender;
    private String dob;

    @Column(name = "blood_group")
    private String bloodGroup;

    private String address;

    @Column(name = "profile_image_path")
    private String profileImage;

    // Placeholder entries for baseline medical check records
    private String chronicDiseases;
    private String medicalReportPath;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    private Double latitude;
    private Double longitude;
}
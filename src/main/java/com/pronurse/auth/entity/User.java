package com.pronurse.auth.entity;

import com.pronurse.auth.model.Role;
import com.pronurse.nurse.entity.NurseProfile; // Ensure this import is added
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "mobile"),
        @UniqueConstraint(columnNames = "email")
})
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 15)
    private String mobile;

    private String email;
    private String name;
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private boolean active = true;
    private boolean isMobileVerified = false;

    // --- NEW: Bidirectional link to fetch nurse tracking details dynamically ---
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude // Prevents infinite recursion loop during logging
    @EqualsAndHashCode.Exclude // Prevents infinite recursion loop in Lombok equals checks
    private NurseProfile nurseProfile;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
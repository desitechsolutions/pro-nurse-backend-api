package com.pronurse.notification.entity;

import com.pronurse.auth.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "booking_alerts")
    private Boolean bookingAlerts = true;

    @Column(name = "payment_alerts")
    private Boolean paymentAlerts = true;

    @Column(name = "review_alerts")
    private Boolean reviewAlerts = true;

    @Column(name = "promotional_alerts")
    private Boolean promotionalAlerts = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

// Made with Bob

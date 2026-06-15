package com.pronurse.booking.entity;

import com.pronurse.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nurse_user_id", nullable = false)
    private User nurseUser;

    @Column(nullable = false, length = 20)
    private String status; // 'RINGING', 'ACCEPTED', 'REJECTED', 'TIMEOUT'

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
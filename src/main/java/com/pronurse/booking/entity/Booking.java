package com.pronurse.booking.entity;

import com.pronurse.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_no", unique = true, nullable = false, length = 50)
    private String bookingNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User patientUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_nurse_user_id")
    private User assignedNurseUser;

    @Column(name = "booking_status", length = 30)
    private String bookingStatus;

    @Column(name = "payment_status", length = 30)
    private String paymentStatus;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "booking_time", nullable = false, length = 20)
    private String bookingTime;

    private String remarks;
    private Double latitude;
    private Double longitude;

    @Column(name = "raw_address", nullable = false)
    private String rawAddress;

    // --- Razorpay Digital Payment Gateway Integration Traces ---
    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature", length = 255)
    private String razorpaySignature;

    // --- Patient Multi-Part Document Asset Storage Tracks ---
    @Column(name = "prescription_file_path", length = 255)
    private String prescriptionFilePath;

    // --- Emergency SOS Support ---
    @Column(name = "is_emergency")
    private Boolean isEmergency = false;

    @Column(name = "emergency_description", columnDefinition = "TEXT")
    private String emergencyDescription;

    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;

    @Column(name = "emergency_contact_mobile", length = 15)
    private String emergencyContactMobile;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<BookingItem> selectedItems = new ArrayList<>();
}
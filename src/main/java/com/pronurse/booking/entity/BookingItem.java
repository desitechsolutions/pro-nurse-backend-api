package com.pronurse.booking.entity;

import com.pronurse.catalog.entity.MedicalService;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "booking_selected_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private MedicalService service;

    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    @Column(name = "price_charged", nullable = false)
    private BigDecimal priceCharged;
}
package com.pronurse.booking.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class LiveAlertPayload {
    private String eventType;       // e.g., "NEW_DISPATCH_OFFER", "BOOKING_ACCEPTED", "NURSE_ARRIVED"
    private String bookingNo;
    private String title;
    private String message;
    private Object contextualData;  // Optional extra data (e.g., coordinates, price)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
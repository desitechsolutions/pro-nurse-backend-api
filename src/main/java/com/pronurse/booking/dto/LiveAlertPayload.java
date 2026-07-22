package com.pronurse.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Payload for live alerts/notifications dispatched to users")
public class LiveAlertPayload {
    @Schema(description = "Type of live event", example = "NEW_DISPATCH_OFFER")
    private String eventType;       // e.g., "NEW_DISPATCH_OFFER", "BOOKING_ACCEPTED", "NURSE_ARRIVED"

    @Schema(description = "The booking reference number", example = "BK-2024-001")
    private String bookingNo;

    @Schema(description = "Alert title", example = "New Booking Request")
    private String title;

    @Schema(description = "Alert body message content", example = "A new booking has been requested near you.")
    private String message;

    @Schema(description = "Optional dynamic contextual metadata associated with the event", example = "{\"lat\": 28.6139, \"lng\": 77.2090}")
    private Object contextualData;  // Optional extra data (e.g., coordinates, price)

    @Builder.Default
    @Schema(description = "Timestamp when the alert was generated", example = "2024-01-20T14:30:00")
    private LocalDateTime timestamp = LocalDateTime.now();
}
package com.pronurse.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {
    
    private String templateId;
    private String title;
    private String body;
    private Map<String, String> data;
    
    // Predefined templates
    public static NotificationTemplate newBookingRequest(String bookingNo, String patientName, String address) {
        return NotificationTemplate.builder()
                .templateId("NEW_BOOKING")
                .title("New Booking Request")
                .body(String.format("New booking from %s at %s", patientName, address))
                .data(Map.of(
                        "type", "BOOKING",
                        "bookingNo", bookingNo,
                        "action", "VIEW_BOOKING"
                ))
                .build();
    }
    
    public static NotificationTemplate emergencyAlert(String bookingNo, String address, String description) {
        return NotificationTemplate.builder()
                .templateId("EMERGENCY_SOS")
                .title("🚨 EMERGENCY SOS REQUEST")
                .body(String.format("URGENT: Emergency at %s - %s", address, description))
                .data(Map.of(
                        "type", "BOOKING",
                        "bookingNo", bookingNo,
                        "emergency", "true",
                        "action", "VIEW_EMERGENCY"
                ))
                .build();
    }
    
    public static NotificationTemplate bookingAccepted(String bookingNo, String nurseName) {
        return NotificationTemplate.builder()
                .templateId("BOOKING_ACCEPTED")
                .title("Booking Confirmed")
                .body(String.format("%s has accepted your booking and is on the way", nurseName))
                .data(Map.of(
                        "type", "BOOKING",
                        "bookingNo", bookingNo,
                        "action", "TRACK_NURSE"
                ))
                .build();
    }
    
    public static NotificationTemplate bookingCompleted(String bookingNo) {
        return NotificationTemplate.builder()
                .templateId("BOOKING_COMPLETED")
                .title("Service Completed")
                .body("Your booking has been completed. Please rate your experience.")
                .data(Map.of(
                        "type", "BOOKING",
                        "bookingNo", bookingNo,
                        "action", "RATE_SERVICE"
                ))
                .build();
    }
    
    public static NotificationTemplate paymentReceived(String bookingNo, String amount) {
        return NotificationTemplate.builder()
                .templateId("PAYMENT_RECEIVED")
                .title("Payment Confirmed")
                .body(String.format("Payment of ₹%s received for booking %s", amount, bookingNo))
                .data(Map.of(
                        "type", "PAYMENT",
                        "bookingNo", bookingNo,
                        "amount", amount,
                        "action", "VIEW_RECEIPT"
                ))
                .build();
    }
    
    public static NotificationTemplate earningsAdded(String amount) {
        return NotificationTemplate.builder()
                .templateId("EARNINGS_ADDED")
                .title("Earnings Added")
                .body(String.format("₹%s has been added to your wallet", amount))
                .data(Map.of(
                        "type", "PAYMENT",
                        "amount", amount,
                        "action", "VIEW_WALLET"
                ))
                .build();
    }
    
    public static NotificationTemplate newReview(String rating, String reviewText) {
        return NotificationTemplate.builder()
                .templateId("NEW_REVIEW")
                .title("New Review Received")
                .body(String.format("You received a %s star review: %s", rating, reviewText))
                .data(Map.of(
                        "type", "REVIEW",
                        "rating", rating,
                        "action", "VIEW_REVIEWS"
                ))
                .build();
    }
    
    public static NotificationTemplate bookingCancelled(String bookingNo, String reason) {
        return NotificationTemplate.builder()
                .templateId("BOOKING_CANCELLED")
                .title("Booking Cancelled")
                .body(String.format("Booking %s has been cancelled. Reason: %s", bookingNo, reason))
                .data(Map.of(
                        "type", "BOOKING",
                        "bookingNo", bookingNo,
                        "action", "VIEW_HISTORY"
                ))
                .build();
    }
    
    public static NotificationTemplate bookingRescheduled(String bookingNo, String newDate, String newTime) {
        return NotificationTemplate.builder()
                .templateId("BOOKING_RESCHEDULED")
                .title("Booking Rescheduled")
                .body(String.format("Booking %s rescheduled to %s at %s", bookingNo, newDate, newTime))
                .data(Map.of(
                        "type", "BOOKING",
                        "bookingNo", bookingNo,
                        "action", "VIEW_BOOKING"
                ))
                .build();
    }
    
    public static NotificationTemplate promotional(String title, String message, String actionUrl) {
        return NotificationTemplate.builder()
                .templateId("PROMOTIONAL")
                .title(title)
                .body(message)
                .data(Map.of(
                        "type", "PROMOTIONAL",
                        "action", "OPEN_URL",
                        "url", actionUrl
                ))
                .build();
    }
}

// Made with Bob

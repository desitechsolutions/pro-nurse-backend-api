package com.pronurse.booking.service;

import com.pronurse.booking.dto.LiveAlertPayload;
import com.pronurse.booking.entity.BookingAssignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchAlertService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Wakes up the specific nurse's app with a new "RINGING" offer
     */
    public void broadcastNewOfferToNurse(String nurseMobile, BookingAssignment directOffer) {
        String destination = "/queue/nurse/" + nurseMobile; // Private channel

        LiveAlertPayload alert = LiveAlertPayload.builder()
                .eventType("NEW_DISPATCH_OFFER")
                .bookingNo(directOffer.getBooking().getBookingNo())
                .title("New Booking Request!")
                .message("A patient is requesting services at " + directOffer.getBooking().getRawAddress())
                .contextualData(directOffer.getExpiresAt()) // Mobile uses this to run the 30s visual countdown timer
                .build();

        messagingTemplate.convertAndSend(destination, alert);
        log.debug("WebSocket packet pushed to channel: {}", destination);
    }

    /**
     * Updates the patient's live tracking map when a nurse accepts
     */
    public void broadcastAcceptanceToPatient(String patientMobile, String bookingNo, String nurseName) {
        String destination = "/queue/patient/" + patientMobile;

        LiveAlertPayload alert = LiveAlertPayload.builder()
                .eventType("BOOKING_ACCEPTED")
                .bookingNo(bookingNo)
                .title("Provider Assigned")
                .message(nurseName + " has accepted your request and is on the way.")
                .build();

        messagingTemplate.convertAndSend(destination, alert);
    }

    /**
     * Notifies nurse about booking cancellation
     */
    public void broadcastCancellationToNurse(String nurseMobile, String bookingNo, String reason) {
        String destination = "/queue/nurse/" + nurseMobile;

        LiveAlertPayload alert = LiveAlertPayload.builder()
                .eventType("BOOKING_CANCELLED")
                .bookingNo(bookingNo)
                .title("Booking Cancelled")
                .message("Patient has cancelled booking " + bookingNo + ". Reason: " + reason)
                .build();

        messagingTemplate.convertAndSend(destination, alert);
        log.debug("Cancellation notification sent to nurse: {}", nurseMobile);
    }

    /**
     * Notifies patient about booking cancellation
     */
    public void broadcastCancellationToPatient(String patientMobile, String bookingNo, String reason) {
        String destination = "/queue/patient/" + patientMobile;

        LiveAlertPayload alert = LiveAlertPayload.builder()
                .eventType("BOOKING_CANCELLED")
                .bookingNo(bookingNo)
                .title("Booking Cancelled")
                .message("Nurse has cancelled booking " + bookingNo + ". Reason: " + reason)
                .build();

        messagingTemplate.convertAndSend(destination, alert);
        log.debug("Cancellation notification sent to patient: {}", patientMobile);
    }

    /**
     * Notifies nurse about booking rescheduling
     */
    public void broadcastRescheduleToNurse(String nurseMobile, String bookingNo, String newDate, String newTime, String reason) {
        String destination = "/queue/nurse/" + nurseMobile;

        LiveAlertPayload alert = LiveAlertPayload.builder()
                .eventType("BOOKING_RESCHEDULED")
                .bookingNo(bookingNo)
                .title("Booking Rescheduled")
                .message("Booking " + bookingNo + " has been rescheduled to " + newDate + " at " + newTime +
                        (reason != null ? ". Reason: " + reason : ""))
                .build();

        messagingTemplate.convertAndSend(destination, alert);
        log.debug("Reschedule notification sent to nurse: {}", nurseMobile);
    }

    /**
     * Broadcasts emergency alert to nurse with priority flag
     */
    public void broadcastEmergencyAlert(String nurseMobile, BookingAssignment assignment) {
        String destination = "/queue/nurse/" + nurseMobile;

        LiveAlertPayload alert = LiveAlertPayload.builder()
                .eventType("EMERGENCY_SOS")
                .bookingNo(assignment.getBooking().getBookingNo())
                .title("🚨 EMERGENCY SOS REQUEST")
                .message("URGENT: Emergency medical assistance needed at " + assignment.getBooking().getRawAddress())
                .contextualData(assignment.getExpiresAt())
                .build();

        messagingTemplate.convertAndSend(destination, alert);
        log.info("Emergency SOS alert sent to nurse: {}", nurseMobile);
    }
}
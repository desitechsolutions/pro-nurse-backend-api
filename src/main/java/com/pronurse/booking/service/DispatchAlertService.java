package com.pronurse.booking.service;

import com.pronurse.booking.dto.LiveAlertPayload;
import com.pronurse.booking.entity.BookingAssignment;
import com.pronurse.notification.service.FCMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchAlertService {

    private final SimpMessagingTemplate messagingTemplate;
    private final FCMService fcmService;

    /**
     * Wakes up the specific nurse's app with a new \"RINGING\" offer (WebSocket + FCM Push)
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

        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", "BOOKING");
            data.put("eventType", "NEW_DISPATCH_OFFER");
            data.put("bookingNo", directOffer.getBooking().getBookingNo());
            if (directOffer.getExpiresAt() != null) {
                data.put("expiresAt", directOffer.getExpiresAt().toString());
            }
            fcmService.sendNotification(
                    nurseMobile,
                    "New Booking Request!",
                    "A patient is requesting services at " + directOffer.getBooking().getRawAddress(),
                    data
            );
        } catch (Exception e) {
            log.error("Failed to send FCM push for new offer to nurse {}: {}", nurseMobile, e.getMessage());
        }
    }

    /**
     * Updates the patient's live tracking map when a nurse accepts (WebSocket + FCM Push)
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

        try {
            fcmService.sendNotification(
                    patientMobile,
                    "Provider Assigned",
                    nurseName + " has accepted your request and is on the way.",
                    Map.of(
                            "type", "BOOKING",
                            "eventType", "BOOKING_ACCEPTED",
                            "bookingNo", bookingNo != null ? bookingNo : ""
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send FCM push for booking acceptance to patient {}: {}", patientMobile, e.getMessage());
        }
    }

    /**
     * Notifies nurse about booking cancellation (WebSocket + FCM Push)
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

        try {
            fcmService.sendNotification(
                    nurseMobile,
                    "Booking Cancelled",
                    "Patient has cancelled booking " + bookingNo + (reason != null ? ". Reason: " + reason : ""),
                    Map.of(
                            "type", "BOOKING",
                            "eventType", "BOOKING_CANCELLED",
                            "bookingNo", bookingNo != null ? bookingNo : ""
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send FCM push for cancellation to nurse {}: {}", nurseMobile, e.getMessage());
        }
    }

    /**
     * Notifies patient about booking cancellation (WebSocket + FCM Push)
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

        try {
            fcmService.sendNotification(
                    patientMobile,
                    "Booking Cancelled",
                    "Nurse has cancelled booking " + bookingNo + (reason != null ? ". Reason: " + reason : ""),
                    Map.of(
                            "type", "BOOKING",
                            "eventType", "BOOKING_CANCELLED",
                            "bookingNo", bookingNo != null ? bookingNo : ""
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send FCM push for cancellation to patient {}: {}", patientMobile, e.getMessage());
        }
    }

    /**
     * Notifies nurse about booking rescheduling (WebSocket + FCM Push)
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

        try {
            fcmService.sendNotification(
                    nurseMobile,
                    "Booking Rescheduled",
                    "Booking " + bookingNo + " has been rescheduled to " + newDate + " at " + newTime,
                    Map.of(
                            "type", "BOOKING",
                            "eventType", "BOOKING_RESCHEDULED",
                            "bookingNo", bookingNo != null ? bookingNo : ""
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send FCM push for reschedule to nurse {}: {}", nurseMobile, e.getMessage());
        }
    }

    /**
     * Broadcasts emergency alert to nurse with priority flag (WebSocket + High Priority FCM Push)
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

        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", "BOOKING");
            data.put("eventType", "EMERGENCY_SOS");
            data.put("priority", "EMERGENCY");
            data.put("bookingNo", assignment.getBooking().getBookingNo());
            if (assignment.getExpiresAt() != null) {
                data.put("expiresAt", assignment.getExpiresAt().toString());
            }
            fcmService.sendNotification(
                    nurseMobile,
                    "🚨 EMERGENCY SOS REQUEST",
                    "URGENT: Emergency medical assistance needed at " + assignment.getBooking().getRawAddress(),
                    data
            );
        } catch (Exception e) {
            log.error("Failed to send FCM emergency alert to nurse {}: {}", nurseMobile, e.getMessage());
        }
    }

    /**
     * Notifies patient when no nurse is available in their area (WebSocket + FCM Push)
     */
    public void broadcastNoNurseAvailable(String patientMobile, String bookingNo) {
        String destination = "/queue/patient/" + patientMobile;

        LiveAlertPayload alert = LiveAlertPayload.builder()
                .eventType("NO_NURSE_AVAILABLE")
                .bookingNo(bookingNo)
                .title("No Provider Currently Available")
                .message("We could not match an available provider right now. Our support team has been notified.")
                .build();

        messagingTemplate.convertAndSend(destination, alert);
        log.info("No nurse available alert pushed to patient: {}", patientMobile);

        try {
            fcmService.sendNotification(
                    patientMobile,
                    "No Provider Currently Available",
                    "We could not match an available provider right now. Our support team has been notified.",
                    Map.of(
                            "type", "BOOKING",
                            "eventType", "NO_NURSE_AVAILABLE",
                            "bookingNo", bookingNo != null ? bookingNo : ""
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send FCM no nurse available to patient {}: {}", patientMobile, e.getMessage());
        }
    }

    /**
     * Broadcasts operational alerts to the admin portal (Web dashboard STOMP topic)
     */
    public void broadcastAdminAlert(String eventType, String title, String message, Object contextualData) {
        String destination = "/topic/admin/alerts";

        LiveAlertPayload alert = LiveAlertPayload.builder()
                .eventType(eventType)
                .title(title)
                .message(message)
                .contextualData(contextualData)
                .build();

        messagingTemplate.convertAndSend(destination, alert);
        log.info("Admin alert broadcast to {}: [{}] {}", destination, eventType, title);
    }

    /**
     * Notifies nurse about low wallet balance (WebSocket + FCM Push)
     */
    public void broadcastDebtReminder(String nurseMobile, BigDecimal currentBalance) {
        String destination = "/queue/nurse/" + nurseMobile;

        LiveAlertPayload alert = LiveAlertPayload.builder()
                .eventType("DEBT_REMINDER")
                .title("Low Wallet Balance")
                .message("Your wallet balance is ₹" + currentBalance + ". Please recharge soon to avoid suspension.")
                .contextualData(currentBalance)
                .build();

        messagingTemplate.convertAndSend(destination, alert);
        log.info("Debt reminder pushed to nurse: {}", nurseMobile);

        try {
            fcmService.sendNotification(
                    nurseMobile,
                    "Low Wallet Balance Alert",
                    "Your wallet balance is ₹" + currentBalance + ". Please recharge soon to avoid suspension.",
                    Map.of(
                            "type", "PAYMENT",
                            "eventType", "DEBT_REMINDER",
                            "balance", currentBalance != null ? currentBalance.toString() : "0"
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send FCM debt reminder to nurse {}: {}", nurseMobile, e.getMessage());
        }
    }

    /**
     * Notifies patient when a booking has expired due to lack of provider match (WebSocket + FCM Push)
     */
    public void broadcastBookingExpired(String patientMobile, String bookingNo) {
        String destination = "/queue/patient/" + patientMobile;

        LiveAlertPayload alert = LiveAlertPayload.builder()
                .eventType("BOOKING_EXPIRED")
                .bookingNo(bookingNo)
                .title("Booking Request Expired")
                .message("Your booking request " + bookingNo + " has expired as no provider was available. You may try booking again.")
                .build();

        messagingTemplate.convertAndSend(destination, alert);
        log.info("Booking expired notification pushed to patient: {}", patientMobile);

        try {
            fcmService.sendNotification(
                    patientMobile,
                    "Booking Request Expired",
                    "Your booking request " + bookingNo + " has expired as no provider was available.",
                    Map.of(
                            "type", "BOOKING",
                            "eventType", "BOOKING_EXPIRED",
                            "bookingNo", bookingNo != null ? bookingNo : ""
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send FCM booking expired to patient {}: {}", patientMobile, e.getMessage());
        }
    }
}

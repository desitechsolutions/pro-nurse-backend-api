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
}
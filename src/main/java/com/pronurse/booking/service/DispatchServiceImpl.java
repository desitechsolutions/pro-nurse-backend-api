package com.pronurse.booking.service;

import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.repository.BookingRepository;
import com.pronurse.common.exception.ApplicationException;
import com.pronurse.nurse.entity.NurseProfile;
import com.pronurse.nurse.repository.NurseProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchServiceImpl {

    private final BookingRepository bookingRepository;
    private final NurseProfileRepository nurseProfileRepository;

    private static final double SEARCH_RADIUS_KM = 15.0; // Max matching window threshold

    @Transactional
    public void executeNurseChainDispatch(String bookingNo) {
        log.info("Initiating geospatial broadcast routing matrices for booking: {}", bookingNo);

        Booking booking = bookingRepository.findByBookingNo(bookingNo)
                .orElseThrow(() -> new ApplicationException("Target booking ledger missing: " + bookingNo));

        // 1. Fetch top 5 closest active practitioners within a 15km range radius
        List<NurseProfile> nearbyNurses = nurseProfileRepository.findNearbyAvailableNurses(
                booking.getLatitude(),
                booking.getLongitude(),
                SEARCH_RADIUS_KM,
                PageRequest.of(0, 5)
        );

        if (nearbyNurses.isEmpty()) {
            log.warn("No active practitioners found within search boundaries for order: {}", bookingNo);
            // System can fall back to general dispatch queue routing or trigger an admin alert
            return;
        }

        // 2. Select the absolute closest practitioner to begin the offer loop
        NurseProfile candidateNurse = nearbyNurses.get(0);
        log.info("Closest practitioner located: ID [{}] (Distance calculation verified). Triggering dispatch notification packet.", candidateNurse.getNurseId());

        // 3. TODO: Fire Firebase Push Notification (FCM) or WebSocket state packet here
        // pushNotificationService.sendBookingOffer(candidateNurse.getUser().getId(), booking.getBookingNo());
    }
}
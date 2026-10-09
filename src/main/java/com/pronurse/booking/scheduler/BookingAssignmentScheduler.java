package com.pronurse.booking.scheduler;

import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.entity.BookingAssignment;
import com.pronurse.booking.repository.BookingAssignmentRepository;
import com.pronurse.booking.repository.BookingRepository;
import com.pronurse.booking.service.BookingService;
import com.pronurse.booking.service.DispatchAlertService;
import com.pronurse.wallet.entity.NurseWallet;
import com.pronurse.wallet.repository.NurseWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingAssignmentScheduler {

    private final BookingAssignmentRepository assignmentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final NurseWalletRepository walletRepository;
    private final DispatchAlertService alertService;

    /**
     * Scans for expired, unanswered nurse offers every 5 seconds.
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void clearExpiredOffers() {
        List<BookingAssignment> timedOutOffers = assignmentRepository
                .findByStatusAndExpiresAtBefore("RINGING", LocalDateTime.now());

        for (BookingAssignment assignment : timedOutOffers) {
            log.info("Offer for booking {} timed out on nurse {}.",
                    assignment.getBooking().getBookingNo(), assignment.getNurseUser().getMobile());

            assignment.setStatus("TIMEOUT");
            assignmentRepository.save(assignment);

            // Re-route the booking to the next closest nurse
            bookingService.triggerChainedDispatch(assignment.getBooking());
        }
    }

    /**
     * Broadcasts daily low balance debt warnings to nurses whose wallet is below -100 threshold
     */
    @Scheduled(cron = "0 0 9 * * *") // Runs every day at 9 AM
    public void sendDebtReminders() {
        List<NurseWallet> lowBalanceNurses = walletRepository.findByCurrentBalanceLessThanAndIsSuspendedFalse(new BigDecimal("-100.00"));
        for (NurseWallet wallet : lowBalanceNurses) {
            alertService.broadcastDebtReminder(wallet.getNurseUser().getMobile(), wallet.getCurrentBalance());
            log.info("Debt reminder pushed to nurse: {} (Balance: {})", wallet.getNurseUser().getMobile(), wallet.getCurrentBalance());
        }
    }

    /**
     * Automatically expires stale, unassigned bookings older than 30 minutes to keep queues clean
     */
    @Scheduled(fixedDelay = 300000) // Runs every 5 minutes
    @Transactional
    public void autoExpireStaleBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        List<Booking> staleBookings = bookingRepository.findStaleUnassignedBookings(cutoff);

        for (Booking booking : staleBookings) {
            log.info("Auto-expiring stale booking: {} (Status: {}, CreatedAt: {})",
                    booking.getBookingNo(), booking.getBookingStatus(), booking.getCreatedAt());

            booking.setBookingStatus("EXPIRED");
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);

            alertService.broadcastBookingExpired(booking.getPatientUser().getMobile(), booking.getBookingNo());
        }
    }
}

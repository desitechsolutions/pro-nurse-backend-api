package com.pronurse.booking.scheduler;

import com.pronurse.booking.entity.BookingAssignment;
import com.pronurse.booking.repository.BookingAssignmentRepository;
import com.pronurse.booking.service.BookingService;
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
    private final BookingService bookingService;
    private final NurseWalletRepository walletRepository;

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

    @Scheduled(cron = "0 0 9 * * *") // Runs every day at 9 AM
    public void sendDebtReminders() {
        List<NurseWallet> lowBalanceNurses = walletRepository.findByCurrentBalanceLessThanAndIsSuspendedFalse(new BigDecimal("-100.00"));
        for (NurseWallet wallet : lowBalanceNurses) {
            // alertService.sendReminder(wallet.getNurseUser().getMobile(), "Your balance is low. Please pay to avoid suspension.");
        }
    }
}
package com.pronurse.wallet.service;

import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.entity.BookingItem;
import com.pronurse.common.exception.ApplicationException;
import com.pronurse.wallet.dto.NurseWalletSummary;
import com.pronurse.wallet.dto.WalletTxStatementItem;
import com.pronurse.wallet.entity.NurseWallet;
import com.pronurse.wallet.entity.PayoutRequest;
import com.pronurse.wallet.entity.WalletTransaction;
import com.pronurse.wallet.repository.NurseWalletRepository;
import com.pronurse.wallet.repository.PayoutRepository;
import com.pronurse.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final NurseWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final PayoutRepository payoutRepository;

    // Production Fee Constants
    private static final BigDecimal PLATFORM_FEE_PERCENTAGE = new BigDecimal("0.10");
    private static final BigDecimal GST_RATE = new BigDecimal("0.18");

    @Override
    @Transactional
    public void processServiceCompletionEarnings(Booking booking) {
        if (booking.getAssignedNurseUser() == null) return;

        // 1. Calculate Gross Booking Price
        BigDecimal gross = booking.getSelectedItems().stream()
                .map(BookingItem::getPriceCharged)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Compute Platform Fee (10%) + GST on Fee (18%)
        BigDecimal platformFee = gross.multiply(PLATFORM_FEE_PERCENTAGE);
        BigDecimal gstOnFee = platformFee.multiply(GST_RATE);
        BigDecimal totalDeduction = platformFee.add(gstOnFee).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netStaffPayout = gross.subtract(totalDeduction);

        // 3. Load/Initialize Wallet
        NurseWallet wallet = walletRepository.findByNurseUserMobile(booking.getAssignedNurseUser().getMobile())
                .orElseGet(() -> NurseWallet.builder()
                        .nurseUser(booking.getAssignedNurseUser())
                        .currentBalance(BigDecimal.ZERO)
                        .totalEarned(BigDecimal.ZERO)
                        .negativeLimit(new BigDecimal("500.00"))
                        .isSuspended(false)
                        .build());

        // 4. Update Ledger
        wallet.setCurrentBalance(wallet.getCurrentBalance().add(netStaffPayout));
        wallet.setTotalEarned(wallet.getTotalEarned().add(netStaffPayout));
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        // 5. Audit Trail
        transactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .booking(booking)
                .transactionType("CREDIT")
                .grossAmount(gross)
                .platformFee(totalDeduction)
                .netEarning(netStaffPayout)
                .createdAt(LocalDateTime.now())
                .build());

        log.info("Financial settlement committed for ticket [{}]. Net Credited: {}", booking.getBookingNo(), netStaffPayout);
    }

    @Override
    @Transactional
    public void requestPayout(String nurseMobile, BigDecimal amount) {
        NurseWallet wallet = walletRepository.findByNurseUserMobile(nurseMobile)
                .orElseThrow(() -> new ApplicationException("Nurse wallet not found."));

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApplicationException("Invalid withdrawal amount.");
        }
        if (wallet.getCurrentBalance().compareTo(amount) < 0) {
            throw new ApplicationException("Insufficient balance for withdrawal.");
        }

        // Create the Payout Request record
        PayoutRequest request = PayoutRequest.builder()
                .wallet(wallet)
                .amount(amount)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        payoutRepository.save(request);

        // Lock the funds immediately to prevent double spending
        wallet.setCurrentBalance(wallet.getCurrentBalance().subtract(amount));
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        log.info("Payout request of {} created for nurse: {}", amount, nurseMobile);
    }

    @Override
    @Transactional(readOnly = true)
    public NurseWalletSummary getNurseWalletDashboard(String nurseMobile) {
        return walletRepository.findByNurseUserMobile(nurseMobile)
                .map(wallet -> {
                    List<WalletTransaction> historicalTx = transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
                    List<WalletTxStatementItem> statementsList = historicalTx.stream().map(tx ->
                            WalletTxStatementItem.builder()
                                    .bookingNo(tx.getBooking().getBookingNo())
                                    .date(tx.getCreatedAt().toString())
                                    .type(tx.getTransactionType())
                                    .grossAmount(tx.getGrossAmount())
                                    .platformDeduction(tx.getPlatformFee())
                                    .netPayoutAmount(tx.getNetEarning())
                                    .build()
                    ).collect(Collectors.toList());

                    return NurseWalletSummary.builder()
                            .walletBalance(wallet.getCurrentBalance())
                            .lifetimeEarnings(wallet.getTotalEarned())
                            .statements(statementsList)
                            .build();
                })
                .orElseGet(() -> NurseWalletSummary.builder()
                        .walletBalance(BigDecimal.ZERO)
                        .lifetimeEarnings(BigDecimal.ZERO)
                        .statements(List.of())
                        .build());
    }

    @Override
    @Transactional
    public void validateWalletThreshold(String nurseMobile) {
        NurseWallet wallet = walletRepository.findByNurseUserMobile(nurseMobile)
                .orElseThrow(() -> new ApplicationException("Wallet not found"));

        if (wallet.isSuspended()) {
            throw new ApplicationException("Account suspended: Wallet limit exceeded.");
        }

        // Check if balance drops below the negative limit
        if (wallet.getCurrentBalance().compareTo(wallet.getNegativeLimit().negate()) < 0) {
            wallet.setSuspended(true);
            walletRepository.save(wallet);
            log.warn("Nurse {} suspended due to negative balance.", nurseMobile);
        }
    }
}
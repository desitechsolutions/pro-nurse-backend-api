package com.pronurse.wallet.service;

import com.pronurse.booking.entity.Booking;
import com.pronurse.wallet.dto.NurseWalletSummary;

import java.math.BigDecimal;

public interface WalletService {
    void processServiceCompletionEarnings(Booking booking);
    NurseWalletSummary getNurseWalletDashboard(String nurseMobile);
    void requestPayout(String nurseMobile, BigDecimal amount);
    void validateWalletThreshold(String nurseMobile);
}
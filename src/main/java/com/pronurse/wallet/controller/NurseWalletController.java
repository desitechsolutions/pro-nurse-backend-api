package com.pronurse.wallet.controller;

import com.pronurse.common.payload.ApiResponse;
import com.pronurse.wallet.dto.NurseWalletSummary;
import com.pronurse.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/nurse/wallet")
@RequiredArgsConstructor
public class NurseWalletController {

    private final WalletService walletService;

    /**
     * Flutter App Hook: Pull real-time operational wallet balances and earnings histories
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<ApiResponse<NurseWalletSummary>> getMyEarnings(Authentication authentication) {
        String nurseMobile = (String) authentication.getPrincipal();
        NurseWalletSummary currentLedger = walletService.getNurseWalletDashboard(nurseMobile);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Nurse practitioner ledger portfolio data statements generated cleanly.",
                currentLedger
        ));
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<ApiResponse<Void>> requestWithdrawal(
            Authentication authentication,
            @RequestBody Map<String, BigDecimal> requestBody) {

        String nurseMobile = (String) authentication.getPrincipal();
        walletService.requestPayout(nurseMobile, requestBody.get("amount"));

        return ResponseEntity.ok(new ApiResponse<>(true, "Payout request submitted for admin review.", null));
    }
}
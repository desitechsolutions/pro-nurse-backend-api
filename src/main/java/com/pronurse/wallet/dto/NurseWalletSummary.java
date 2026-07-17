package com.pronurse.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Summary of nurse wallet balance, earnings history, and transactions")
public class NurseWalletSummary {
    @Schema(description = "Current withdrawable wallet balance", example = "2500.00")
    private BigDecimal walletBalance;

    @Schema(description = "Lifetime total earnings accumulated", example = "15000.00")
    private BigDecimal lifetimeEarnings;

    @Schema(description = "List of recent transaction statement items")
    private List<WalletTxStatementItem> statements;
}
package com.pronurse.wallet.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NurseWalletSummary {
    private BigDecimal walletBalance;
    private BigDecimal lifetimeEarnings;
    private List<WalletTxStatementItem> statements;
}
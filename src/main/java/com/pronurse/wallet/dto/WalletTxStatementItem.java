package com.pronurse.wallet.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTxStatementItem {
    private String bookingNo;
    private String date;
    private String type; // CREDIT / DEBIT
    private BigDecimal grossAmount;
    private BigDecimal platformDeduction; // The 10% Cut
    private BigDecimal netPayoutAmount;     // 90% Staff Earned portion
}
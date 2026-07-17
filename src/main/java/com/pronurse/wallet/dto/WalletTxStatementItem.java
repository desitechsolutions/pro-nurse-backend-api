package com.pronurse.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details of a single transaction statement in the wallet ledger")
public class WalletTxStatementItem {
    @Schema(description = "Unique booking number related to the transaction", example = "BK-12345")
    private String bookingNo;

    @Schema(description = "Transaction timestamp or date string", example = "2026-07-17T16:30:00")
    private String date;

    @Schema(description = "Transaction type (e.g. CREDIT or DEBIT)", example = "CREDIT")
    private String type; // CREDIT / DEBIT

    @Schema(description = "Gross amount before deductions", example = "1000.00")
    private BigDecimal grossAmount;

    @Schema(description = "Deduction amount (e.g. 10% platform fee)", example = "100.00")
    private BigDecimal platformDeduction; // The 10% Cut

    @Schema(description = "Net payout amount received by the nurse (e.g. 90% portion)", example = "900.00")
    private BigDecimal netPayoutAmount;     // 90% Staff Earned portion
}
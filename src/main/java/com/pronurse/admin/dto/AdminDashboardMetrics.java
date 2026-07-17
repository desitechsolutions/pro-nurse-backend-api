package com.pronurse.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dashboard metrics representing live platform stats and key performance indicators")
public class AdminDashboardMetrics {
    @Schema(description = "Total number of patients registered in the system", example = "1250")
    private long totalPatientsCount;

    @Schema(description = "Total number of nurses registered in the system", example = "85")
    private long totalNursesCount;

    @Schema(description = "Number of nurse profiles pending verification", example = "12")
    private long pendingApprovalsCount;

    @Schema(description = "Total bookings count in history", example = "3500")
    private long totalBookingsCount;

    @Schema(description = "Count of active/live bookings currently in progress", example = "8")
    private long activeLiveBookingsCount;

    @Schema(description = "Gross revenue of the platform", example = "450000.00")
    private BigDecimal totalGrossRevenue;

    @Schema(description = "Percentage of bookings marked COMPLETED vs overall", example = "94.5")
    private double globalFulfillmentRate; // Percentage of bookings marked COMPLETED vs overall
}
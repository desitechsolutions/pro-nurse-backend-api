package com.pronurse.admin.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardMetrics {
    private long totalPatientsCount;
    private long totalNursesCount;
    private long pendingApprovalsCount;
    private long totalBookingsCount;
    private long activeLiveBookingsCount;
    private BigDecimal totalGrossRevenue;
    private double globalFulfillmentRate; // Percentage of bookings marked COMPLETED vs overall
}
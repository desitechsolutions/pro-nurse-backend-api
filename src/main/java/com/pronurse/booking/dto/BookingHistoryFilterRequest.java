package com.pronurse.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Advanced booking history filter request")
public class BookingHistoryFilterRequest {

    @Schema(description = "Start date for filtering", example = "2024-01-01")
    private LocalDate startDate;

    @Schema(description = "End date for filtering", example = "2024-12-31")
    private LocalDate endDate;

    @Schema(description = "Booking status filter", example = "[\"COMPLETED\", \"CONFIRMED\"]")
    private List<String> statuses;

    @Schema(description = "Service IDs filter", example = "[1, 2, 3]")
    private List<Long> serviceIds;

    @Schema(description = "Minimum amount", example = "500.00")
    private BigDecimal minAmount;

    @Schema(description = "Maximum amount", example = "5000.00")
    private BigDecimal maxAmount;

    @Schema(description = "Search by booking number or nurse name", example = "BK-2024")
    private String searchText;

    @Schema(description = "Emergency bookings only", example = "false")
    private Boolean emergencyOnly;

    @Schema(description = "Page number (0-based)", example = "0")
    private Integer page = 0;

    @Schema(description = "Page size", example = "20")
    private Integer size = 20;

    @Schema(description = "Sort field", example = "bookingDate", allowableValues = {"bookingDate", "createdAt", "totalAmount"})
    private String sortBy = "bookingDate";

    @Schema(description = "Sort direction", example = "DESC", allowableValues = {"ASC", "DESC"})
    private String sortDirection = "DESC";
}

// Made with Bob

package com.pronurse.booking.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingHistoryResponse {
    private String bookingId;
    private String bookingDate;
    private String bookingTime;
    private String bookingStatus;
    private String paymentStatus;
    private String address;
    private String remarks;
    private String prescriptionUrl;

    // Counterpart details depending on who is making the call
    private String CounterpartyName;
    private String CounterpartyMobile;

    private Double nurseLatitude;
    private Double nurseLongitude;

    private BigDecimal totalAmount;
    private List<String> serviceNames;
}
package com.pronurse.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseDashboardResponse {
    private String bookingId;
    private String patientName;
    private String patientMobile;
    private String bookingDate;
    private String bookingTime;
    private String address;
    private String remarks;
    private BigDecimal totalPrice;
    private String bookingStatus;
    private String assignmentStatus; // 'RINGING' or 'ACCEPTED'
    private List<String> selectedServices;
}
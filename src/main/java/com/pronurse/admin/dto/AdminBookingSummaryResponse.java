package com.pronurse.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class AdminBookingSummaryResponse {
    private Long id;
    private String bookingNo;
    private String patientName;
    private String patientMobile;
    private String assignedNurseName;
    private String assignedNurseMobile;
    private LocalDate bookingDate;
    private String bookingTime;
    private String bookingStatus;
    private String paymentStatus;
    private BigDecimal totalAmount;
}
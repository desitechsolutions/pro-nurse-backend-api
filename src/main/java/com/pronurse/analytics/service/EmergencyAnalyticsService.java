package com.pronurse.analytics.service;

import com.pronurse.analytics.dto.EmergencyAnalyticsResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface EmergencyAnalyticsService {
    
    /**
     * Get emergency response analytics for a specific booking
     */
    EmergencyAnalyticsResponse getEmergencyAnalytics(String bookingNo);
    
    /**
     * Get all emergency bookings analytics for a date range
     */
    List<EmergencyAnalyticsResponse> getEmergencyAnalyticsByDateRange(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get average response time statistics
     */
    Map<String, Object> getAverageResponseTimeStats(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get emergency acceptance rate
     */
    Map<String, Object> getEmergencyAcceptanceRate(LocalDate startDate, LocalDate endDate);
}



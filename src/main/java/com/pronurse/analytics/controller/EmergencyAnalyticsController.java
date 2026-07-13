package com.pronurse.analytics.controller;

import com.pronurse.analytics.dto.EmergencyAnalyticsResponse;
import com.pronurse.analytics.service.EmergencyAnalyticsService;
import com.pronurse.common.payload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics/emergency")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "19. Emergency Analytics", description = "Emergency response time analytics and statistics")
@SecurityRequirement(name = "bearerAuth")
public class EmergencyAnalyticsController {

    private final EmergencyAnalyticsService analyticsService;

    @GetMapping("/{bookingNo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'PATIENT')")
    @Operation(summary = "Get emergency analytics for a booking", 
               description = "Get detailed response time analytics for a specific emergency booking")
    public ResponseEntity<ApiResponse<EmergencyAnalyticsResponse>> getEmergencyAnalytics(
            @PathVariable String bookingNo) {
        
        EmergencyAnalyticsResponse analytics = analyticsService.getEmergencyAnalytics(bookingNo);
        
        return ResponseEntity.ok(ApiResponse.<EmergencyAnalyticsResponse>builder()
                .success(true)
                .message("Emergency analytics retrieved successfully")
                .data(analytics)
                .build());
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get emergency analytics by date range", 
               description = "Get all emergency bookings analytics for a specific date range")
    public ResponseEntity<ApiResponse<List<EmergencyAnalyticsResponse>>> getEmergencyAnalyticsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<EmergencyAnalyticsResponse> analytics = analyticsService
                .getEmergencyAnalyticsByDateRange(startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.<List<EmergencyAnalyticsResponse>>builder()
                .success(true)
                .message("Emergency analytics retrieved successfully")
                .data(analytics)
                .build());
    }

    @GetMapping("/stats/response-time")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get average response time statistics", 
               description = "Get average, min, max response times for emergency bookings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAverageResponseTimeStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> stats = analyticsService.getAverageResponseTimeStats(startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Response time statistics retrieved successfully")
                .data(stats)
                .build());
    }

    @GetMapping("/stats/acceptance-rate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get emergency acceptance rate", 
               description = "Get acceptance rate statistics for emergency bookings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEmergencyAcceptanceRate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> stats = analyticsService.getEmergencyAcceptanceRate(startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Acceptance rate statistics retrieved successfully")
                .data(stats)
                .build());
    }
}



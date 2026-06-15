package com.pronurse.analytics.service;

import com.pronurse.analytics.dto.EmergencyAnalyticsResponse;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.entity.BookingAssignment;
import com.pronurse.booking.repository.BookingAssignmentRepository;
import com.pronurse.booking.repository.BookingRepository;
import com.pronurse.common.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencyAnalyticsServiceImpl implements EmergencyAnalyticsService {

    private final BookingRepository bookingRepository;
    private final BookingAssignmentRepository assignmentRepository;

    @Override
    @Transactional(readOnly = true)
    public EmergencyAnalyticsResponse getEmergencyAnalytics(String bookingNo) {
        Booking booking = bookingRepository.findByBookingNo(bookingNo)
                .orElseThrow(() -> new ApplicationException("Booking not found"));

        if (booking.getIsEmergency() == null || !booking.getIsEmergency()) {
            throw new ApplicationException("This is not an emergency booking");
        }

        List<BookingAssignment> assignments = assignmentRepository
                .findByBookingIdOrderByNotifiedAtAsc(booking.getId());

        if (assignments.isEmpty()) {
            throw new ApplicationException("No assignment records found");
        }

        BookingAssignment firstAssignment = assignments.get(0);
        BookingAssignment acceptedAssignment = assignments.stream()
                .filter(a -> "ACCEPTED".equals(a.getStatus()) || "EMERGENCY_RINGING".equals(a.getStatus()))
                .findFirst()
                .orElse(null);

        Long responseTime = null;
        if (acceptedAssignment != null && acceptedAssignment.getNotifiedAt() != null) {
            responseTime = Duration.between(
                    booking.getCreatedAt(),
                    acceptedAssignment.getNotifiedAt()
            ).getSeconds();
        }

        long rejectedCount = assignments.stream()
                .filter(a -> "REJECTED".equals(a.getStatus()))
                .count();

        return EmergencyAnalyticsResponse.builder()
                .bookingNo(booking.getBookingNo())
                .emergencyCreatedAt(booking.getCreatedAt())
                .firstNotificationTime(firstAssignment.getNotifiedAt())
                .acceptedAt(acceptedAssignment != null ? acceptedAssignment.getNotifiedAt() : null)
                .responseTimeSeconds(responseTime)
                .nursesNotified(assignments.size())
                .nursesRejected((int) rejectedCount)
                .acceptedNurseName(acceptedAssignment != null && acceptedAssignment.getNurseUser() != null
                        ? acceptedAssignment.getNurseUser().getName() : null)
                .emergencyDescription(booking.getEmergencyDescription())
                .patientAddress(booking.getRawAddress())
                .status(booking.getBookingStatus())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyAnalyticsResponse> getEmergencyAnalyticsByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Booking> emergencyBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getIsEmergency() != null && b.getIsEmergency())
                .filter(b -> !b.getBookingDate().isBefore(startDate) && !b.getBookingDate().isAfter(endDate))
                .collect(Collectors.toList());

        return emergencyBookings.stream()
                .map(booking -> {
                    try {
                        return getEmergencyAnalytics(booking.getBookingNo());
                    } catch (Exception e) {
                        log.error("Error getting analytics for booking: {}", booking.getBookingNo(), e);
                        return null;
                    }
                })
                .filter(analytics -> analytics != null)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAverageResponseTimeStats(LocalDate startDate, LocalDate endDate) {
        List<EmergencyAnalyticsResponse> analytics = getEmergencyAnalyticsByDateRange(startDate, endDate);

        List<Long> responseTimes = analytics.stream()
                .filter(a -> a.getResponseTimeSeconds() != null)
                .map(EmergencyAnalyticsResponse::getResponseTimeSeconds)
                .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEmergencies", analytics.size());
        stats.put("emergenciesWithResponse", responseTimes.size());

        if (!responseTimes.isEmpty()) {
            double avgResponseTime = responseTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);

            long minResponseTime = responseTimes.stream()
                    .mapToLong(Long::longValue)
                    .min()
                    .orElse(0L);

            long maxResponseTime = responseTimes.stream()
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0L);

            stats.put("averageResponseTimeSeconds", avgResponseTime);
            stats.put("minResponseTimeSeconds", minResponseTime);
            stats.put("maxResponseTimeSeconds", maxResponseTime);
            stats.put("averageResponseTimeMinutes", avgResponseTime / 60.0);
        } else {
            stats.put("averageResponseTimeSeconds", 0);
            stats.put("minResponseTimeSeconds", 0);
            stats.put("maxResponseTimeSeconds", 0);
            stats.put("averageResponseTimeMinutes", 0);
        }

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getEmergencyAcceptanceRate(LocalDate startDate, LocalDate endDate) {
        List<EmergencyAnalyticsResponse> analytics = getEmergencyAnalyticsByDateRange(startDate, endDate);

        long totalEmergencies = analytics.size();
        long acceptedEmergencies = analytics.stream()
                .filter(a -> a.getAcceptedNurseName() != null)
                .count();

        long totalNursesNotified = analytics.stream()
                .mapToInt(EmergencyAnalyticsResponse::getNursesNotified)
                .sum();

        long totalNursesRejected = analytics.stream()
                .mapToInt(EmergencyAnalyticsResponse::getNursesRejected)
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEmergencies", totalEmergencies);
        stats.put("acceptedEmergencies", acceptedEmergencies);
        stats.put("rejectedEmergencies", totalEmergencies - acceptedEmergencies);
        stats.put("acceptanceRate", totalEmergencies > 0 ? (acceptedEmergencies * 100.0 / totalEmergencies) : 0);
        stats.put("totalNursesNotified", totalNursesNotified);
        stats.put("totalNursesRejected", totalNursesRejected);
        stats.put("nurseAcceptanceRate", totalNursesNotified > 0 
                ? ((totalNursesNotified - totalNursesRejected) * 100.0 / totalNursesNotified) : 0);

        return stats;
    }
}

// Made with Bob

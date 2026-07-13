package com.pronurse.booking.repository;

import com.pronurse.booking.entity.BookingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingAssignmentRepository extends JpaRepository<BookingAssignment, Long> {
    List<BookingAssignment> findByStatusAndExpiresAtBefore(String status, LocalDateTime time);
    Optional<BookingAssignment> findByBookingBookingNoAndNurseUserMobile(String bookingNo, String mobile);

    @org.springframework.data.jpa.repository.Query(
            "SELECT ba FROM BookingAssignment ba " +
                    "JOIN FETCH ba.booking b " +
                    "JOIN FETCH b.patientUser u " +
                    "WHERE ba.nurseUser.mobile = :mobile " +
                    "AND ba.status IN ('RINGING', 'ACCEPTED') " +
                    "AND b.bookingStatus IN ('PENDING', 'ACCEPTED', 'IN_PROGRESS') " +
                    "ORDER BY ba.notifiedAt DESC"
    )
    List<com.pronurse.booking.entity.BookingAssignment> findActiveNurseDashboardFeeds(@org.springframework.data.repository.query.Param("mobile") String mobile);
    
    List<BookingAssignment> findByBookingIdOrderByNotifiedAtAsc(Long bookingId);
}
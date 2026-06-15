package com.pronurse.booking.repository;

import com.pronurse.booking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingNo(String bookingNo);

    /**
     * Native spatial query fallback matching the closest on-duty available nurse
     * while completely excluding anyone who has already rejected or timed out.
     */
    @Query(value = "SELECT np.user_id FROM nurse_profiles np " +
            "JOIN users u ON np.user_id = u.id " +
            "WHERE np.is_verified = true AND np.is_on_duty = true " +
            "AND np.user_id NOT IN (SELECT ba.nurse_user_id FROM booking_assignments ba WHERE ba.booking_id = :bookingId) " +
            "ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(np.latitude)) * cos(radians(np.longitude) - radians(:lon)) + sin(radians(:lat)) * sin(radians(np.latitude)))) ASC " +
            "LIMIT 1", nativeQuery = true)
    Optional<Long> findNextClosestNurseId(@Param("lat") double lat, @Param("lon") double lon, @Param("bookingId") Long bookingId);

    List<Booking> findByPatientUserMobileOrderByCreatedAtDesc(String mobile);


    List<Booking> findByAssignedNurseUserMobileOrderByCreatedAtDesc(String mobile);
    Page<Booking> findByBookingStatus(String bookingStatus, Pageable pageable);

}
package com.pronurse.admin.repository;

import com.pronurse.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;

@Repository
public interface AdminAnalyticsRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'PATIENT'")
    long countTotalPatients();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'NURSE'")
    long countTotalNurses();

    @Query("SELECT COUNT(np) FROM NurseProfile np WHERE np.verificationStatus = 'Pending'")
    long countPendingNurseApprovals();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookingStatus IN ('PENDING', 'ACCEPTED', 'IN_PROGRESS')")
    long countLiveActiveBookings();

    @Query("SELECT COALESCE(SUM(bi.priceCharged), 0) FROM BookingItem bi WHERE bi.booking.paymentStatus = 'PAID'")
    BigDecimal calculateGrossRevenue();
}
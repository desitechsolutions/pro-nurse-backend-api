package com.pronurse.util;

import com.pronurse.auth.entity.User;
import com.pronurse.auth.model.Role;
import com.pronurse.patient.entity.PatientProfile;
import com.pronurse.nurse.entity.NurseProfile;
import com.pronurse.catalog.entity.MedicalService;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.entity.BookingItem;
import com.pronurse.booking.entity.BookingAssignment;
import com.pronurse.review.entity.NurseReview;
import com.pronurse.wallet.entity.NurseWallet;
import com.pronurse.wallet.entity.WalletTransaction;
import com.pronurse.wallet.entity.PayoutRequest;
import com.pronurse.favorites.entity.FavoriteNurse;
import com.pronurse.notification.entity.FCMToken;
import com.pronurse.notification.entity.NotificationPreference;
import com.pronurse.enums.Gender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TestDataFactory {

    public static User createUser(String mobile, Role role) {
        User user = new User();
        user.setMobile(mobile);
        user.setName("Test User " + mobile);
        user.setEmail("user" + mobile + "@test.com");
        user.setPasswordHash("$2a$10$7ad74b33bbf1463393476e9df8fe4f0d"); // dummy bcrypt hash
        user.setRole(role);
        user.setActive(true);
        user.setMobileVerified(true);
        return user;
    }

    public static PatientProfile createPatientProfile(User user, String patientId) {
        PatientProfile profile = new PatientProfile();
        profile.setPatientId(patientId);
        profile.setUser(user);
        profile.setGender(Gender.MALE);
        profile.setDob(LocalDate.of(1990, 5, 15));
        profile.setBloodGroup("O+");
        profile.setAddress("123 Test Patient Address");
        profile.setLatitude(28.6289);
        profile.setLongitude(77.3649);
        profile.setChronicDiseases("None");
        return profile;
    }

    public static NurseProfile createNurseProfile(User user, String nurseId) {
        NurseProfile profile = new NurseProfile();
        profile.setNurseId(nurseId);
        profile.setUser(user);
        profile.setGender(Gender.FEMALE);
        profile.setDob(LocalDate.of(1995, 8, 20));
        profile.setAddress("456 Test Nurse Address");
        profile.setQualification("B.Sc Nursing");
        profile.setExperience("5 Years");
        profile.setSpecialization("ICU Care");
        profile.setLanguages("Hindi, English");
        profile.setLatitude(28.6300);
        profile.setLongitude(77.3650);
        profile.setRegistrationNumber("REG-" + nurseId);
        profile.setVerified(true);
        profile.setOnDuty(true);
        profile.setVerificationStatus("Approved");
        profile.setAverageRating(4.5);
        profile.setTotalReviewsCount(10);
        profile.setCity("Lucknow");
        profile.setConsultationFee(BigDecimal.valueOf(500.00));
        return profile;
    }

    public static MedicalService createMedicalService(String name, BigDecimal price) {
        return MedicalService.builder()
                .name(name)
                .description("Mock description for " + name)
                .basePrice(price)
                .estimatedDurationMinutes(120)
                .isActive(true)
                .build();
    }

    public static Booking createBooking(User patient, String bookingNo) {
        Booking booking = new Booking();
        booking.setBookingNo(bookingNo);
        booking.setPatientUser(patient);
        booking.setBookingDate(LocalDate.now());
        booking.setBookingTime("10:00 AM");
        booking.setRawAddress("789 Booking Street Address");
        booking.setBookingStatus("PENDING");
        booking.setPaymentStatus("PENDING");
        booking.setLatitude(28.6289);
        booking.setLongitude(77.3649);
        return booking;
    }

    public static BookingItem createBookingItem(Booking booking, MedicalService service) {
        return BookingItem.builder()
                .booking(booking)
                .service(service)
                .itemName(service.getName())
                .priceCharged(service.getBasePrice())
                .build();
    }

    public static BookingAssignment createBookingAssignment(Booking booking, User nurse) {
        return BookingAssignment.builder()
                .booking(booking)
                .nurseUser(nurse)
                .status("RINGING")
                .notifiedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
    }

    public static NurseReview createNurseReview(Booking booking, User nurse, User patient, int rating) {
        return NurseReview.builder()
                .booking(booking)
                .nurseUser(nurse)
                .patientUser(patient)
                .ratingScore(rating)
                .reviewText("Great service by " + nurse.getName())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static NurseWallet createNurseWallet(User nurse) {
        return NurseWallet.builder()
                .nurseUser(nurse)
                .currentBalance(BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO)
                .negativeLimit(BigDecimal.valueOf(500.00))
                .isSuspended(false)
                .build();
    }

    public static WalletTransaction createWalletTransaction(NurseWallet wallet, Booking booking, BigDecimal amount) {
        BigDecimal platformFee = amount.multiply(BigDecimal.valueOf(0.1)); // 10%
        BigDecimal netEarning = amount.subtract(platformFee);
        return WalletTransaction.builder()
                .wallet(wallet)
                .booking(booking)
                .transactionType("CREDIT")
                .grossAmount(amount)
                .platformFee(platformFee)
                .netEarning(netEarning)
                .build();
    }

    public static PayoutRequest createPayoutRequest(NurseWallet wallet, BigDecimal amount) {
        PayoutRequest request = new PayoutRequest();
        request.setWallet(wallet);
        request.setAmount(amount);
        request.setStatus("PENDING");
        return request;
    }

    public static FavoriteNurse createFavoriteNurse(User patient, User nurse) {
        FavoriteNurse favorite = new FavoriteNurse();
        favorite.setPatient(patient);
        favorite.setNurse(nurse);
        return favorite;
    }

    public static FCMToken createFCMToken(User user, String deviceToken) {
        FCMToken fcmToken = new FCMToken();
        fcmToken.setUser(user);
        fcmToken.setDeviceToken(deviceToken);
        fcmToken.setDeviceType("ANDROID");
        fcmToken.setDeviceName("Pixel 6");
        fcmToken.setIsActive(true);
        fcmToken.setLastUsedAt(LocalDateTime.now());
        return fcmToken;
    }

    public static NotificationPreference createNotificationPreference(User user) {
        NotificationPreference pref = new NotificationPreference();
        pref.setUser(user);
        pref.setBookingAlerts(true);
        pref.setPaymentAlerts(true);
        pref.setReviewAlerts(true);
        pref.setPromotionalAlerts(false);
        return pref;
    }
}

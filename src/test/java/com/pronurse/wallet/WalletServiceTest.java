package com.pronurse.wallet;

import com.pronurse.auth.entity.User;
import com.pronurse.auth.model.Role;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.entity.BookingItem;
import com.pronurse.booking.repository.BookingRepository;
import com.pronurse.catalog.entity.MedicalService;
import com.pronurse.catalog.repository.MedicalServiceRepository;
import com.pronurse.config.TestConfig;
import com.pronurse.wallet.dto.NurseWalletSummary;
import com.pronurse.wallet.entity.NurseWallet;
import com.pronurse.wallet.repository.NurseWalletRepository;
import com.pronurse.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
class WalletServiceTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private NurseWalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private MedicalServiceRepository serviceRepository;

    private User testNurse;
    private User testPatient;

    @BeforeEach
    void setUp() {
        // Create test nurse
        testNurse = new User();
        testNurse.setMobile("6666666666");
        testNurse.setName("Test Nurse");
        testNurse.setRole(Role.NURSE);
        testNurse.setActive(true);
        testNurse = userRepository.save(testNurse);

        // Create test patient
        testPatient = new User();
        testPatient.setMobile("5555555555");
        testPatient.setName("Test Patient");
        testPatient.setRole(Role.PATIENT);
        testPatient.setActive(true);
        testPatient = userRepository.save(testPatient);
    }

    @Test
    void testProcessServiceCompletionEarnings() {
        // Arrange - Create a completed booking
        MedicalService service = new MedicalService();
        service.setName("Test Service");
        service.setBasePrice(new BigDecimal("1000.00"));
        service.setIsActive(true);
        service = serviceRepository.save(service);

        Booking booking = Booking.builder()
                .bookingNo("TEST-BOOK-001")
                .patientUser(testPatient)
                .assignedNurseUser(testNurse)
                .bookingStatus("COMPLETED")
                .paymentStatus("PAID")
                .bookingDate(LocalDate.now())
                .bookingTime("10:00 AM")
                .latitude(28.4595)
                .longitude(77.0266)
                .rawAddress("Test Address")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        BookingItem item = BookingItem.builder()
                .booking(booking)
                .service(service)
                .itemName(service.getName())
                .priceCharged(service.getBasePrice())
                .build();

        booking.setSelectedItems(List.of(item));
        booking = bookingRepository.save(booking);

        // Act
        walletService.processServiceCompletionEarnings(booking);

        // Assert
        NurseWallet wallet = walletRepository.findByNurseUserMobile(testNurse.getMobile()).orElse(null);
        assertNotNull(wallet);
        assertTrue(wallet.getCurrentBalance().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(wallet.getTotalEarned().compareTo(BigDecimal.ZERO) > 0);
        
        // Platform fee is 10% + 18% GST on fee = 11.8% total deduction
        // Net should be approximately 882 (1000 - 118)
        BigDecimal expectedNet = new BigDecimal("882.00");
        assertEquals(0, wallet.getCurrentBalance().compareTo(expectedNet));
    }

    @Test
    void testGetNurseWalletDashboard() {
        // Arrange - Create wallet first
        NurseWallet wallet = NurseWallet.builder()
                .nurseUser(testNurse)
                .currentBalance(new BigDecimal("500.00"))
                .totalEarned(new BigDecimal("1000.00"))
                .negativeLimit(new BigDecimal("500.00"))
                .isSuspended(false)
                .build();
        walletRepository.save(wallet);

        // Act
        NurseWalletSummary summary = walletService.getNurseWalletDashboard(testNurse.getMobile());

        // Assert
        assertNotNull(summary);
        assertEquals(new BigDecimal("500.00"), summary.getWalletBalance());
        assertEquals(new BigDecimal("1000.00"), summary.getLifetimeEarnings());
    }
}

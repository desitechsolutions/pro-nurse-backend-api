package com.pronurse.booking;

import com.pronurse.booking.dto.CreateBookingRequest;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.repository.BookingRepository;
import com.pronurse.booking.service.BookingService;
import com.pronurse.auth.entity.User;
import com.pronurse.auth.model.Role;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.catalog.entity.MedicalService;
import com.pronurse.catalog.repository.MedicalServiceRepository;
import com.pronurse.nurse.entity.NurseProfile;
import com.pronurse.nurse.repository.NurseProfileRepository;
import com.pronurse.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
class BookingServiceTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MedicalServiceRepository serviceRepository;

    @Autowired
    private NurseProfileRepository nurseProfileRepository;

    private User testPatient;
    private User testNurse;
    private MedicalService testService;

    @BeforeEach
    void setUp() {
        // Create test patient
        testPatient = new User();
        testPatient.setMobile("7777777777");
        testPatient.setName("Test Patient");
        testPatient.setRole(Role.PATIENT);
        testPatient.setActive(true);
        testPatient = userRepository.save(testPatient);

        // Create test nurse
        testNurse = new User();
        testNurse.setMobile("6666666666");
        testNurse.setName("Test Nurse");
        testNurse.setRole(Role.NURSE);
        testNurse.setActive(true);
        testNurse = userRepository.save(testNurse);

        // Create nurse profile
        NurseProfile nurseProfile = new NurseProfile();
        nurseProfile.setUser(testNurse);
        nurseProfile.setNurseId("NUR-TEST-001");
        nurseProfile.setQualification("BSc Nursing");
        nurseProfile.setExperience("5");
        nurseProfile.setSpecialization("General");
        nurseProfile.setLatitude(28.4595);
        nurseProfile.setLongitude(77.0266);
        nurseProfile.setVerified(true);
        nurseProfile.setOnDuty(true);
        nurseProfile.setVerificationStatus("APPROVED");
        nurseProfileRepository.save(nurseProfile);

        // Create test service
        testService = new MedicalService();
        testService.setName("Test Service");
        testService.setDescription("Test Description");
        testService.setBasePrice(new BigDecimal("500.00"));
        testService.setIsActive(true);
        testService = serviceRepository.save(testService);
    }

    @Test
    void testCreateBooking() {
        // Arrange
        CreateBookingRequest request = new CreateBookingRequest();
        request.setBookingDate(LocalDate.now().plusDays(1).toString());
        request.setBookingTime("10:00 AM");
        request.setLatitude("28.4595");
        request.setLongitude("77.0266");
        request.setAddress("Test Address, Delhi");
        request.setRemarks("Test booking");
        request.setSelectedServiceIds(List.of(testService.getId()));

        // Act
        String bookingNo = bookingService.createMultiItemBooking(testPatient.getMobile(), request);

        // Assert
        assertNotNull(bookingNo);
        assertTrue(bookingNo.startsWith("BOOK-"));

        Booking booking = bookingRepository.findByBookingNo(bookingNo).orElse(null);
        assertNotNull(booking);
        assertEquals("PENDING", booking.getBookingStatus());
        assertEquals(testPatient.getId(), booking.getPatientUser().getId());
        assertEquals(1, booking.getSelectedItems().size());
    }

    @Test
    void testGetPatientBookingHistory() {
        // Arrange - Create a booking first
        CreateBookingRequest request = new CreateBookingRequest();
        request.setBookingDate(LocalDate.now().plusDays(1).toString());
        request.setBookingTime("10:00 AM");
        request.setLatitude("28.4595");
        request.setLongitude("77.0266");
        request.setAddress("Test Address");
        request.setRemarks("Test");
        request.setSelectedServiceIds(List.of(testService.getId()));
        bookingService.createMultiItemBooking(testPatient.getMobile(), request);

        // Act
        List<Booking> history = bookingService.getPatientBookingHistory(testPatient.getMobile());

        // Assert
        assertNotNull(history);
        assertFalse(history.isEmpty());
        assertEquals(1, history.size());
    }
}

// Made with Bob

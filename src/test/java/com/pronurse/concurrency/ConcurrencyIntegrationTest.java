package com.pronurse.concurrency;

import com.pronurse.auth.entity.User;
import com.pronurse.auth.model.Role;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.entity.BookingAssignment;
import com.pronurse.booking.repository.BookingAssignmentRepository;
import com.pronurse.booking.repository.BookingRepository;
import com.pronurse.booking.service.BookingService;
import com.pronurse.config.TestConfig;
import com.pronurse.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class ConcurrencyIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingAssignmentRepository assignmentRepository;

    private User nurseUser1;
    private User nurseUser2;
    private Booking booking;

    @BeforeEach
    void setUp() {
        assignmentRepository.deleteAll();
        bookingRepository.deleteAll();
        userRepository.deleteAll();

        User patient = TestDataFactory.createUser("9876543210", Role.PATIENT);
        patient = userRepository.save(patient);

        nurseUser1 = TestDataFactory.createUser("9876543211", Role.NURSE);
        nurseUser1 = userRepository.save(nurseUser1);

        nurseUser2 = TestDataFactory.createUser("9876543212", Role.NURSE);
        nurseUser2 = userRepository.save(nurseUser2);

        booking = TestDataFactory.createBooking(patient, "BOOK-CONC-999");
        booking.setLatitude(28.6289);
        booking.setLongitude(77.3649);
        booking = bookingRepository.save(booking);
    }

    @Test
    void testConcurrentBookingAcceptance() throws InterruptedException {
        // Create 2 offers
        BookingAssignment assignment1 = TestDataFactory.createBookingAssignment(booking, nurseUser1);
        assignmentRepository.save(assignment1);

        BookingAssignment assignment2 = TestDataFactory.createBookingAssignment(booking, nurseUser2);
        assignmentRepository.save(assignment2);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        tasks.add(() -> {
            latch.await();
            try {
                bookingService.processNurseResponse(booking.getBookingNo(), nurseUser1.getMobile(), true, null);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
            return null;
        });

        tasks.add(() -> {
            latch.await();
            try {
                bookingService.processNurseResponse(booking.getBookingNo(), nurseUser2.getMobile(), true, null);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
            return null;
        });

        List<Future<Void>> futures = new ArrayList<>();
        for (var task : tasks) {
            futures.add(executor.submit(task));
        }

        latch.countDown(); // Trigger threads simultaneously

        for (var future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                // ignore
            }
        }

        executor.shutdown();

        // Exactly one nurse must have successfully accepted the booking
        assertEquals(1, successCount.get());
        assertEquals(1, failureCount.get());

        // Refresh and check status is ACCEPTED
        Booking updated = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals("ACCEPTED", updated.getBookingStatus());
    }
}

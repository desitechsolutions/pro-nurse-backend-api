package com.pronurse.booking;

import tools.jackson.databind.ObjectMapper;
import com.pronurse.auth.entity.User;
import com.pronurse.auth.model.Role;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.auth.security.JwtUtil;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.repository.BookingRepository;
import com.pronurse.booking.service.RazorpayClientProvider;
import com.pronurse.config.TestConfig;
import com.pronurse.catalog.entity.MedicalService;
import com.pronurse.catalog.repository.MedicalServiceRepository;
import com.pronurse.util.TestDataFactory;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@AutoConfigureMockMvc
@Transactional
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private MedicalServiceRepository serviceRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RazorpayClientProvider razorpayClientProvider;

    private User patientUser;
    private Booking booking;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        bookingRepository.deleteAll();
        serviceRepository.deleteAll();
        userRepository.deleteAll();

        patientUser = TestDataFactory.createUser("9876543210", Role.PATIENT);
        patientUser = userRepository.save(patientUser);

        token = jwtUtil.generateAccessToken(patientUser);

        booking = TestDataFactory.createBooking(patientUser, "BOOK-PAY-111");
        booking = bookingRepository.save(booking);

        MedicalService service = TestDataFactory.createMedicalService("Standard Nursing", BigDecimal.valueOf(1000.00));
        service = serviceRepository.save(service);

        com.pronurse.booking.entity.BookingItem item = TestDataFactory.createBookingItem(booking, service);
        booking.getSelectedItems().add(item);
        booking = bookingRepository.save(booking);

        // Setup Razorpay client mock
        RazorpayClient mockClient = mock(RazorpayClient.class);
        mockClient.orders = mock(OrderClient.class);
        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("rzp_order_mock999");
        when(mockClient.orders.create(any(JSONObject.class))).thenReturn(mockOrder);

        when(razorpayClientProvider.getClient(any(), any())).thenReturn(mockClient);
    }

    @Test
    void testCreateRazorpayOrderSuccess() throws Exception {
        Map<String, String> body = Map.of("bookingId", booking.getBookingNo());

        mockMvc.perform(post("/api/payment/create-order")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value("rzp_order_mock999"));

        // Verify order ID was updated in the booking
        Booking updated = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals("rzp_order_mock999", updated.getRazorpayOrderId());
    }

    @Test
    void testVerifyRazorpayPaymentSignatureFailure() throws Exception {
        Map<String, String> body = Map.of(
                "bookingId", booking.getBookingNo(),
                "paymentId", "pay_12345",
                "orderId", "rzp_order_mock999",
                "signature", "invalid_signature"
        );

        // This will call Razorpay's Utils.verifySignature which will fail because signature is invalid
        mockMvc.perform(post("/api/payment/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Payment verification failed: Invalid payment signature detected!"));
    }
}

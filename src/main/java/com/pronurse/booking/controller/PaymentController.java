package com.pronurse.booking.controller;

import com.pronurse.booking.service.BookingService;
import com.pronurse.common.payload.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final BookingService bookingService;

    public PaymentController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/create-order")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOrder(@RequestBody Map<String, String> body) {
        var orderDetails = bookingService.createRazorpayOrder(body.get("bookingId"));
        return ResponseEntity.ok(new ApiResponse<>(true, "Razorpay checkout order token mapped", orderDetails));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Void>> verifyCheckout(@RequestBody Map<String, String> body) {
        bookingService.verifyRazorpayPayment(
                body.get("bookingId"),
                body.get("paymentId"),
                body.get("orderId"),
                body.get("signature")
        );
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment securely processed, status verified successfully.", null));
    }
}
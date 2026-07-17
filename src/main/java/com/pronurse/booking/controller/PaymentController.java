package com.pronurse.booking.controller;

import com.pronurse.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@Tag(
        name = "08. Payment Processing",
        description = "Payment processing APIs for creating Razorpay orders and verifying payments")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentController {

    private final BookingService bookingService;

    public PaymentController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/create-order")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Create payment order", description = "Creates a Razorpay payment order for the specified booking ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Razorpay order created successfully", content = @Content(schema = @Schema(implementation = com.pronurse.common.payload.ApiResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid booking ID or payment setup failure"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Booking not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.pronurse.common.payload.ApiResponse<Map<String, Object>>> createOrder(@RequestBody Map<String, String> body) {
        var orderDetails = bookingService.createRazorpayOrder(body.get("bookingId"));
        return ResponseEntity.ok(new com.pronurse.common.payload.ApiResponse<>(true, "Razorpay checkout order token mapped", orderDetails));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Verify checkout payment", description = "Verifies Razorpay payment signature and updates the booking payment status.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment verified successfully", content = @Content(schema = @Schema(implementation = com.pronurse.common.payload.ApiResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid payment signature or verification failure"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.pronurse.common.payload.ApiResponse<Void>> verifyCheckout(@RequestBody Map<String, String> body) {
        bookingService.verifyRazorpayPayment(
                body.get("bookingId"),
                body.get("paymentId"),
                body.get("orderId"),
                body.get("signature")
        );
        return ResponseEntity.ok(new com.pronurse.common.payload.ApiResponse<>(true, "Payment securely processed, status verified successfully.", null));
    }
}
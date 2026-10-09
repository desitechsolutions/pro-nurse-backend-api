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
@Tag(name = "09. Payment Gateway (Razorpay)", description = "Checkout order creation, Razorpay webhook signature verification, and automated refund tracking.")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentController {

    private final BookingService bookingService;

    public PaymentController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/create-order")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(
        summary = "Create Razorpay payment order",
        description = "Creates a Razorpay checkout payment order token for the specified booking number.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Booking ID reference payload",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\n  \"bookingId\": \"PN-TEST-BOOK-001\"\n}"
                )
            )
        )
    )
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
    @Operation(
        summary = "Verify Razorpay checkout payment",
        description = "Verifies the cryptographic payment signature returned by the Razorpay mobile/web checkout SDK and updates booking payment status to PAID.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Razorpay payment verification signature tokens",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\n  \"bookingId\": \"PN-TEST-BOOK-001\",\n  \"orderId\": \"order_DBJOWzybf0sJbb\",\n  \"paymentId\": \"pay_29QQoUBcxrAMg6\",\n  \"signature\": \"9ef4dffbfdbf42f718f7176462c2f32a747d83d020d57e90c75edd4dced6aa29\"\n}"
                )
            )
        )
    )
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
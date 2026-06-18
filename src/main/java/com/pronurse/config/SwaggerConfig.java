package com.pronurse.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pro Nurse Backend API")
                        .version("1.0.0")
                        .description("""
                                # Pro Nurse Healthcare Platform API
                                
                                A comprehensive healthcare service platform connecting patients with professional nurses for home-based medical care.
                                
                                ## Key Features
                                - 🔐 OTP-based Authentication with JWT
                                - 👤 Patient & Nurse Profile Management
                                - 📅 Intelligent Booking & Dispatch System
                                - 💰 Integrated Payment Gateway (Razorpay)
                                - ⭐ Review & Rating System
                                - 💳 Nurse Wallet & Earnings Management
                                - 📍 Real-time Location Tracking
                                - 🔔 WebSocket Real-time Notifications
                                - 👨‍💼 Admin Portal with Analytics
                                
                                ## Authentication Flow
                                1. Send OTP to mobile number
                                2. Verify OTP and receive JWT access token
                                3. Use access token in Authorization header for protected endpoints
                                4. Refresh token automatically rotates via HttpOnly cookies
                                
                                ## User Roles
                                - **PATIENT**: Book services, manage profile, submit reviews
                                - **NURSE**: Accept bookings, update location, manage earnings
                                - **ADMIN**: Verify nurses, manage services, moderate content
                                
                                ## Booking Flow
                                1. Patient browses service catalog
                                2. Patient creates booking with location
                                3. System finds nearest available nurse
                                4. Nurse receives real-time notification (30s to respond)
                                5. Nurse accepts/rejects booking
                                6. If rejected/timeout, system cascades to next nurse
                                7. Patient makes payment via Razorpay
                                8. Nurse completes service
                                9. Patient submits review
                                10. Earnings credited to nurse wallet
                                
                                ## Payment Flow
                                - Platform Fee: 10% + 18% GST = 11.8% total deduction
                                - Net earnings credited to nurse wallet
                                - Nurses can request payouts
                                
                                ## Technical Details
                                - Base URL: http://localhost:8080
                                - Authentication: Bearer JWT Token
                                - Rate Limiting: 3 OTP requests per 5 minutes
                                - File Upload: Max 20MB per file, 50MB per request
                                """)
                        .contact(new Contact()
                                .name("Biruma Technology Solutions Pvt. Ltd.")
                                .url("https://www.desitechsolutions.com")
                                .email("support@desitechsolutions.com"))
                        .license(new License()
                                .name("Proprietary - All rights reserved by Biruma Technology Solutions Pvt. Ltd.")
                                .url("https://www.desitechsolutions.com/terms-of-service")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://pronurse-api-0-1-0.onrender.com")
                                .description("Production Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from /api/auth/otp/verify endpoint")));
    }
}



package com.pronurse.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
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
                        .title("Pro Nurse Healthcare Platform API")
                        .version("1.0.0")
                        .description("""
                                # Pro Nurse Healthcare Platform API Specification
                                
                                A comprehensive healthcare service platform connecting patients with verified professional nurses for home-based medical care.
                                
                                ## 🔐 Authentication Architecture
                                - **OTP Workflow**: `POST /api/auth/otp/send` ➔ `POST /api/auth/otp/verify` (Returns Access Token + Refresh Token).
                                - **Password Registration**: `POST /api/auth/register` (Supports direct patient/nurse account provisioning).
                                - **Silent Token Refresh**: `POST /api/auth/refresh` (Accepts HttpOnly cookie `refreshToken` or JSON body `{"refreshToken": "..."}`).
                                - **Session Invalidation**: `POST /api/auth/logout` (Revokes refresh token in database and wipes cookies).
                                - **Default Test OTP**: In development/testing mode, use OTP `123456`.
                                
                                ## 👥 User Roles & Access Control
                                - **PATIENT**: Browse service catalog, book nurse visits, upload prescriptions, view medical reports, track nurse en-route, make payments, and write reviews.
                                - **NURSE**: Toggle on-duty availability, accept/reject booking offers, start service (`IN_PROGRESS`), complete visits, stream live GPS coordinates, manage wallet earnings, and request payouts.
                                - **ADMIN**: Verify nurse onboarding documents, manage service catalog, moderate reviews, review system KPIs, and oversee bookings.
                                
                                ## 🔄 Booking Lifecycle & Chained Dispatch
                                1. **Catalog & Creation**: Patient selects medical services and creates booking with GPS coordinates.
                                2. **Chained Dispatch Algorithm**: Finds nearest verified on-duty nurses within 15 km with sufficient wallet balance (not suspended).
                                3. **Nurse Ringing**: Candidate receives offer via WebSocket (`/queue/nurse/{mobile}`) with a 30-second window to accept.
                                4. **Cascading / Timeout**: If rejected or timeout expires, system auto-cascades to the next nearest practitioner.
                                5. **No Nurse Fallback**: If all candidates are exhausted, status updates to `NO_NURSE_AVAILABLE` and alerts are broadcast to patient and admin channels.
                                6. **Auto-Expiry**: Unassigned bookings older than 30 minutes are automatically transitioned to `EXPIRED` by background daemon.
                                7. **Execution**: Nurse navigates (`ACCEPTED`), starts service (`IN_PROGRESS`), and completes booking (`COMPLETED`).
                                8. **Cancellation & Tiered Refunds**:
                                   - &ge; 24 hours prior: 100% Full Refund (`REFUND_INITIATED_FULL`)
                                   - 2 to 24 hours prior: 50% Partial Refund (`REFUND_INITIATED_PARTIAL`)
                                   - < 2 hours prior: 0% Forfeited (`REFUND_FORFEITED`)
                                
                                ## 💳 Economics & Nurse Wallet
                                - **Platform Commission**: 10% base fee.
                                - **GST**: 18% on platform fee (1.8% of gross, giving 11.8% total deduction).
                                - **Net Earning**: 88.2% credited directly to nurse wallet upon service completion.
                                - **Debt Reminders**: Daily daemon notifies nurses whose negative wallet balance exceeds threshold (< -100).
                                
                                ## 📍 Real-Time Telemetry & WebSockets
                                - **STOMP Endpoint**: `/ws` (with SockJS fallback).
                                - **Nurse Live Tracking**: REST `POST /api/nurse/location` or STOMP `/app/track/location` relays coordinates to `/topic/tracking/{bookingNo}`.
                                - **Alert Channels**: `/queue/patient/{mobile}`, `/queue/nurse/{mobile}`, `/topic/admin/alerts`.
                                
                                ## 📁 File Storage & Document Viewing
                                - Prescriptions: `/api/files/prescription/{filename}` (Inline PDF/Image viewer).
                                - Patient Medical Reports: `/api/files/medical-report/{patientUserId}` (Protected medical history).
                                - Profile Avatars: `/api/nurse/profile/avatar/{filename}` & `/api/files/profile-image/{userId}`.
                                - Nurse Onboarding Documents: `/api/nurse/onboarding/documents/{docId}/view` & `/api/admin/onboarding/documents/{docId}/download`.
                                """)
                        .contact(new Contact()
                                .name("DesiTech Solutions Engineering")
                                .url("https://www.desitechsolutions.com")
                                .email("support@desitechsolutions.com"))
                        .license(new License()
                                .name("Proprietary - DesiTech Solutions Pvt. Ltd.")
                                .url("https://www.desitechsolutions.com/terms-of-service")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server (Port " + serverPort + ")"),
                        new Server()
                                .url("https://pronurse-api-0-1-0.onrender.com")
                                .description("Staging / Production Cloud Server")
                ))
                .tags(List.of(
                        new Tag().name("01. Authentication & Security").description("User registration, mobile OTP verification, password login, JWT token rotation, and logout operations."),
                        new Tag().name("02. Patient Profile Management").description("Patient personal profile synchronization, avatar uploads, and medical report attachments."),
                        new Tag().name("03. Nurse Profile Management").description("Practitioner profile details, qualifications, experience, specialties, and on-duty availability toggles."),
                        new Tag().name("04. Nurse Discovery & Search").description("Public nurse directory search with filtering by keyword, city, gender, rating, and availability date."),
                        new Tag().name("05. Nurse Tracking & Live Telemetry").description("Real-time GPS coordinate streaming, location synchronization, and patient tracking map relays."),
                        new Tag().name("06. Patient Booking & Dispatch").description("Service catalog browsing, booking creation, prescription uploads, and active booking details."),
                        new Tag().name("07. Mobile Nurse Actions (Flutter)").description("Dedicated mobile app nurse actions: accept offers (30s window), reject with reason, start service (IN_PROGRESS), complete, and live GPS streaming."),
                        new Tag().name("08. Web Nurse Booking Actions").description("Web portal nurse booking actions: accept, reject, complete bookings, and retrieve operational dashboard feeds."),
                        new Tag().name("09. Payment Gateway (Razorpay)").description("Checkout order creation, Razorpay webhook signature verification, and automated refund tracking."),
                        new Tag().name("10. Booking Management & Cancellation").description("Booking cancellations with automated tiered refund policy calculation, rescheduling, and status transitions."),
                        new Tag().name("11. Booking History & Filtering").description("Paginated booking history with multi-attribute filtering, status filters, and CSV/PDF report exports for patients and nurses."),
                        new Tag().name("12. Emergency SOS & Quick Care").description("Priority emergency booking creation with broadcast dispatch to the 5 nearest available practitioners."),
                        new Tag().name("13. Nurse Wallet & Payout Management").description("Practitioner earnings ledger, fee deductions (10% + 18% GST), wallet balances, and bank payout requests."),
                        new Tag().name("14. Patient Reviews & Feedback").description("Post-service ratings and review submission for nurse practitioners."),
                        new Tag().name("15. Favorite Nurses (Quick Booking)").description("Bookmark preferred nurses and perform direct instant bookings bypassing standard dispatch algorithms."),
                        new Tag().name("16. Nurse Onboarding & Documents").description("Practitioner document management: upload required credentials, track review progress, replace rejected files, and submit for verification."),
                        new Tag().name("17. Admin Onboarding Review").description("Administrative verification workflow: review uploaded practitioner documents (approve, reject with notes), audit logs, and finalize onboarding."),
                        new Tag().name("18. Admin Portal & System Oversight").description("Administrative dashboard, system health KPI metrics, practitioner verification, service catalog management, and user access moderation."),
                        new Tag().name("19. Emergency Response Analytics").description("Response time statistics, acceptance rates, and date-range telemetry analytics for emergency SOS calls."),
                        new Tag().name("20. Push Notifications & Preferences").description("FCM device token registration, notification preferences configuration, and token cleanup."),
                        new Tag().name("21. Secure File & Document Management").description("Secure authenticated streaming and inline viewing of prescriptions, profile avatars, medical reports, and uploaded documents.")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT access token. (Bearer prefix added automatically). Obtain token via /api/auth/otp/verify or /api/auth/register.")));
    }
}

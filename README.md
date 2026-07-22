# 🏥 Pro Nurse Backend API

A comprehensive healthcare platform backend API connecting patients with qualified nurses for home healthcare services. Built with Spring Boot 4.0.6, featuring real-time location tracking, payment integration, emergency SOS services, and push notifications.

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Technology Stack](#-technology-stack)
- [Project Structure](#-project-structure)
- [Installation & Getting Started](#-installation--getting-started)
- [Running the Application](#-running-the-application)
- [Running Tests](#-running-tests)
- [API Documentation](#-api-documentation)
- [Database Schema](#-database-schema)
- [Deployment Notes](#-deployment-notes)
- [Development Workflow](#-development-workflow)
- [Troubleshooting](#-troubleshooting)
- [Support & Footer](#-support--company-information)

---

## 🎯 Overview

Pro Nurse is a production-ready home healthcare booking system designed and maintained by **Biruma Technology Solutions Pvt Ltd.**
It provides:
- **Patients** the ability to book qualified nurses for specialized services.
- **Nurses** the ability to accept/reject booking offers, track earnings, and toggle duty status.
- **Admins** a portal to verify nurse credentials, manage services, and monitor operations.

---

## ✨ Features

### 🔐 Authentication & Authorization
- OTP-based login (via mobile verification)
- JWT access tokens and secure HttpOnly cookie refresh token rotation
- Role-based authorization: `PATIENT`, `NURSE`, and `ADMIN`
- Rate-limiting mechanisms on OTP requests

### 👤 User Management
- **Patient Profile**: Demographic data, chronic illness registry, and medical file uploads.
- **Nurse Profile**: Verification tracking, experience records, location coordinate caching.

### 📅 Booking & Dispatch Engine
- Multi-service cart checkout.
- Spatial-matching dispatch using PostgreSQL Haversine native queries to alert the closest nurse.
- Booking acceptance, rejection, and completion workflows.
- Booking logs exported to PDF or CSV.

### 🚨 Emergency SOS
- 1-click emergency dispatch to the nearest available nurse.
- Emergency contact notifications and SOS activity analytics.

### ⭐ Favorite Nurses
- Patient-nurse favorites list mapping.
- Instant booking matching with favorite nurses.

### 📍 Real-time Tracking
- Live location tracking of assigned nurses using WebSocket (STOMP).

### 🔔 Push Notifications
- Firebase Cloud Messaging (FCM) integration for real-time app alert synchronization.

### 💰 Wallet & Payments
- Razorpay order creation and checksum validation.
- Platform fee computation (10% platform commission + 18% GST = 11.8% gross deduction).
- Payout withdrawals to nurse bank accounts.

---

## 🛠️ Technology Stack

- **Framework**: Spring Boot 4.0.6 (as configured in `pom.xml`)
- **Language**: Java 21
- **Database**: PostgreSQL (production), H2 (in-memory test profile)
- **Database Migration**: Flyway
- **Real-Time Messaging**: Spring WebSockets (STOMP protocol)
- **Push Notification**: Firebase Cloud Messaging (FCM)
- **Payment Gateway**: Razorpay Java SDK
- **Security**: Spring Security & JSON Web Token (JJWT)
- **Documentation**: Springdoc-OpenAPI / Swagger UI
- **Build Tool**: Maven

---

## 📁 Project Structure

```text
pro-nurse-backend-api/
├── src/
│   ├── main/
│   │   ├── java/com/pronurse/
│   │   │   ├── admin/              # Admin-specific controllers & services
│   │   │   ├── analytics/          # Emergency SOS dashboard metrics
│   │   │   ├── auth/               # OTP, JWT Filter, registration, and logout
│   │   │   ├── booking/            # Booking engine, dispatch, and history
│   │   │   ├── catalog/            # Service catalog
│   │   │   ├── common/             # Global error mapping, file, SMS & Twilio utilities
│   │   │   ├── config/             # Swagger and Security configuration
│   │   │   ├── favorites/          # Patient favorites system
│   │   │   ├── notification/       # Firebase FCM integrations
│   │   │   ├── nurse/              # Nurse profile tracking
│   │   │   ├── patient/            # Patient profile management
│   │   │   ├── review/             # Ratings and feedback
│   │   │   └── wallet/             # Wallet, payouts, and transaction logging
│   │   └── resources/
│   │       ├── db/migration/       # Flyway database schema files
│   │       ├── application.properties
│   │       └── application-local.properties
│   └── test/                       # Integration and Unit tests
├── Dockerfile
├── pom.xml
└── README.md
```

---

## 🚀 Installation & Getting Started

### Prerequisites
- **Java**: JDK 21
- **Maven**: 3.8+
- **Database**: PostgreSQL 14+
- **FCM**: Firebase admin service account configuration
- **Razorpay**: Developer API credentials

### Step-by-Step Setup

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd pro-nurse-backend-api
   ```

2. **Configure Database**
   Log in to PostgreSQL and create the database:
   ```sql
   CREATE DATABASE pronurse_db;
   CREATE USER pronurse_user WITH PASSWORD 'your_secure_password';
   GRANT ALL PRIVILEGES ON DATABASE pronurse_db TO pronurse_user;
   ```

3. **Configure Local Settings**
   Create `src/main/resources/application-local.properties` with the following variables:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/pronurse_db
   spring.datasource.username=pronurse_user
   spring.datasource.password=your_secure_password
   
   jwt.secret=your-256-bit-secret-key-here-minimum-32-characters
   jwt.expiration=86400000
   
   razorpay.key.id=rzp_test_your_key_id
   razorpay.key.secret=your_razorpay_secret
   
   firebase.credentials.path=src/main/resources/firebase-credentials.json
   sms.provider=mock
   ```

4. **Add Firebase Credentials**
   Download the Firebase Service Account private key (`firebase-credentials.json`) from the Firebase Console and save it to `src/main/resources/firebase-credentials.json`.

---

## 🏃 Running the Application

### Using Maven
```bash
mvn clean compile
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Once started, the API base URL is: `http://localhost:8080`

---

## 🧪 Running Tests

The test suite runs against an in-memory H2 database with transactional isolation.

```bash
# Run the entire test suite
mvn test

# Run a specific integration test
mvn test -Dtest=MobileNurseActionControllerTest
```
* **Current Status:** All **47 tests** are compiling and passing successfully.

---

## 📚 API Documentation

### Swagger UI
Interact with endpoints directly via Swagger UI at:
```
http://localhost:8080/swagger-ui/index.html
```

### Authentication in Swagger
1. Click the **Authorize** lock button in the top right.
2. Enter the bearer token (JWT) generated via `POST /api/auth/otp/verify`.
3. Calls will include the `Authorization: Bearer <token>` header automatically.

---

## 🗄️ Database Schema

Schema migrations are managed by Flyway. The database consists of 16 tables:
1. `users` - Base security profile.
2. `patient_profiles` - Patient chronic details.
3. `nurse_profiles` - Verification statuses and experience metrics.
4. `medical_services` - Service catalog.
5. `bookings` - Service orders.
6. `booking_selected_items` - Booking line items.
7. `booking_assignments` - Nurse offers tracking.
8. `nurse_reviews` - Patient feedback.
9. `nurse_wallets` - Balances.
10. `wallet_transactions` - Wallet history logs.
11. `payout_requests` - Withdrawal requests.
12. `refresh_tokens` - JWT refresh token rotations.
13. `emergency_contacts` - SOS contacts.
14. `favorite_nurses` - Favorite nurse mapping.
15. `fcm_tokens` - Firebase registrations.
16. `notification_preferences` - Channel filters.

---

## 🚢 Deployment Notes

### Containerization (Docker)
Build the Docker image:
```bash
docker build -t pronurse-api:latest .
```

Run the container:
```bash
docker run -d -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host:5432/pronurse_db \
  -e JWT_SECRET=your_jwt_secret \
  -e RAZORPAY_KEY_ID=your_key_id \
  -e RAZORPAY_KEY_SECRET=your_key_secret \
  pronurse-api:latest
```

---

## 🤝 Development Workflow

Refer to **[DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)** for instructions regarding architecture, lifecycles, and templates on adding endpoints, entities, or services.

Refer to **[COMPANY_POLICY.md](COMPANY_POLICY.md)** for branching models, pull requests, and review standards.

---

## 📞 Support & Company Information

This repository is developed and maintained by the engineering team at:

**Company:** Biruma Technology Solutions Pvt Ltd.  
**Website:** [desitechsolutions.com](https://desitechsolutions.com)  
**Support Email:** [info@desitechsolutions.com](mailto:info@desitechsolutions.com)  

Copyright © 2026 Biruma Technology Solutions Pvt Ltd. All rights reserved.

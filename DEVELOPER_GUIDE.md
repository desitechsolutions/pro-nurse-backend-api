# Pro Nurse Backend API - Complete Developer Guide

**Version**: 1.0.0  
**Last Updated**: June 15, 2026  
**Base URL**: `http://localhost:8080` (local) | `https://api.pronurse.com` (production)

---

## Table of Contents

1. [Overview](#overview)
2. [Getting Started](#getting-started)
3. [Authentication & Authorization](#authentication--authorization)
4. [API Endpoints Reference](#api-endpoints-reference)
5. [WebSocket Integration](#websocket-integration)
6. [Push Notifications](#push-notifications)
7. [Mobile App Integration](#mobile-app-integration)
8. [Error Handling](#error-handling)
9. [Testing Guide](#testing-guide)
10. [Deployment Guide](#deployment-guide)

---

## Overview

Pro Nurse is a healthcare platform connecting patients with qualified nurses for home healthcare services. The backend API provides comprehensive functionality for booking management, real-time tracking, payments, and emergency services.

### Key Features

✅ **User Management**: Patient, Nurse, and Admin roles  
✅ **Booking System**: Multi-service booking with intelligent nurse dispatch  
✅ **Real-time Tracking**: Live location updates via WebSocket  
✅ **Payment Integration**: Razorpay payment gateway  
✅ **Wallet System**: Nurse earnings and payout management  
✅ **Emergency SOS**: Priority emergency booking system  
✅ **Push Notifications**: Firebase Cloud Messaging integration  
✅ **Reviews & Ratings**: Patient feedback system  
✅ **Favorite Nurses**: Save and book preferred nurses  
✅ **Export Features**: CSV/PDF booking history export  
✅ **Analytics Dashboard**: Real-time metrics and insights

### Technology Stack

- **Framework**: Spring Boot 3.4.5
- **Language**: Java 21
- **Database**: PostgreSQL (production), H2 (testing)
- **Authentication**: JWT (JSON Web Tokens)
- **Real-time**: WebSocket (STOMP protocol)
- **Payments**: Razorpay
- **Notifications**: Firebase Cloud Messaging (FCM)
- **Documentation**: Swagger/OpenAPI 3.0
- **Build Tool**: Maven 3.8+

---

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- PostgreSQL 14+ (for production)
- Firebase account (for push notifications)
- Razorpay account (for payments)

### Installation Steps

1. **Clone the repository**
```bash
git clone <repository-url>
cd pro-nurse-backend-api
```

2. **Configure Database**

Create PostgreSQL database:
```sql
CREATE DATABASE pronurse_db;
CREATE USER pronurse_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE pronurse_db TO pronurse_user;
```

3. **Configure Application Properties**

Create `src/main/resources/application-local.properties`:
```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/pronurse_db
spring.datasource.username=pronurse_user
spring.datasource.password=your_password

# JWT Configuration
jwt.secret=your-256-bit-secret-key-here-minimum-32-characters
jwt.expiration=86400000

# Razorpay Configuration
razorpay.key.id=rzp_test_your_key_id
razorpay.key.secret=your_razorpay_secret

# Firebase Configuration
firebase.credentials.path=src/main/resources/firebase-credentials.json

# SMS Configuration
sms.provider=mock
sms.api.key=your_sms_api_key

# File Upload
file.upload.dir=./uploads
```

4. **Add Firebase Credentials**

Download `firebase-credentials.json` from Firebase Console and place it in `src/main/resources/`

5. **Build the Project**
```bash
mvn clean install
```

6. **Run Database Migrations**
```bash
mvn flyway:migrate
```

7. **Run the Application**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

8. **Access Swagger UI**
```
http://localhost:8080/swagger-ui.html
```

---

## Authentication & Authorization

### User Roles

| Role | Description | Permissions |
|------|-------------|-------------|
| **PATIENT** | End users booking services | Create bookings, view history, add reviews, manage favorites |
| **NURSE** | Healthcare providers | Accept bookings, update location, manage wallet, view earnings |
| **ADMIN** | System administrators | Full access, analytics, user management, service management |

### Authentication Flow

#### 1. Request OTP

**Endpoint**: `POST /api/auth/request-otp`

**Request**:
```json
{
  "mobile": "9876543210",
  "role": "PATIENT"
}
```

**Response**:
```json
{
  "success": true,
  "message": "OTP sent successfully to 9876543210"
}
```

**Rate Limiting**: 3 requests per minute per mobile number

#### 2. Verify OTP

**Endpoint**: `POST /api/auth/verify-otp`

**Request**:
```json
{
  "mobile": "9876543210",
  "otp": "123456",
  "role": "PATIENT"
}
```

**Response**:
```json
{
  "success": true,
  "message": "OTP verified successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": 1,
      "mobile": "9876543210",
      "name": "John Doe",
      "email": "john@example.com",
      "role": "PATIENT",
      "active": true
    }
  }
}
```

#### 3. Refresh Token

**Endpoint**: `POST /api/auth/refresh-token`

**Request**:
```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
}
```

**Response**: Same as verify OTP response with new tokens

### Using JWT in Requests

All protected endpoints require JWT token in header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## API Endpoints Reference

### Authentication Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/request-otp` | Request OTP for login | No |
| POST | `/api/auth/verify-otp` | Verify OTP and get tokens | No |
| POST | `/api/auth/refresh-token` | Refresh access token | No |
| POST | `/api/auth/logout` | Logout user | Yes |

### Patient Profile Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/patient/profile` | Get patient profile | Yes (PATIENT) |
| PUT | `/api/patient/profile` | Update patient profile | Yes (PATIENT) |
| POST | `/api/patient/profile/upload-image` | Upload profile image | Yes (PATIENT) |
| POST | `/api/patient/profile/upload-report` | Upload medical report | Yes (PATIENT) |

### Nurse Profile Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/nurse/profile` | Get nurse profile | Yes (NURSE) |
| PUT | `/api/nurse/profile` | Update nurse profile | Yes (NURSE) |
| PUT | `/api/nurse/profile/duty-status` | Toggle on-duty status | Yes (NURSE) |
| GET | `/api/nurse/search` | Search nurses by criteria | Yes |
| GET | `/api/nurse/{nurseUserId}/public` | Get public nurse profile | Yes |

### Service Catalog Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/catalog/services` | Get all active services | Yes |
| GET | `/api/catalog/services/{id}` | Get service details | Yes |

### Booking Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/patient/bookings` | Create new booking | Yes (PATIENT) |
| GET | `/api/patient/bookings/history` | Get booking history | Yes (PATIENT) |
| GET | `/api/patient/bookings/{bookingNo}` | Get booking details | Yes |
| POST | `/api/patient/bookings/{bookingNo}/cancel` | Cancel booking | Yes (PATIENT) |
| POST | `/api/patient/bookings/{bookingNo}/reschedule` | Reschedule booking | Yes (PATIENT) |
| GET | `/api/patient/bookings/export/csv` | Export bookings to CSV | Yes (PATIENT) |
| GET | `/api/patient/bookings/export/pdf` | Export bookings to PDF | Yes (PATIENT) |

### Nurse Booking Actions

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/nurse/dashboard` | Get nurse dashboard | Yes (NURSE) |
| POST | `/api/nurse/bookings/{bookingNo}/accept` | Accept booking | Yes (NURSE) |
| POST | `/api/nurse/bookings/{bookingNo}/reject` | Reject booking | Yes (NURSE) |
| POST | `/api/nurse/bookings/{bookingNo}/start` | Start service | Yes (NURSE) |
| POST | `/api/nurse/bookings/{bookingNo}/complete` | Complete service | Yes (NURSE) |

### Emergency SOS Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/emergency/sos` | Create emergency booking | Yes (PATIENT) |
| POST | `/api/emergency/contacts` | Add emergency contact | Yes (PATIENT) |
| GET | `/api/emergency/contacts` | Get emergency contacts | Yes (PATIENT) |
| DELETE | `/api/emergency/contacts/{id}` | Delete emergency contact | Yes (PATIENT) |
| GET | `/api/admin/emergency/analytics` | Get emergency analytics | Yes (ADMIN) |

### Favorite Nurses Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/favorites/nurses/{nurseUserId}` | Add nurse to favorites | Yes (PATIENT) |
| DELETE | `/api/favorites/nurses/{nurseUserId}` | Remove from favorites | Yes (PATIENT) |
| GET | `/api/favorites/nurses` | Get favorite nurses | Yes (PATIENT) |
| POST | `/api/favorites/nurses/{nurseUserId}/book` | Book favorite nurse | Yes (PATIENT) |

### Review Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/reviews` | Add review for nurse | Yes (PATIENT) |
| GET | `/api/reviews/nurse/{nurseUserId}` | Get nurse reviews | Yes |
| GET | `/api/reviews/booking/{bookingNo}` | Get booking review | Yes |

### Wallet Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/nurse/wallet/summary` | Get wallet summary | Yes (NURSE) |
| GET | `/api/nurse/wallet/transactions` | Get transaction history | Yes (NURSE) |
| POST | `/api/nurse/wallet/payout` | Request payout | Yes (NURSE) |
| GET | `/api/nurse/wallet/payouts` | Get payout history | Yes (NURSE) |

### Payment Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/payments/create-order` | Create Razorpay order | Yes (PATIENT) |
| POST | `/api/payments/verify` | Verify payment | Yes (PATIENT) |

### Notification Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/notifications/fcm/register` | Register FCM token | Yes |
| DELETE | `/api/notifications/fcm/unregister` | Unregister FCM token | Yes |
| PUT | `/api/notifications/preferences` | Update notification preferences | Yes |
| GET | `/api/notifications/preferences` | Get notification preferences | Yes |

### Admin Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/admin/dashboard` | Get admin dashboard metrics | Yes (ADMIN) |
| GET | `/api/admin/nurses` | Get all nurses | Yes (ADMIN) |
| POST | `/api/admin/nurses/{nurseUserId}/verify` | Verify nurse | Yes (ADMIN) |
| PUT | `/api/admin/services/{id}` | Update service | Yes (ADMIN) |
| GET | `/api/admin/bookings` | Get all bookings | Yes (ADMIN) |
| POST | `/api/admin/reviews/{id}/moderate` | Moderate review | Yes (ADMIN) |

---

## WebSocket Integration

### Connection Setup

**WebSocket URL**: `ws://localhost:8080/ws-alerts`

### Topics

| Topic | Description | Subscribers |
|-------|-------------|-------------|
| `/topic/bookings` | Booking status updates | Patient, Nurse |
| `/topic/nurse-location` | Real-time nurse location | Patient |
| `/topic/emergency-alerts` | Emergency SOS alerts | Nurses, Admin |
| `/user/queue/notifications` | Personal notifications | All users |

### Message Formats

#### Booking Update
```json
{
  "bookingNo": "BOOK-ABC123",
  "status": "CONFIRMED",
  "nurseUserId": 2,
  "nurseName": "Jane Smith",
  "timestamp": "2026-06-15T10:30:00"
}
```

#### Location Update
```json
{
  "nurseUserId": 2,
  "latitude": 28.6139,
  "longitude": 77.2090,
  "timestamp": 1718456789000
}
```

#### Emergency Alert
```json
{
  "bookingNo": "BOOK-EMERGENCY-XYZ",
  "patientName": "John Doe",
  "latitude": 28.6139,
  "longitude": 77.2090,
  "emergencyDescription": "Patient fell and injured",
  "priority": "HIGH"
}
```

---

## Push Notifications

### Notification Types

| Type | Title | Body | Data |
|------|-------|------|------|
| BOOKING_CONFIRMED | "Booking Confirmed" | "Your booking {bookingNo} has been confirmed" | bookingNo, nurseUserId |
| NURSE_ASSIGNED | "Nurse Assigned" | "{nurseName} has been assigned to your booking" | bookingNo, nurseUserId |
| BOOKING_STARTED | "Service Started" | "Your nurse has started the service" | bookingNo |
| BOOKING_COMPLETED | "Service Completed" | "Service completed successfully" | bookingNo |
| EMERGENCY_ALERT | "Emergency SOS" | "Emergency booking created. Nurse on the way" | bookingNo, priority |
| PAYMENT_SUCCESS | "Payment Successful" | "Payment of ₹{amount} received" | bookingNo, amount |
| PAYOUT_PROCESSED | "Payout Processed" | "₹{amount} transferred to your account" | payoutId, amount |

### Notification Preferences

Users can control which notifications they receive:

```json
{
  "bookingUpdates": true,
  "promotions": false,
  "emergencyAlerts": true,
  "paymentUpdates": true
}
```

---

## Mobile App Integration

### Android Integration (Kotlin)

#### 1. Setup Dependencies

```gradle
dependencies {
    // Retrofit for API calls
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
    
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    
    // ViewModel and LiveData
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.6.2'
    
    // Firebase
    implementation platform('com.google.firebase:firebase-bom:32.3.1')
    implementation 'com.google.firebase:firebase-messaging-ktx'
    
    // WebSocket
    implementation 'com.github.NaikSoftware:StompProtocolAndroid:1.6.6'
    
    // Google Maps
    implementation 'com.google.android.gms:play-services-maps:18.1.0'
    implementation 'com.google.android.gms:play-services-location:21.0.1'
    
    // Secure Storage
    implementation 'androidx.security:security-crypto:1.1.0-alpha06'
}
```

#### 2. API Service Interface

```kotlin
interface ApiService {
    
    @POST("api/auth/request-otp")
    suspend fun requestOtp(@Body request: OtpRequest): ApiResponse<Unit>
    
    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: OtpVerificationRequest): ApiResponse<AuthResponse>
    
    @GET("api/patient/profile")
    suspend fun getPatientProfile(): ApiResponse<PatientProfile>
    
    @POST("api/patient/bookings")
    suspend fun createBooking(@Body request: CreateBookingRequest): ApiResponse<BookingResponse>
    
    @GET("api/patient/bookings/history")
    suspend fun getBookingHistory(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiResponse<Page<BookingHistory>>
    
    @POST("api/emergency/sos")
    suspend fun createEmergencySOS(@Body request: EmergencySOSRequest): ApiResponse<BookingResponse>
    
    @POST("api/favorites/nurses/{nurseUserId}")
    suspend fun addFavoriteNurse(@Path("nurseUserId") nurseUserId: Long): ApiResponse<Unit>
    
    @POST("api/reviews")
    suspend fun addReview(@Body request: AddReviewRequest): ApiResponse<Unit>
}
```

#### 3. Retrofit Client Setup

```kotlin
object RetrofitClient {
    
    private const val BASE_URL = "http://your-server-url:8080/"
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = TokenManager.getAccessToken()
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}
```

#### 4. Repository Pattern

```kotlin
class BookingRepository(private val apiService: ApiService) {
    
    suspend fun createBooking(request: CreateBookingRequest): Result<BookingResponse> {
        return try {
            val response = apiService.createBooking(request)
            if (response.success) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getBookingHistory(
        status: String? = null,
        page: Int = 0
    ): Result<Page<BookingHistory>> {
        return try {
            val response = apiService.getBookingHistory(status, page)
            if (response.success) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

#### 5. ViewModel Implementation

```kotlin
class BookingViewModel(
    private val bookingRepository: BookingRepository
) : ViewModel() {
    
    private val _bookingState = MutableLiveData<BookingState>()
    val bookingState: LiveData<BookingState> = _bookingState
    
    fun createBooking(request: CreateBookingRequest) {
        viewModelScope.launch {
            _bookingState.value = BookingState.Loading
            
            when (val result = bookingRepository.createBooking(request)) {
                is Result.Success -> {
                    _bookingState.value = BookingState.Success(result.data)
                }
                is Result.Failure -> {
                    _bookingState.value = BookingState.Error(result.exception.message ?: "Unknown error")
                }
            }
        }
    }
}

sealed class BookingState {
    object Loading : BookingState()
    data class Success(val booking: BookingResponse) : BookingState()
    data class Error(val message: String) : BookingState()
}
```

#### 6. WebSocket Integration

```kotlin
class WebSocketManager(private val serverUrl: String) {
    
    private var stompClient: StompClient? = null
    private val compositeDisposable = CompositeDisposable()
    
    fun connect(token: String) {
        val headers = listOf(
            StompHeader("Authorization", "Bearer $token")
        )
        
        stompClient = Stomp.over(
            Stomp.ConnectionProvider.OKHTTP,
            "$serverUrl/ws-alerts"
        )
        
        stompClient?.lifecycle()?.subscribe { lifecycleEvent ->
            when (lifecycleEvent.type) {
                LifecycleEvent.Type.OPENED -> {
                    Log.d("WebSocket", "Connected")
                    subscribeToTopics()
                }
                LifecycleEvent.Type.CLOSED -> {
                    Log.d("WebSocket", "Disconnected")
                }
                LifecycleEvent.Type.ERROR -> {
                    Log.e("WebSocket", "Error", lifecycleEvent.exception)
                }
            }
        }?.let { compositeDisposable.add(it) }
        
        stompClient?.connect(headers)
    }
    
    private fun subscribeToTopics() {
        // Subscribe to booking updates
        stompClient?.topic("/topic/bookings")?.subscribe { message ->
            val booking = Gson().fromJson(message.payload, BookingUpdate::class.java)
            EventBus.post(BookingUpdateEvent(booking))
        }?.let { compositeDisposable.add(it) }
        
        // Subscribe to location updates
        stompClient?.topic("/topic/nurse-location")?.subscribe { message ->
            val location = Gson().fromJson(message.payload, LocationUpdate::class.java)
            EventBus.post(LocationUpdateEvent(location))
        }?.let { compositeDisposable.add(it) }
    }
    
    fun sendLocation(latitude: Double, longitude: Double) {
        val location = LocationUpdate(latitude, longitude, System.currentTimeMillis())
        stompClient?.send("/app/nurse-location", Gson().toJson(location))?.subscribe()
    }
    
    fun disconnect() {
        compositeDisposable.clear()
        stompClient?.disconnect()
    }
}
```

#### 7. Firebase Messaging

```kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // Register token with backend
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitClient.apiService.registerFCMToken(
                    FCMTokenRequest(
                        token = token,
                        deviceType = "ANDROID",
                        deviceId = Settings.Secure.getString(
                            contentResolver,
                            Settings.Secure.ANDROID_ID
                        )
                    )
                )
            } catch (e: Exception) {
                Log.e("FCM", "Failed to register token", e)
            }
        }
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        remoteMessage.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "",
                body = notification.body ?: "",
                data = remoteMessage.data
            )
        }
    }
    
    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val notificationId = System.currentTimeMillis().toInt()
        
        val intent = when (data["type"]) {
            "BOOKING_CONFIRMED" -> {
                Intent(this, BookingDetailsActivity::class.java).apply {
                    putExtra("bookingNo", data["bookingNo"])
                }
            }
            "EMERGENCY_ALERT" -> {
                Intent(this, EmergencyTrackingActivity::class.java).apply {
                    putExtra("bookingNo", data["bookingNo"])
                }
            }
            else -> Intent(this, MainActivity::class.java)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }
}
```

### iOS Integration (Swift)

#### 1. API Service

```swift
class APIService {
    
    static let shared = APIService()
    private let baseURL = "http://your-server-url:8080"
    
    func requestOTP(mobile: String, role: String, completion: @escaping (Result<Void, Error>) -> Void) {
        let url = URL(string: "\(baseURL)/api/auth/request-otp")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body: [String: Any] = ["mobile": mobile, "role": role]
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(error))
                return
            }
            completion(.success(()))
        }.resume()
    }
    
    func verifyOTP(mobile: String, otp: String, role: String, completion: @escaping (Result<AuthResponse, Error>) -> Void) {
        let url = URL(string: "\(baseURL)/api/auth/verify-otp")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body: [String: Any] = ["mobile": mobile, "otp": otp, "role": role]
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(error))
                return
            }
            
            guard let data = data else {
                completion(.failure(NSError(domain: "", code: -1, userInfo: nil)))
                return
            }
            
            do {
                let authResponse = try JSONDecoder().decode(AuthResponse.self, from: data)
                completion(.success(authResponse))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }
    
    func createBooking(request: CreateBookingRequest, completion: @escaping (Result<BookingResponse, Error>) -> Void) {
        let url = URL(string: "\(baseURL)/api/patient/bookings")!
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        urlRequest.setValue("Bearer \(TokenManager.shared.accessToken ?? "")", forHTTPHeaderField: "Authorization")
        
        urlRequest.httpBody = try? JSONEncoder().encode(request)
        
        URLSession.shared.dataTask(with: urlRequest) { data, response, error in
            if let error = error {
                completion(.failure(error))
                return
            }
            
            guard let data = data else {
                completion(.failure(NSError(domain: "", code: -1, userInfo: nil)))
                return
            }
            
            do {
                let bookingResponse = try JSONDecoder().decode(BookingResponse.self, from: data)
                completion(.success(bookingResponse))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }
}
```

---

## Error Handling

### Standard Error Response

```json
{
  "success": false,
  "message": "Error description",
  "error": {
    "code": "ERROR_CODE",
    "details": "Detailed error information",
    "timestamp": "2026-06-15T10:30:00"
  }
}
```

### Common Error Codes

| Code | HTTP Status | Description | Action |
|------|-------------|-------------|--------|
| `UNAUTHORIZED` | 401 | Invalid or expired token | Refresh token or re-login |
| `FORBIDDEN` | 403 | Insufficient permissions | Check user role |
| `NOT_FOUND` | 404 | Resource not found | Verify resource ID |
| `VALIDATION_ERROR` | 400 | Invalid request data | Check request format |
| `BOOKING_NOT_FOUND` | 404 | Booking does not exist | Verify booking number |
| `NURSE_NOT_AVAILABLE` | 400 | No nurses available | Try different time/location |
| `PAYMENT_FAILED` | 400 | Payment processing failed | Retry payment |
| `INSUFFICIENT_BALANCE` | 400 | Wallet balance too low | Add funds |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests | Wait and retry |

---

## Testing Guide

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=BookingServiceTest

# Run with coverage
mvn clean test jacoco:report
```

### Test Structure

```
src/test/java/
├── com/pronurse/
│   ├── auth/
│   │   └── AuthServiceIntegrationTest.java
│   ├── booking/
│   │   └── BookingServiceTest.java
│   ├── wallet/
│   │   └── WalletServiceTest.java
│   └── config/
│       └── TestConfig.java
```

### Writing Tests

```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingServiceTest {
    
    @Autowired
    private BookingService bookingService;
    
    @Test
    void testCreateBooking() {
        // Arrange
        CreateBookingRequest request = new CreateBookingRequest();
        request.setSelectedServiceIds(List.of(1L));
        request.setBookingDate("2026-06-20");
        request.setBookingTime("10:00 AM");
        
        // Act
        String bookingNo = bookingService.createMultiItemBooking("9876543210", request);
        
        // Assert
        assertNotNull(bookingNo);
        assertTrue(bookingNo.startsWith("BOOK-"));
    }
}
```

---

## Deployment Guide

### Production Checklist

- [ ] Configure production database
- [ ] Set up environment variables
- [ ] Configure SMS gateway (Twilio/AWS SNS)
- [ ] Upload Firebase credentials
- [ ] Configure Razorpay production keys
- [ ] Set up SSL/TLS certificates
- [ ] Configure CORS for production domains
- [ ] Set up monitoring and logging
- [ ] Configure backup strategy
- [ ] Set up CI/CD pipeline

### Environment Variables

```bash
# Database
DATABASE_URL=jdbc:postgresql://prod-db:5432/pronurse_db
DATABASE_USERNAME=pronurse_user
DATABASE_PASSWORD=secure_password

# JWT
JWT_SECRET=your-production-secret-key-minimum-32-characters
JWT_EXPIRATION=86400000

# Razorpay
RAZORPAY_KEY_ID=rzp_live_your_key_id
RAZORPAY_KEY_SECRET=your_production_secret

# Firebase
FIREBASE_CREDENTIALS_PATH=/app/config/firebase-credentials.json

# SMS
SMS_PROVIDER=twilio
SMS_API_KEY=your_twilio_api_key
SMS_API_SECRET=your_twilio_api_secret

# File Storage
FILE_UPLOAD_DIR=/app/uploads
```

### Docker Deployment

```dockerfile
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/pro-nurse-backend-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

```bash
# Build Docker image
docker build -t pronurse-api:latest .

# Run container
docker run -d \
  -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://db:5432/pronurse_db \
  -e JWT_SECRET=your_secret \
  --name pronurse-api \
  pronurse-api:latest
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pronurse-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: pronurse-api
  template:
    metadata:
      labels:
        app: pronurse-api
    spec:
      containers:
      - name: pronurse-api
        image: pronurse-api:latest
        ports:
        - containerPort: 8080
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: pronurse-secrets
              key: database-url
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: pronurse-secrets
              key: jwt-secret
```

---

## Support & Resources

### Documentation
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API Documentation: `API_DOCUMENTATION.md`
- Workflow Documentation: `WORKFLOW_DOCUMENTATION.md`

### Contact
- Technical Support: support@pronurse.com
- Developer Portal: https://developers.pronurse.com

### Version History
- **v1.0.0** (June 2026): Initial release with all core features

---

**Made with ❤️ by the Pro Nurse Development Team**

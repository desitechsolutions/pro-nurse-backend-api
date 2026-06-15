# Pro Nurse Backend API - Complete API Documentation

## Base URL
- **Local Development**: `http://localhost:8080`
- **Production**: `https://your-production-domain.com`

## Authentication
All protected endpoints require a JWT token in the Authorization header:
```
Authorization: Bearer <access_token>
```

---

## 1. Authentication APIs

### 1.1 Send OTP
**Endpoint**: `POST /api/auth/otp/send`  
**Access**: Public  
**Description**: Initiates OTP workflow for user registration/login

**Request Body**:
```json
{
  "mobile": "9876543210"
}
```

**Response**:
```json
{
  "success": true,
  "message": "OTP sent successfully. For local testing use default OTP: 123456",
  "data": null
}
```

### 1.2 Verify OTP & Authenticate
**Endpoint**: `POST /api/auth/otp/verify`  
**Access**: Public  
**Description**: Verifies OTP and returns authentication tokens

**Request Body**:
```json
{
  "mobile": "9876543210",
  "otp": "123456",
  "name": "John Doe",
  "roleType": "PATIENT"
}
```

**Response**:
```json
{
  "success": true,
  "message": "Authentication successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "role": "PATIENT",
    "refreshToken": "refresh-token-uuid"
  }
}
```

### 1.3 Refresh Token
**Endpoint**: `POST /api/auth/refresh`  
**Access**: Public (requires refresh token cookie)  
**Description**: Rotates refresh token and generates new access token

**Response**:
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "new-access-token",
    "role": "PATIENT"
  }
}
```

### 1.4 Logout
**Endpoint**: `POST /api/auth/logout`  
**Access**: Public  
**Description**: Invalidates refresh token and clears session

**Response**:
```json
{
  "success": true,
  "message": "Logged out successfully",
  "data": null
}
```

---

## 2. Patient APIs

### 2.1 Get Patient Profile
**Endpoint**: `GET /api/patient/profile`  
**Access**: PATIENT role required  
**Description**: Retrieves patient profile details

**Response**:
```json
{
  "success": true,
  "message": "Profile fetched successfully",
  "data": {
    "id": 1,
    "patientId": "PAT-001",
    "name": "John Doe",
    "mobile": "9876543210",
    "email": "john@example.com",
    "gender": "Male",
    "dob": "1990-01-01",
    "bloodGroup": "O+",
    "address": "123 Main St, Delhi",
    "profileImage": "/uploads/profiles/patients/1.jpg",
    "latitude": 28.4595,
    "longitude": 77.0266
  }
}
```

### 2.2 Update Patient Profile
**Endpoint**: `PUT /api/patient/profile`  
**Access**: PATIENT role required  
**Content-Type**: `multipart/form-data`  
**Description**: Updates patient profile with optional image upload

**Request Parameters**:
- `data` (JSON): Profile update data
- `profileImage` (File, optional): Profile image file

**Request Data**:
```json
{
  "patientId": "PAT-001",
  "name": "John Doe",
  "email": "john@example.com",
  "gender": "Male",
  "dob": "1990-01-01",
  "bloodGroup": "O+",
  "address": "123 Main St, Delhi",
  "latitude": "28.4595",
  "longitude": "77.0266"
}
```

---

## 3. Nurse APIs

### 3.1 Get Nurse Profile
**Endpoint**: `GET /api/nurse/profile`  
**Access**: NURSE role required  
**Description**: Retrieves nurse profile details

**Response**:
```json
{
  "success": true,
  "message": "Profile fetched successfully",
  "data": {
    "id": 1,
    "nurseId": "NUR-001",
    "name": "Jane Smith",
    "mobile": "9876543210",
    "email": "jane@example.com",
    "gender": "Female",
    "dob": "1985-05-15",
    "qualification": "GNM",
    "experience": "5 Years",
    "specialization": "Post Surgery Care",
    "languages": "Hindi, English",
    "address": "456 Park Ave, Mumbai",
    "profileImage": "/uploads/profiles/nurses/1.jpg",
    "averageRating": 4.5,
    "isOnDuty": true,
    "verificationStatus": "Approved"
  }
}
```

### 3.2 Update Nurse Profile
**Endpoint**: `PUT /api/nurse/profile`  
**Access**: NURSE role required  
**Content-Type**: `multipart/form-data`

### 3.3 Update Location
**Endpoint**: `POST /api/nurse/location/update`  
**Access**: NURSE role required  
**Description**: Updates nurse's real-time GPS coordinates

**Request Body**:
```json
{
  "latitude": "28.4595",
  "longitude": "77.0266"
}
```

### 3.4 Toggle Duty Status
**Endpoint**: `POST /api/nurse/duty/toggle`  
**Access**: NURSE role required  
**Description**: Toggles nurse availability status

**Response**:
```json
{
  "success": true,
  "message": "Duty status updated successfully",
  "data": {
    "isOnDuty": true
  }
}
```

---

## 4. Booking APIs

### 4.1 Get Service Catalog
**Endpoint**: `GET /api/services`  
**Access**: Public  
**Description**: Retrieves available medical services

**Response**:
```json
{
  "success": true,
  "message": "Fetched catalog successfully",
  "data": [
    {
      "id": 1,
      "name": "Home Nursing",
      "description": "Professional nursing care at your doorstep",
      "basePrice": 0.00,
      "subServices": [
        {
          "id": 2,
          "name": "Injection Administration",
          "description": "IV/IM Injection service",
          "basePrice": 350.00
        }
      ]
    }
  ]
}
```

### 4.2 Create Booking
**Endpoint**: `POST /api/booking/create`  
**Access**: PATIENT role required  
**Description**: Creates a new booking request

**Request Body**:
```json
{
  "bookingDate": "2024-12-25",
  "bookingTime": "10:00 AM",
  "latitude": "28.4595",
  "longitude": "77.0266",
  "address": "123 Main St, Delhi, India",
  "remarks": "Patient needs urgent care",
  "selectedServiceIds": [2, 3]
}
```

**Response**:
```json
{
  "success": true,
  "message": "Booking registered",
  "data": {
    "bookingId": "BOOK-A1B2C3D4",
    "status": "PENDING"
  }
}
```

### 4.3 Create Booking with Prescription
**Endpoint**: `POST /api/booking/create-with-prescription`  
**Access**: PATIENT role required  
**Content-Type**: `multipart/form-data`

**Request Parameters**:
- `data` (JSON): Booking data
- `prescription` (File, optional): Prescription document

### 4.4 Get Patient Booking History
**Endpoint**: `GET /api/booking/patient/list`  
**Access**: PATIENT role required

### 4.5 Get Booking Details
**Endpoint**: `GET /api/booking/history/details/{bookingNo}`  
**Access**: Authenticated users

---

## 5. Payment APIs

### 5.1 Create Razorpay Order
**Endpoint**: `POST /api/payment/create-order`  
**Access**: PATIENT role required  
**Description**: Creates Razorpay order for payment

**Request Body**:
```json
{
  "bookingNo": "BOOK-A1B2C3D4"
}
```

**Response**:
```json
{
  "success": true,
  "message": "Payment order created",
  "data": {
    "orderId": "order_xyz123",
    "amount": 850.00,
    "key": "rzp_test_key"
  }
}
```

### 5.2 Verify Payment
**Endpoint**: `POST /api/payment/verify`  
**Access**: PATIENT role required  
**Description**: Verifies Razorpay payment signature

**Request Body**:
```json
{
  "bookingNo": "BOOK-A1B2C3D4",
  "paymentId": "pay_xyz123",
  "orderId": "order_xyz123",
  "signature": "signature_hash"
}
```

---

## 6. Nurse Action APIs

### 6.1 Get Nurse Dashboard
**Endpoint**: `GET /api/nurse/dashboard`  
**Access**: NURSE role required  
**Description**: Gets active booking requests for nurse

### 6.2 Accept Booking
**Endpoint**: `POST /api/nurse/booking/accept`  
**Access**: NURSE role required

**Request Body**:
```json
{
  "bookingId": "BOOK-A1B2C3D4"
}
```

### 6.3 Reject Booking
**Endpoint**: `POST /api/nurse/booking/reject`  
**Access**: NURSE role required

**Request Body**:
```json
{
  "bookingId": "BOOK-A1B2C3D4",
  "reason": "Not available at requested time"
}
```

### 6.4 Complete Booking
**Endpoint**: `POST /api/nurse/booking/complete`  
**Access**: NURSE role required

**Request Body**:
```json
{
  "bookingId": "BOOK-A1B2C3D4",
  "remarks": "Service completed successfully"
}
```

---

## 7. Review APIs

### 7.1 Submit Review
**Endpoint**: `POST /api/patient/review/submit`  
**Access**: PATIENT role required  
**Description**: Submits review for completed booking

**Request Body**:
```json
{
  "bookingId": "BOOK-A1B2C3D4",
  "rating": 5,
  "review": "Excellent service, very professional"
}
```

---

## 8. Wallet APIs

### 8.1 Get Wallet Summary
**Endpoint**: `GET /api/nurse/wallet/summary`  
**Access**: NURSE role required  
**Description**: Gets nurse wallet balance and transaction history

**Response**:
```json
{
  "success": true,
  "message": "Wallet summary fetched",
  "data": {
    "walletBalance": 5000.00,
    "lifetimeEarnings": 25000.00,
    "statements": [
      {
        "bookingNo": "BOOK-A1B2C3D4",
        "date": "2024-12-20T10:30:00",
        "type": "CREDIT",
        "grossAmount": 1000.00,
        "platformDeduction": 118.00,
        "netPayoutAmount": 882.00
      }
    ]
  }
}
```

### 8.2 Request Payout
**Endpoint**: `POST /api/nurse/wallet/payout/request`  
**Access**: NURSE role required

**Request Body**:
```json
{
  "amount": 1000.00
}
```

---

## 9. Admin APIs

### 9.1 Get Admin Profile
**Endpoint**: `GET /api/admin/me`  
**Access**: ADMIN role required

### 9.2 Get Dashboard Metrics
**Endpoint**: `GET /api/admin/analytics/metrics`  
**Access**: ADMIN role required

### 9.3 Verify Nurse
**Endpoint**: `POST /api/admin/nurse/verify`  
**Access**: ADMIN role required

**Request Body**:
```json
{
  "nurseId": 1,
  "action": "APPROVE",
  "rejectReason": null
}
```

### 9.4 Get All Nurses
**Endpoint**: `GET /api/admin/nurses?status=Pending&page=0&size=20`  
**Access**: ADMIN role required

### 9.5 Get All Bookings
**Endpoint**: `GET /api/admin/bookings?status=COMPLETED&page=0&size=20`  
**Access**: ADMIN role required

### 9.6 Manage Services
**Endpoint**: `POST /api/admin/services/save?serviceId=1`  
**Access**: ADMIN role required

### 9.7 Moderate Review
**Endpoint**: `POST /api/admin/reviews/moderate`  
**Access**: ADMIN role required

### 9.8 Toggle User Status
**Endpoint**: `POST /api/admin/users/status-toggle?userId=1&enableAccount=true`  
**Access**: ADMIN role required

---

## Error Responses

All error responses follow this format:
```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

### Common HTTP Status Codes
- `200 OK`: Successful request
- `400 Bad Request`: Invalid request data
- `401 Unauthorized`: Missing or invalid authentication
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: Server error

---

## WebSocket APIs

### Real-time Booking Alerts
**Endpoint**: `ws://localhost:8080/ws-alerts`  
**Protocol**: STOMP over WebSocket

**Subscribe to nurse alerts**:
```
SUBSCRIBE /queue/nurse/{mobile}
```

**Subscribe to patient alerts**:
```
SUBSCRIBE /queue/patient/{mobile}
```

---

## Rate Limiting
- OTP requests: 3 requests per 5 minutes per mobile number
- Other endpoints: No explicit rate limiting (implement as needed)

---

## Notes
1. All timestamps are in ISO 8601 format
2. Monetary values are in INR (Indian Rupees)
3. Platform fee: 10% + 18% GST on fee = 11.8% total deduction
4. Default OTP for testing: `123456`
5. Booking assignment timeout: 30 seconds
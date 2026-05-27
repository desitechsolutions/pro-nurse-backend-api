# Pro Nurse Backend API

Backend API service for the Pro Nurse platform developed using Spring Boot, Java 21, and MySQL.

The platform supports authentication, patient/provider management, verification workflows, role-based access control, and healthcare service management APIs.

---

## Tech Stack

- Java 21
- Spring Boot 4.0.6
- Spring Security
- JWT Authentication
- Spring Data JPA
- Hibernate
- MySQL
- Maven
- Flyway Migration
- Lombok
- MapStruct
- REST APIs

---

## Features

- User Registration & Login
- JWT-based Authentication
- Role-based Authorization
- Patient / Nurse / Admin Modules
- Provider Verification Workflow
- RESTful API Architecture
- Database Migration using Flyway
- Global Exception Handling
- Request Validation
- API Response Standardization

---

## Project Structure

```bash
src/
 ├── main/
 │   ├── java/
 │   │   └── com/pronurse/
 │   │       ├── config/
 │   │       ├── controller/
 │   │       ├── dto/
 │   │       ├── entity/
 │   │       ├── exception/
 │   │       ├── mapper/
 │   │       ├── repository/
 │   │       ├── security/
 │   │       ├── service/
 │   │       └── utils/
 │   └── resources/
 │       ├── db/migration/
 │       ├── application.yml
 │       └── application.properties

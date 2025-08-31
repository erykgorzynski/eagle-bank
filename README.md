# Eagle Bank API

A Spring Boot REST API for Eagle Bank that provides comprehensive banking functionality including user management, account operations, and transaction processing.

## Overview

Eagle Bank API is a modern banking application built with Spring Boot 3.1.5 that allows users to:
- Create and manage user accounts
- Create, update, and delete bank accounts
- Perform deposits and withdrawals
- View transaction history
- Secure authentication with JWT tokens

## Technology Stack

- **Java 17**
- **Spring Boot 3.1.5**
- **Spring Security** with JWT authentication
- **Spring Data JPA** with Hibernate
- **H2 Database** (in-memory for development and testing)
- **MapStruct** for object mapping
- **Lombok** for boilerplate code reduction
- **Maven** for dependency management
- **OpenAPI 3.1** with Swagger UI
- **JUnit 5** for testing

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/erykgorzynski/eagle-bank.git
   cd eagle-bank
   ```

2. **Build the project**
   ```bash
   ./mvnw clean compile
   ```

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

The application will start on `http://localhost:8080`

### Database Configuration

The application uses H2 as an in-memory database for development and testing.

**H2 Database:**
- No additional setup required
- Console available at: `http://localhost:8080/h2-console`
- Database is automatically created and populated on startup

## API Documentation

### Interactive Documentation
Visit `http://localhost:8080/swagger-ui.html` for interactive API documentation.

### API Endpoints

#### Authentication
- `POST /v1/auth/login` - Authenticate user and get JWT token

#### User Management
- `POST /v1/users` - Create a new user
- `GET /v1/users/{userId}` - Get user by ID
- `PATCH /v1/users/{userId}` - Update user
- `DELETE /v1/users/{userId}` - Delete user

#### Account Management
- `POST /v1/accounts` - Create a new bank account
- `GET /v1/accounts` - List user's accounts
- `GET /v1/accounts/{accountNumber}` - Get account by number
- `PATCH /v1/accounts/{accountNumber}` - Update account
- `DELETE /v1/accounts/{accountNumber}` - Delete account

#### Transaction Management
- `POST /v1/accounts/{accountNumber}/transactions` - Create transaction (deposit/withdrawal)
- `GET /v1/accounts/{accountNumber}/transactions` - List account transactions
- `GET /v1/accounts/{accountNumber}/transactions/{transactionId}` - Get specific transaction


## Project Structure

```
src/
├── main/java/org/example/
│   ├── EagleBankApplication.java       # Main application class
│   ├── config/                        # Configuration classes
│   ├── controller/                     # REST controllers
│   ├── entity/                         # JPA entities
│   ├── exception/                      # Custom exceptions
│   ├── mapper/                         # MapStruct mappers
│   ├── repository/                     # Data repositories
│   ├── security/                       # Security configurations
│   └── service/                        # Business logic services
├── main/resources/
│   └── application.properties          # Application configuration
└── test/                              # Test classes
```


## Error Handling

The API provides comprehensive error responses:

- `400 Bad Request` - Invalid input data or malformed requests
- `401 Unauthorized` - Missing or invalid authentication token
- `403 Forbidden` - Insufficient permissions to access resource
- `404 Not Found` - Resource not found (user, account, or transaction)
- `409 Conflict` - Resource conflict (e.g., user cannot be deleted when associated with bank accounts)
- `422 Unprocessable Entity` - Business rule violations (e.g., insufficient funds for withdrawal)
- `500 Internal Server Error` - Unexpected server errors


## License

This project is part of a technical assessment and is for demonstration purposes only.

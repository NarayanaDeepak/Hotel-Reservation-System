# Hotel Reservation System

## 1. Project Overview

The Hotel Reservation System is a Spring Boot REST API application that manages hotel customers, room types, amenities, rooms, reservations, and payments.

The application includes HTTP Basic authentication, role-based authorization with ownership checks, double-booking protection, automatic stay completion, caching, auditing, and demo data seeding.

It provides REST endpoints for managing hotel operations and supports API exploration through Swagger UI.

## 2. Technology Stack

- Java 17 (Maven compiler configuration)
- Spring Boot 3.3.4
- Spring Web MVC
- Spring Data JPA
- Hibernate ORM
- MySQL (application) and H2 (automated tests)
- HikariCP connection pool
- Spring Security (HTTP Basic, BCrypt)
- Bean Validation
- Caffeine Cache
- Spring Boot Actuator
- SpringDoc OpenAPI / Swagger UI
- Lombok
- JUnit 5, Mockito, MockMvc
- Maven
- Eclipse IDE / Spring Tool Suite (STS)
- Postman

## 3. Main Features

### 3.1 Guest Registration and Customer Management

- Register a guest account together with a customer profile
- Retrieve the logged-in customer's own profile
- Retrieve customer information by ID
- Update customer details
- Delete customer records (administrator only)
- List customers with pagination and sorting (administrator only)
- Retrieve a customer's reservation history
- Audit fields

### 3.2 Room Type, Amenity and Room Management

- Create, retrieve, update and delete room types
- Create, retrieve, update and delete amenities
- Assign amenities to room types (many-to-many)
- Create, retrieve, update and delete rooms
- Change a room's status (`AVAILABLE`, `MAINTENANCE`, `OUT_OF_SERVICE`)
- Filter rooms by room type, price range, capacity and status
- Pagination and sorting
- Caffeine caching of room types and amenities
- Administrator-only modifications

### 3.3 Authentication and Authorization

- HTTP Basic authentication
- Role-based authorization
- `GUEST` and `ADMIN` roles
- Password encryption using BCrypt
- Stateless authentication
- Object-level ownership checks, so a guest can only access their own reservations, payments and history

Public endpoints include registration, browsing room types, amenities and rooms, and searching availability. Administrator-only operations require an account with the `ADMIN` role.

### 3.4 Room Availability Search

- Search rooms that are free for a complete stay (check-in, check-out, number of guests, optional room type)
- Rooms in maintenance or out of service are excluded
- Overlap logic uses half-open intervals, so the check-out day is free for the next guest

### 3.5 Reservation Management

- Create reservations
- Retrieve reservations by ID
- List reservations, optionally by status (administrator only)
- Update reservation dates, room, guests and special requests
- Cancel reservations
- Calculate the total amount (nights multiplied by the room type's price per night)
- Reservation status report (administrator only)
- Track reservation status (`CONFIRMED`, `CANCELLED`, `COMPLETED`)

### 3.6 Payment Management

- Record payments against a reservation
- Retrieve payments for a reservation
- Reject payments that exceed the outstanding balance
- Track payment status (`UNPAID`, `PARTIALLY_PAID`, `PAID`, `REFUNDED`)
- Refund successful payments automatically when a reservation is cancelled

**Note:** Payment recording is a demonstration implementation. It does not integrate with a real payment gateway.

### 3.7 Automatic Stay Completion

The application includes scheduled stay completion processing.

The scheduler marks `CONFIRMED` reservations whose check-out day has passed as `COMPLETED`.

The schedule is configurable through application properties.

### 3.8 Concurrency Control and Auditing

- Pessimistic locking of the room row serialises concurrent bookings of the same room
- Optimistic locking (`@Version`) makes concurrent edits of any other record fail with `409 Conflict` instead of overwriting silently
- Audit fields (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`) on every entity

### 3.9 Caching

The application uses Caffeine as its in-memory caching provider.

Caching is configured for the `roomTypes` and `amenities` cache names. Cache expiration and maximum size are configurable through `application.properties`.

### 3.10 Monitoring

Spring Boot Actuator provides application monitoring endpoints.

The application exposes configured endpoints for:

- Health
- Application information
- Metrics
- Cache information
- Scheduled tasks
- Loggers

## 4. Project Structure

The main Java package is:

`com.example.hotelreservation`

The project uses a layered architecture.

```text
hotel-reservation-system/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/hotelreservation/
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       ├── entity/
│   │   │       ├── enums/
│   │   │       ├── exception/
│   │   │       ├── mapper/
│   │   │       ├── repository/
│   │   │       ├── scheduler/
│   │   │       ├── security/
│   │   │       ├── service/
│   │   │       │   └── impl/
│   │   │       └── specification/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-mysql.properties
│   └── test/
├── .gitignore
├── pom.xml
└── README.md
```

The structure above represents the main application layers. Additional classes and packages may exist in the project.

### Package Responsibilities

- **config:** Application configuration, including `DataInitializer`, cache, JPA auditing, scheduler and OpenAPI configuration.
- **controller:** REST API endpoints (Auth, Amenity, RoomType, Room, Customer, Reservation).
- **dto:** Request and response data structures, including `ApiErrorResponse` and `PageResponse`.
- **entity:** JPA entities mapped to database tables.
- **enums:** Enumeration types used by the application.
- **exception:** Custom exceptions and the global exception handler.
- **mapper:** Mapping between entities and DTOs.
- **repository:** Database access using Spring Data JPA, including custom JPQL and native queries.
- **scheduler:** Scheduled background processing.
- **security:** HTTP Basic security configuration, user details service, ownership checker and 401/403 handlers.
- **service:** Business service interfaces.
- **service.impl:** Business service implementations.
- **specification:** Dynamic room filtering.

## 5. Database Configuration

The application uses MySQL.

Database name:

`hotel_db`

The database connection is configured through the following properties:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/hotel_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=root
```

The example username and password are intended for local configuration only. Spring Boot lets an environment variable override any property, so you can supply your actual database credentials without editing the file:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Optionally, you can make the placeholders explicit in `application.properties`:

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/hotel_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:change-me}
```

The `createDatabaseIfNotExist=true` option creates the `hotel_db` database automatically. To create it manually instead:

```sql
CREATE DATABASE hotel_db;
```

Hibernate schema configuration:

```properties
spring.jpa.hibernate.ddl-auto=update
```

The application uses Spring Data JPA and Hibernate for database operations.

**Note:** The `update` setting is convenient for development. Production deployments should use a controlled database migration strategy such as Flyway or Liquibase.

## 6. Prerequisites

Before running the application, install or configure:

1. A compatible JDK.
2. MySQL Server.
3. Maven.
4. An IDE such as Eclipse or Spring Tool Suite, if preferred.
5. Postman for API testing.

The Maven compiler configuration specifies Java 17. Use a compatible JDK to build the application.

## 7. Running the Application

### Step 1: Start MySQL

Start MySQL. The `hotel_db` database is created automatically on first start. To create it manually, execute:

```sql
CREATE DATABASE hotel_db;
```

### Step 2: Configure the environment variables (optional)

Configure the following variables using your local terminal or IDE run configuration:

- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `HOTEL_SEED_ADMIN_PASSWORD`
- `HOTEL_SEED_GUEST_PASSWORD`

Example values for local development:

```text
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your-local-mysql-password
HOTEL_SEED_ADMIN_PASSWORD=your-strong-admin-password
HOTEL_SEED_GUEST_PASSWORD=your-strong-guest-password
```

Do not commit real passwords or other credentials to GitHub.

Windows (PowerShell):

```powershell
$env:HOTEL_SEED_ADMIN_PASSWORD="your-strong-admin-password"
```

Linux or macOS:

```bash
export HOTEL_SEED_ADMIN_PASSWORD='your-strong-admin-password'
```

In Eclipse or STS: **Run → Run Configurations →** select the application under **Spring Boot App → Environment** tab **→ Add**.

Keep the values stable while testing. Environment variables are read only at startup.

### Step 3: Import the project

In Eclipse or STS:

1. Select **File → Import**.
2. Choose **Existing Maven Projects**.
3. Select the project directory.
4. Finish the import.
5. Allow Maven dependencies to resolve.

### Step 4: Run the tests

From the project root, execute:

```bash
mvn test
```

The tests use an in-memory H2 database, so MySQL is not needed for them.

### Step 5: Start the application

Run the main application class:

`HotelReservationApplication`

Alternatively, execute:

```bash
mvn spring-boot:run
```

Confirm that the application starts successfully and connects to MySQL.

### Application URL

The default application port is `8080`.

Base URL:

http://localhost:8080

## 8. Swagger UI

The application uses SpringDoc OpenAPI to document its REST endpoints.

Open Swagger UI while the application is running:

http://localhost:8080/swagger-ui.html

The API specification is available at:

http://localhost:8080/v3/api-docs

Swagger UI can be used to explore the available endpoints and examine request and response formats.

Protected endpoints still require the appropriate authentication. Use the **Authorize** button and enter a username and password (HTTP Basic).

## 9. Actuator Health

The health endpoint is:

http://localhost:8080/actuator/health

This endpoint provides application health information and is public, as is `/actuator/info`.

Other configured Actuator endpoints (metrics, caches, scheduled tasks and loggers) require the `ADMIN` role:

- http://localhost:8080/actuator/metrics
- http://localhost:8080/actuator/caches
- http://localhost:8080/actuator/scheduledtasks

Actuator access should be appropriately restricted when deploying the application outside a local development environment.

## 10. Main API Endpoints

The following list summarizes the endpoints documented for the application. Consult the controllers and Swagger UI for the exact mappings, request fields, and response structures.

### 10.1 Authentication

Base path:

`/api/auth`

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Register a guest login and customer profile (public) |

Registered users authenticate with HTTP Basic on every protected request. There is no token to obtain.

### 10.2 Customers

Base path:

`/api/customers`

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/customers` | List customers with pagination and sorting (ADMIN) |
| GET | `/api/customers/me` | Retrieve the logged-in customer's profile |
| GET | `/api/customers/{id}` | Retrieve a customer (ADMIN or owner) |
| PUT | `/api/customers/{id}` | Update a customer (ADMIN or owner) |
| DELETE | `/api/customers/{id}` | Delete a customer (ADMIN) |
| GET | `/api/customers/{id}/reservations` | Retrieve a customer's reservation history (ADMIN or owner) |

### 10.3 Room Types

Base path:

`/api/room-types`

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/room-types` | Create a room type (ADMIN) |
| GET | `/api/room-types` | List room types (public, cached) |
| GET | `/api/room-types/{id}` | Retrieve a room type (public, cached) |
| PUT | `/api/room-types/{id}` | Update a room type (ADMIN) |
| DELETE | `/api/room-types/{id}` | Delete a room type (ADMIN) |

### 10.4 Amenities

Base path:

`/api/amenities`

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/amenities` | Create an amenity (ADMIN) |
| GET | `/api/amenities` | List amenities (public, cached) |
| GET | `/api/amenities/{id}` | Retrieve an amenity (public, cached) |
| PUT | `/api/amenities/{id}` | Update an amenity (ADMIN) |
| DELETE | `/api/amenities/{id}` | Delete an amenity (ADMIN) |

### 10.5 Rooms

Base path:

`/api/rooms`

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/rooms/availability?checkIn&checkOut&guests&roomTypeId` | Rooms free for the whole stay (public) |
| GET | `/api/rooms?roomTypeId&minPrice&maxPrice&minCapacity&status` | Filter rooms (public) |
| GET | `/api/rooms/{id}` | Retrieve a room (public) |
| POST | `/api/rooms` | Create a room (ADMIN) |
| PUT | `/api/rooms/{id}` | Update a room (ADMIN) |
| PATCH | `/api/rooms/{id}/status` | Change a room's status (ADMIN) |
| DELETE | `/api/rooms/{id}` | Delete a room (ADMIN) |

### 10.6 Reservations

Base path:

`/api/reservations`

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/reservations` | Create a reservation (ADMIN or owner of `customerId`) |
| GET | `/api/reservations?status=` | List reservations (ADMIN) |
| GET | `/api/reservations/report` | Count of reservations per status (ADMIN) |
| GET | `/api/reservations/{id}` | Retrieve a reservation (ADMIN or owner) |
| PUT | `/api/reservations/{id}` | Update a reservation (ADMIN or owner) |
| PATCH | `/api/reservations/{id}/cancel` | Cancel a reservation and refund payments (ADMIN or owner) |

### 10.7 Payments

Base path:

`/api/reservations/{id}/payments`

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/reservations/{id}/payments` | Record a payment (ADMIN or owner) |
| GET | `/api/reservations/{id}/payments` | Retrieve payments for a reservation (ADMIN or owner) |

Payment recording is a demonstration workflow and does not process transactions through a real payment gateway.

## 11. Authentication and API Testing

Postman can be used to test the REST APIs.

### Testing workflow

1. Start the application.
2. Register a guest, or use a seeded account.
3. Open Postman.
4. Select the required HTTP method and endpoint.
5. Open the Authorization tab.
6. Select **Basic Auth**.
7. Enter the username and password.
8. Add the request body (**Body → raw → JSON**) when required.
9. Send the request and inspect the response.

Use the ADMIN account for administrator-only operations.

A GUEST account should not be used to test operations restricted to administrators.

### HTTP status codes

Common HTTP status codes include:

- `200 OK`: Request completed successfully.
- `201 Created`: A resource was created.
- `204 No Content`: Request completed without a response body (for example a delete).
- `400 Bad Request`: Invalid request data. The response lists the failing fields in `details`.
- `401 Unauthorized`: Missing or invalid authentication.
- `403 Forbidden`: Authenticated user lacks the required permission.
- `404 Not Found`: Requested resource was not found.
- `405 Method Not Allowed`: The HTTP method is not supported for the endpoint.
- `409 Conflict`: Room already booked, duplicate value, or the record was modified by another request.
- `500 Internal Server Error`: An unexpected server-side error occurred.

The actual status code depends on the endpoint and its implementation.

Every error uses the same JSON body:

```json
{
  "timestamp": "2026-10-09T12:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Room 101 is already booked between 2026-12-22 and 2026-12-24",
  "path": "/api/reservations",
  "details": []
}
```

## 12. Reservation Rules

A stay covers `[checkIn, checkOut)`: the check-out day is free for the next guest.

- Check-in cannot be in the past, and check-out must be after check-in.
- A stay is at most 30 nights.
- The number of guests cannot exceed the room type's capacity.
- The room must be `AVAILABLE`.
- A new stay cannot overlap a `CONFIRMED` reservation for the same room.
- Only `CONFIRMED` reservations can be updated, paid or cancelled.
- A stay that has already started cannot be cancelled.
- The total amount is the number of nights multiplied by the room type's price per night.
- A payment cannot exceed the outstanding balance.
- Cancelling a reservation refunds every successful payment in the same transaction.

Example: a Standard room at 2500.00 per night from 20 to 23 December is 3 nights, so the total is 7500.00.

## 13. Automatic Stay Completion

The stay completion schedule is configured through:

```properties
hotel.scheduler.enabled=true
hotel.scheduler.complete-stays-rate-ms=3600000
```

The value `3600000` represents a run every hour. Use `60000` (one minute) while testing. Set `hotel.scheduler.enabled=false` to switch the scheduler off.

The scheduler updates `CONFIRMED` reservations whose check-out day has passed to the `COMPLETED` status with a single bulk update.

The exact processing behavior depends on the implementation in the scheduler and its associated repository query.

## 14. Demo Data and Seed Configuration

On startup, `DataInitializer` seeds demo data only when the `app_users` table is empty:

- One `ADMIN` user and one `GUEST` user with a matching customer profile
- Five amenities: WiFi, Air Conditioning, Mini Bar, Breakfast, Sea View
- Three room types: Standard (2500.00, capacity 2), Deluxe (4200.00, capacity 3), Suite (8000.00, capacity 4)
- Six rooms: 101, 102, 103 (Standard), 201, 202 (Deluxe), 301 (Suite)

Passwords are stored as BCrypt hashes. The seed settings are configured through:

```properties
hotel.seed.enabled=true
hotel.seed.admin-username=admin
hotel.seed.admin-password=admin123
hotel.seed.guest-username=guest
hotel.seed.guest-password=guest123
```

Spring Boot lets an environment variable override each property:

| Environment variable | Overrides |
|---|---|
| `HOTEL_SEED_ENABLED` | `hotel.seed.enabled` (set to `false` in production) |
| `HOTEL_SEED_ADMIN_USERNAME` | `hotel.seed.admin-username` |
| `HOTEL_SEED_ADMIN_PASSWORD` | `hotel.seed.admin-password` |
| `HOTEL_SEED_GUEST_USERNAME` | `hotel.seed.guest-username` |
| `HOTEL_SEED_GUEST_PASSWORD` | `hotel.seed.guest-password` |

Environment variables take priority over `application.properties`, so the file does not need to be edited.

Seeding is skipped once `app_users` has rows. Changing a seed password later does not update an existing user. In development, drop the database and restart to seed again:

```sql
DROP DATABASE hotel_db;
```

The default demo passwords are for local use only. Do not commit real passwords to GitHub.

## 15. Important Security Considerations

- Change the default seed passwords, or set `HOTEL_SEED_ENABLED=false` in production, so the known demo passwords cannot be used.
- Supply credentials through environment variables or a secure configuration mechanism.
- Do not commit passwords or secrets to GitHub.
- Use HTTPS when deploying, because HTTP Basic sends credentials with every request.
- Ensure administrator-only operations are protected by the security configuration.
- Keep the ownership checks that stop a guest from reading another guest's data.
- Do not trust client-supplied payment statuses in a production payment workflow.
- Integrate and validate a real payment gateway before handling actual payments.
- Restrict Swagger UI and Actuator endpoints appropriately in production.
- Use controlled database migrations for production deployments.
- Test authentication, authorization, and business rules with automated tests.

## 16. Testing

The project includes automated tests for service rules, repository queries and the HTTP layer.

Run the test suite using:

```bash
mvn test
```

The tests use in-memory H2 and fixed seed users.

| Test class | Covers |
|---|---|
| `HotelReservationApplicationTests` | Context loads and demo users are seeded |
| `ResponseEntityTest` | HTTP status and body of every controller, security matrix, full lifecycle |
| `ReservationServiceTest` | Pricing, overlap, date and capacity rules, cancel and refund (Mockito) |
| `PaymentServiceTest` | Partial and full payment, overpayment, invalid states (Mockito) |
| `AvailabilityRuleTest` | Overlap and availability queries, history, native report, Specification |
| `StayCompletionRuleTest` | Scheduler rule and the `@Version` optimistic lock |

Review the Maven output to confirm whether all tests pass.

The presence of a test class does not guarantee complete coverage of the application. Authentication, authorization, ownership, reservation workflows, and payment processing should be covered by appropriate tests.

## 17. Future Enhancements

Potential enhancements include:

- JWT authentication for mobile or single-page clients.
- Real payment gateway integration with idempotency keys.
- Production-ready database migrations (Flyway or Liquibase).
- A scheduler lock (for example ShedLock) when running several instances.
- A shared cache such as Redis when running several instances.
- Email or SMS booking confirmations.
- Additional automated integration and security tests.
- Centralized logging and monitoring.
- Deployment using a cloud or container platform.

## 18. Disclaimer

This project is a demonstration Hotel Reservation System built using Spring Boot and related technologies.

It demonstrates REST API development, database persistence, authentication, authorization, caching, scheduled processing, and concurrency control.

Additional security controls, payment gateway integration, production configuration, and comprehensive automated testing are required before using the application to manage real hotel bookings and payments.

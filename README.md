# Parking Reservation System

A small backend service for reserving parking spots. Users register with their vehicle
type (regular, electric vehicle owner, handicapped permit holder), and the API lets
them book a spot for a time window, cancel a booking, or let the system auto-assign
the best available spot for them.

Built with **Spring Boot 3 / Java 21**, backed by **PostgreSQL**, with **Flyway** for
database migrations.

## Features

- Register users and track their eligibility (EV owner / handicapped permit holder)
- List parking spots and see which ones are occupied right now
- Create a reservation for a specific spot and time window
- Auto-assign a reservation: the system picks a suitable free spot for you
  - Handicapped permit holders only get a `HANDICAPPED` spot
  - EV owners prefer an `EV` spot but fall back to `STANDARD`
  - Everyone else only gets a `STANDARD` spot
- Cancel a reservation (only allowed before it has started)
- List all reservations (active and cancelled) for a given spot
- A minimal built-in HTML test client
- Interactive API docs via Swagger UI

## Tech stack

| Layer          | Technology                              |
|----------------|------------------------------------------|
| Language        | Java 21                                  |
| Framework       | Spring Boot 3.3.5 (Web, Validation, JPA) |
| Database        | PostgreSQL                               |
| Migrations      | Flyway                                   |
| API docs        | springdoc-openapi (Swagger UI)           |
| Testing         | JUnit 5, Testcontainers                  |
| Build tool      | Maven (with the Maven Wrapper)           |
| Containers      | Docker / Docker Compose                  |

## Project structure

```
src/main/java/com/parkingreservation/
├── api/            REST controllers, request/response DTOs, error handling
├── application/    Use case / service layer (ReservationService, custom exceptions)
├── domain/         Core business rules: model classes and reservation policies
│                   (standard / EV / handicapped)
└── infrastructure/ JPA entities and Spring Data repositories

src/main/resources/
├── application.yml     Spring Boot configuration
├── db/migration/       Flyway SQL migration scripts
└── static/index.html   Built in browser test client
```

## Getting started

### Configure environment variables

Copy the example env file and adjust it if needed (the defaults work out of the box
for local development):

```bash
cp .env.example .env
```

### Run everything with Docker Compose

This starts a PostgreSQL database and the application, and applies the database
migrations automatically on startup:

```bash
docker compose up --build
```

The app will be available at `http://localhost:8080`

### Try it out

- Open `http://localhost:8080` in a browser for the built in test client 
- Open `http://localhost:8080/swagger-ui.html` for the interactive API docs
- Check `http://localhost:8080/actuator/health` to confirm the service is up


## Running the tests

```bash
./mvnw test
```

Integration tests use Testcontainers, so Docker must be running locally for the full
test suite to pass.

## API overview

All endpoints are prefixed with `/api`.

| Method | Path                                    | Description                                   |
|--------|------------------------------------------|-----------------------------------------------|
| GET    | `/users`                                 | List all registered users                     |
| POST   | `/users`                                 | Register a new user                           |
| GET    | `/parking-spots`                         | List all parking spots with current occupancy |
| POST   | `/reservations`                          | Create a reservation for a specific spot      |
| POST   | `/reservations/auto-assign`              | Auto-assign a free spot and create a reservation |
| POST   | `/reservations/{reservationId}/cancel`   | Cancel a reservation                          |
| GET    | `/parking-spots/{parkingSpotId}/reservations` | List reservations for a parking spot     |

See the Swagger UI (`/swagger-ui.html`) for full request/response schemas and status
codes.

## Database migrations

Migrations live in `src/main/resources/db/migration` and follow Flyway's naming
convention (`V1__description.sql`, `V2__description.sql`, ...). They run
automatically when the application starts.

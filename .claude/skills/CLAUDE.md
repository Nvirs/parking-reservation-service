# Development Guidelines

## Project Overview
A Java 21 & Spring Boot 3.16 REST API for managing parking spot reservations, following a pragmatic Domain-Driven / Policy-based architecture.

## Tech Stack
**Language:** Java 21 
* Framework: Spring Boot 3.16
* Database & Migration: PostgreSQL 16, Flyway
* Documentation: Swagger
* Testing: JUnit 5, Testcontainers


## Security Guidelines 
**Input Validation**: ALWAYS validate incoming Request DTOs using @Valid, @NotNull, @Future, etc. Never trust raw user input.
**SQL Injection Prevention:** Use Spring Data JPA methods or parameterized Query Methods. Never concatenate raw Strings into SQL queries.
**Data Exposure:** Never return Database Entities (@Entity) directly from Controllers. Always map them to DTOs to prevent sensitive domain data leakage.
**Secrets Management:** NEVER hardcode credentials, tokens, or DB passwords in application.yml or source code. Always use environment variables (${DB_PASSWORD}).
**Error Handling:** Ensure the Global Exception Handler (@ControllerAdvice) catches unhandled exceptions and returns generic error responses. Do NOT expose internal stack traces or database details to the client.

## Build & Test Commands
* Build project: ./mvnw clean package
* Run tests: ./mvnw test
* Start local environment: docker-compose up -d
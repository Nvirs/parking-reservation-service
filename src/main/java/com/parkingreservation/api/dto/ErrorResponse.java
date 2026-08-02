package com.parkingreservation.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Standard error payload returned for failed requests")
public record ErrorResponse(

        @Schema(description = "Time the error occurred")
        LocalDateTime timestamp,

        @Schema(description = "HTTP status code", example = "404")
        int status,

        @Schema(description = "HTTP status reason phrase", example = "Not Found")
        String error,

        @Schema(description = "Human-readable error message")
        String message,

        @Schema(description = "Request path that produced the error", example = "/api/reservations")
        String path,

        @Schema(description = "Field-level validation details, if any")
        List<String> details
) {

    public ErrorResponse(int status, String error, String message, String path) {
        this(status, error, message, path, List.of());
    }

    public ErrorResponse(int status, String error, String message, String path, List<String> details) {
        this(LocalDateTime.now(), status, error, message, path, details);
    }
}

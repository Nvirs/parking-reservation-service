package com.parkingreservation.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "Request payload to auto-assign a suitable parking spot to a user")
public record AutoAssignReservationRequest(

        @NotNull(message = "userId must not be null")
        @Schema(description = "Identifier of the user making the reservation", example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long userId,

        @NotNull(message = "startTime must not be null")
        @Future(message = "startTime must be in the future")
        @Schema(description = "Start of the reservation window", example = "2026-08-03T09:00:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime startTime,

        @NotNull(message = "endTime must not be null")
        @Future(message = "endTime must be in the future")
        @Schema(description = "End of the reservation window", example = "2026-08-03T11:00:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime endTime
) {
}

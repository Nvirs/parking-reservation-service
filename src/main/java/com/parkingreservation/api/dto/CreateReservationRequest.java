package com.parkingreservation.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "Request payload to create a new reservation for a parking spot")
public record CreateReservationRequest(

        @NotNull(message = "parkingSpotId must not be null")
        @Schema(description = "Identifier of the parking spot to reserve", example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long parkingSpotId,

        @NotBlank(message = "requester must not be blank")
        @Size(max = 100, message = "requester must be at most 100 characters")
        @Schema(description = "Name or identifier of the person making the reservation",
                example = "alice", requiredMode = Schema.RequiredMode.REQUIRED)
        String requester,

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

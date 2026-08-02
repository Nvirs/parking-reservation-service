package com.parkingreservation.api.dto;

import com.parkingreservation.domain.model.ParkingType;
import com.parkingreservation.domain.model.Reservation;
import com.parkingreservation.domain.model.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Reservation details returned by the API")
public record ReservationResponse(

        @Schema(description = "Reservation identifier")
        UUID id,

        @Schema(description = "Identifier of the reserved parking spot", example = "1")
        Long parkingSpotId,

        @Schema(description = "Human-readable code of the reserved parking spot", example = "A-1")
        String parkingSpotCode,

        @Schema(description = "Type of the reserved parking spot")
        ParkingType parkingSpotType,

        @Schema(description = "Identifier of the user who made the reservation", example = "1")
        Long userId,

        @Schema(description = "Name of the user who made the reservation", example = "alice")
        String userName,

        @Schema(description = "Start of the reservation window")
        LocalDateTime startTime,

        @Schema(description = "End of the reservation window")
        LocalDateTime endTime,

        @Schema(description = "Current status of the reservation")
        ReservationStatus status
) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.id(),
                reservation.parkingSpot().id(),
                reservation.parkingSpot().code(),
                reservation.parkingSpot().type(),
                reservation.requester().id(),
                reservation.requester().name(),
                reservation.startTime(),
                reservation.endTime(),
                reservation.status());
    }
}

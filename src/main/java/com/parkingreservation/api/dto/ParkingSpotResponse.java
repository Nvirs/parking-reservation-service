package com.parkingreservation.api.dto;

import com.parkingreservation.application.ParkingSpotAvailability;
import com.parkingreservation.domain.model.ParkingType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Parking spot details, including current occupancy")
public record ParkingSpotResponse(

        @Schema(description = "Parking spot identifier")
        Long id,

        @Schema(description = "Human-readable code of the parking spot", example = "A-1")
        String code,

        @Schema(description = "Type of the parking spot")
        ParkingType type,

        @Schema(description = "Whether the spot is occupied by an ACTIVE reservation right now; " +
                "informational only, it does not affect availability for a future window")
        boolean occupiedNow
) {

    public static ParkingSpotResponse from(ParkingSpotAvailability availability) {
        return new ParkingSpotResponse(
                availability.spot().id(),
                availability.spot().code(),
                availability.spot().type(),
                availability.occupiedNow());
    }
}

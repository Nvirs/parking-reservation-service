package com.parkingreservation.application;

import java.util.UUID;

public class ParkingSpotNotFoundException extends RuntimeException {

    public ParkingSpotNotFoundException(UUID parkingSpotId) {
        super("Parking spot %s not found".formatted(parkingSpotId));
    }
}

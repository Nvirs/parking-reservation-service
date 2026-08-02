package com.parkingreservation.application;

public class ParkingSpotNotFoundException extends RuntimeException {

    public ParkingSpotNotFoundException(Long parkingSpotId) {
        super("Parking spot %s not found".formatted(parkingSpotId));
    }
}

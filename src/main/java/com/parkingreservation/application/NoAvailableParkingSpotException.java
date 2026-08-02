package com.parkingreservation.application;

public class NoAvailableParkingSpotException extends RuntimeException {

    public NoAvailableParkingSpotException(Long userId) {
        super("No available parking spot found for user %s in the requested time window".formatted(userId));
    }
}

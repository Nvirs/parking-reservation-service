package com.parkingreservation.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ParkingSpot(UUID id, String code, ParkingType type) {

    public ParkingSpot {
        Objects.requireNonNull(type, "type must not be null");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
    }
}

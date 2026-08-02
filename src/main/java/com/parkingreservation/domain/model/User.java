package com.parkingreservation.domain.model;

public record User(Long id, String name, boolean electricVehicleOwner, boolean handicappedPermitHolder) {

    public User {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}

package com.parkingreservation.application;

import java.util.UUID;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(UUID reservationId) {
        super("Reservation %s not found".formatted(reservationId));
    }
}

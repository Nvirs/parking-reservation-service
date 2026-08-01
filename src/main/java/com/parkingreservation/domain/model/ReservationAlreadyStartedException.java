package com.parkingreservation.domain.model;

public class ReservationAlreadyStartedException extends RuntimeException {

    public ReservationAlreadyStartedException(String message) {
        super(message);
    }
}

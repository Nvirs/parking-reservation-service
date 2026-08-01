package com.parkingreservation.domain.policy;

public class ReservationPolicyViolationException extends RuntimeException {

    public ReservationPolicyViolationException(String message) {
        super(message);
    }
}

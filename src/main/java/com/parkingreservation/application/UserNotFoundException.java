package com.parkingreservation.application;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("User %s not found".formatted(userId));
    }
}

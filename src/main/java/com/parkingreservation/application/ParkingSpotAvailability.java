package com.parkingreservation.application;

import com.parkingreservation.domain.model.ParkingSpot;

public record ParkingSpotAvailability(ParkingSpot spot, boolean occupiedNow) {
}

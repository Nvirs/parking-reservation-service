package com.parkingreservation.domain.policy;

import com.parkingreservation.domain.model.ParkingType;
import com.parkingreservation.domain.model.Reservation;

import java.util.List;

public class StandardParkingPolicy implements ReservationPolicy {

    @Override
    public ParkingType supportedType() {
        return ParkingType.STANDARD;
    }

    @Override
    public void validate(Reservation request, List<Reservation> existingReservations) {
        requireMatchingParkingType(request);
        requireNoTimeConflict(request, existingReservations);
    }
}

package com.parkingreservation.domain.policy;

import com.parkingreservation.domain.model.ParkingType;
import com.parkingreservation.domain.model.Reservation;

import java.util.List;

public class EvParkingPolicy implements ReservationPolicy {

    @Override
    public ParkingType supportedType() {
        return ParkingType.EV;
    }

    @Override
    public void validate(Reservation request, List<Reservation> existingReservations) {
        requireMatchingParkingType(request);
        requireNoTimeConflict(request, existingReservations);
    }
}

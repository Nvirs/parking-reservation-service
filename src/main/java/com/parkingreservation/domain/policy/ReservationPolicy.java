package com.parkingreservation.domain.policy;

import com.parkingreservation.domain.model.ParkingType;
import com.parkingreservation.domain.model.Reservation;
import com.parkingreservation.domain.model.ReservationStatus;

import java.util.List;

public interface ReservationPolicy {

    ParkingType supportedType();

    /**
     * Validates the given reservation request, throwing a
     * {@link ReservationPolicyViolationException} if it is not acceptable.
     */
    void validate(Reservation request, List<Reservation> existingReservations);

    default void requireMatchingParkingType(Reservation request) {
        ParkingType actualType = request.parkingSpot().type();
        if (actualType != supportedType()) {
            throw new ReservationPolicyViolationException(
                    "Parking spot %s is of type %s and cannot be reserved under the %s policy"
                            .formatted(request.parkingSpot().code(), actualType, supportedType()));
        }
    }

    default void requireNoTimeConflict(Reservation request, List<Reservation> existingReservations) {
        for (Reservation existing : existingReservations) {
            if (existing.status() == ReservationStatus.CANCELLED) {
                continue;
            }
            if (existing.id() != null && existing.id().equals(request.id())) {
                continue;
            }
            if (!existing.parkingSpot().id().equals(request.parkingSpot().id())) {
                continue;
            }
            if (existing.overlaps(request)) {
                throw new ReservationPolicyViolationException(
                        "Requested time slot conflicts with existing reservation %s for spot %s"
                                .formatted(existing.id(), request.parkingSpot().code()));
            }
        }
    }

    default void requireElectricVehicleOwner(Reservation request) {
        if (!request.requester().electricVehicleOwner()) {
            throw new ReservationPolicyViolationException(
                    "EV parking spots require the requester to be registered as an electric vehicle owner");
        }
    }

    default void requireHandicappedPermitHolder(Reservation request) {
        if (!request.requester().handicappedPermitHolder()) {
            throw new ReservationPolicyViolationException(
                    "Handicapped parking spots require the requester to hold a handicapped permit");
        }
    }
}

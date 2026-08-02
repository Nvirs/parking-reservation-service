package com.parkingreservation.domain.policy;

import com.parkingreservation.domain.model.ParkingSpot;
import com.parkingreservation.domain.model.ParkingType;
import com.parkingreservation.domain.model.Reservation;
import com.parkingreservation.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class HandicappedParkingPolicyTest extends ReservationPolicyContractTest {

    @Override
    protected ReservationPolicy policy() {
        return new HandicappedParkingPolicy();
    }

    @Override
    protected ParkingType supportedType() {
        return ParkingType.HANDICAPPED;
    }

    @Override
    protected ParkingType unsupportedType() {
        return ParkingType.STANDARD;
    }

    @Test
    @DisplayName("rejects a request when the requester does not hold a handicapped permit")
    void rejectsWhenNotAHandicappedPermitHolder() {
        ParkingSpot spot = spotOf(ParkingType.HANDICAPPED);
        User nonPermitHolder = new User(1L, "requester", true, false);
        Reservation request = reservationOn(spot, BASE, BASE.plusHours(2), nonPermitHolder);

        assertThatExceptionOfType(ReservationPolicyViolationException.class)
                .isThrownBy(() -> policy().validate(request, List.of()));
    }
}

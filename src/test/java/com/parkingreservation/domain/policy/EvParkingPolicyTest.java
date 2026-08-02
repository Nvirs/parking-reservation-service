package com.parkingreservation.domain.policy;

import com.parkingreservation.domain.model.ParkingSpot;
import com.parkingreservation.domain.model.ParkingType;
import com.parkingreservation.domain.model.Reservation;
import com.parkingreservation.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class EvParkingPolicyTest extends ReservationPolicyContractTest {

    @Override
    protected ReservationPolicy policy() {
        return new EvParkingPolicy();
    }

    @Override
    protected ParkingType supportedType() {
        return ParkingType.EV;
    }

    @Override
    protected ParkingType unsupportedType() {
        return ParkingType.STANDARD;
    }

    @Test
    @DisplayName("rejects a request when the requester is not registered as an electric vehicle owner")
    void rejectsWhenNotAnElectricVehicleOwner() {
        ParkingSpot spot = spotOf(ParkingType.EV);
        User nonEvOwner = new User(1L, "requester", false, true);
        Reservation request = reservationOn(spot, BASE, BASE.plusHours(2), nonEvOwner);

        assertThatExceptionOfType(ReservationPolicyViolationException.class)
                .isThrownBy(() -> policy().validate(request, List.of()));
    }
}

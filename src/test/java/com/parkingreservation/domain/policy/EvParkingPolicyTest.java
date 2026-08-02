package com.parkingreservation.domain.policy;

import com.parkingreservation.domain.model.ParkingType;

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
}

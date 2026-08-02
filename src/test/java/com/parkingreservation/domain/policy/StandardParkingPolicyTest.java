package com.parkingreservation.domain.policy;

import com.parkingreservation.domain.model.ParkingType;

class StandardParkingPolicyTest extends ReservationPolicyContractTest {

    @Override
    protected ReservationPolicy policy() {
        return new StandardParkingPolicy();
    }

    @Override
    protected ParkingType supportedType() {
        return ParkingType.STANDARD;
    }

    @Override
    protected ParkingType unsupportedType() {
        return ParkingType.EV;
    }
}

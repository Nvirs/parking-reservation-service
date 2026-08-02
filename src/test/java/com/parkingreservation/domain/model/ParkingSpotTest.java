package com.parkingreservation.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ParkingSpotTest {

    @Test
    void rejectsBlankCode() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ParkingSpot(1L, " ", ParkingType.STANDARD));
    }

    @Test
    void rejectsNullCode() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ParkingSpot(1L, null, ParkingType.STANDARD));
    }

    @Test
    void rejectsNullType() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ParkingSpot(1L, "A-1", null));
    }
}

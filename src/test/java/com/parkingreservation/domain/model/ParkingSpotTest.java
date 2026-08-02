package com.parkingreservation.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ParkingSpotTest {

    @Test
    void rejectsBlankCode() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ParkingSpot(UUID.randomUUID(), " ", ParkingType.STANDARD));
    }

    @Test
    void rejectsNullCode() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ParkingSpot(UUID.randomUUID(), null, ParkingType.STANDARD));
    }

    @Test
    void rejectsNullType() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ParkingSpot(UUID.randomUUID(), "A-1", null));
    }
}

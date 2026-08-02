package com.parkingreservation.domain.policy;

import com.parkingreservation.domain.model.ParkingSpot;
import com.parkingreservation.domain.model.ParkingType;
import com.parkingreservation.domain.model.Reservation;
import com.parkingreservation.domain.model.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Shared contract exercised by every {@link ReservationPolicy} implementation.
 */
abstract class ReservationPolicyContractTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 8, 3, 9, 0);
    private static final AtomicLong PARKING_SPOT_ID_SEQUENCE = new AtomicLong(1);

    protected abstract ReservationPolicy policy();

    protected abstract ParkingType supportedType();

    protected abstract ParkingType unsupportedType();

    private ParkingSpot spotOf(ParkingType type) {
        long id = PARKING_SPOT_ID_SEQUENCE.getAndIncrement();
        return new ParkingSpot(id, "SPOT-" + type + "-" + id, type);
    }

    private Reservation reservationOn(ParkingSpot spot, LocalDateTime start, LocalDateTime end) {
        return new Reservation(UUID.randomUUID(), spot, "requester", start, end);
    }

    private Reservation reservationOn(ParkingSpot spot, LocalDateTime start, LocalDateTime end, ReservationStatus status) {
        return new Reservation(UUID.randomUUID(), spot, "requester", start, end, status);
    }

    @Test
    @DisplayName("accepts a request for the supported type with no conflicting reservations")
    void acceptsNonConflictingRequestForSupportedType() {
        ParkingSpot spot = spotOf(supportedType());
        Reservation request = reservationOn(spot, BASE, BASE.plusHours(2));

        assertThatCode(() -> policy().validate(request, List.of()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects a request whose parking spot type does not match the policy's supported type")
    void rejectsMismatchedParkingType() {
        ParkingSpot spot = spotOf(unsupportedType());
        Reservation request = reservationOn(spot, BASE, BASE.plusHours(2));

        assertThatExceptionOfType(ReservationPolicyViolationException.class)
                .isThrownBy(() -> policy().validate(request, List.of()))
                .withMessageContaining(unsupportedType().toString());
    }

    @Test
    @DisplayName("rejects a request that overlaps an existing active reservation for the same spot")
    void rejectsOverlappingActiveReservationForSameSpot() {
        ParkingSpot spot = spotOf(supportedType());
        Reservation existing = reservationOn(spot, BASE, BASE.plusHours(2));
        Reservation request = reservationOn(spot, BASE.plusMinutes(30), BASE.plusHours(3));

        assertThatExceptionOfType(ReservationPolicyViolationException.class)
                .isThrownBy(() -> policy().validate(request, List.of(existing)));
    }

    @Test
    @DisplayName("accepts back-to-back reservations that only touch at the boundary")
    void acceptsBackToBackReservations() {
        ParkingSpot spot = spotOf(supportedType());
        Reservation existing = reservationOn(spot, BASE, BASE.plusHours(2));
        Reservation request = reservationOn(spot, BASE.plusHours(2), BASE.plusHours(4));

        assertThatCode(() -> policy().validate(request, List.of(existing)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ignores a cancelled reservation that would otherwise conflict")
    void ignoresCancelledConflictingReservation() {
        ParkingSpot spot = spotOf(supportedType());
        Reservation cancelled = reservationOn(spot, BASE, BASE.plusHours(2), ReservationStatus.CANCELLED);
        Reservation request = reservationOn(spot, BASE.plusMinutes(30), BASE.plusHours(1));

        assertThatCode(() -> policy().validate(request, List.of(cancelled)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ignores an overlapping reservation that belongs to a different parking spot")
    void ignoresConflictOnDifferentSpot() {
        ParkingSpot spotA = spotOf(supportedType());
        ParkingSpot spotB = spotOf(supportedType());
        Reservation existingOnOtherSpot = reservationOn(spotB, BASE, BASE.plusHours(2));
        Reservation request = reservationOn(spotA, BASE, BASE.plusHours(2));

        assertThatCode(() -> policy().validate(request, List.of(existingOnOtherSpot)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("reports the policy's supported type")
    void exposesSupportedType() {
        assertThat(policy().supportedType()).isEqualTo(supportedType());
    }
}

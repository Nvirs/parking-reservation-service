package com.parkingreservation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ReservationTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 3, 9, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 8, 3, 11, 0);

    private ParkingSpot spot() {
        return new ParkingSpot(1L, "A-1", ParkingType.STANDARD);
    }

    private User user() {
        return new User(1L, "alice", false, false);
    }

    @Test
    @DisplayName("new reservations default to ACTIVE status")
    void newReservationDefaultsToActive() {
        Reservation reservation = new Reservation(UUID.randomUUID(), spot(), user(), START, END);

        assertThat(reservation.status()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(reservation.isActive()).isTrue();
    }

    @Test
    @DisplayName("rejects a start time that is not before the end time")
    void rejectsStartTimeNotBeforeEndTime() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Reservation(UUID.randomUUID(), spot(), user(), END, START));
    }

    @Test
    @DisplayName("rejects an equal start and end time")
    void rejectsEqualStartAndEndTime() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Reservation(UUID.randomUUID(), spot(), user(), START, START));
    }

    @Test
    @DisplayName("rejects a duration shorter than 15 minutes")
    void rejectsDurationShorterThanMinimum() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Reservation(UUID.randomUUID(), spot(), user(), START, START.plusMinutes(14)));
    }

    @Test
    @DisplayName("accepts a duration of exactly 15 minutes")
    void acceptsMinimumDuration() {
        assertThatCode(() -> new Reservation(UUID.randomUUID(), spot(), user(), START, START.plusMinutes(15)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("accepts a duration of exactly 24 hours")
    void acceptsMaximumDuration() {
        assertThatCode(() -> new Reservation(UUID.randomUUID(), spot(), user(), START, START.plusHours(24)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects a duration longer than 24 hours")
    void rejectsDurationLongerThanMaximum() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Reservation(UUID.randomUUID(), spot(), user(), START,
                        START.plusHours(24).plusMinutes(1)));
    }

    @Test
    @DisplayName("rejects a null requester")
    void rejectsNullRequester() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Reservation(UUID.randomUUID(), spot(), null, START, END));
    }

    @Test
    @DisplayName("rejects a null parking spot")
    void rejectsNullParkingSpot() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Reservation(UUID.randomUUID(), null, user(), START, END));
    }

    @Test
    @DisplayName("cancelling before the start time transitions status to CANCELLED")
    void cancelBeforeStartTransitionsToCancelled() {
        Reservation reservation = new Reservation(UUID.randomUUID(), spot(), user(), START, END);

        reservation.cancel(START.minusMinutes(1));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.isActive()).isFalse();
    }

    @Test
    @DisplayName("cancelling at or after the start time throws ReservationAlreadyStartedException")
    void cancelAtOrAfterStartThrows() {
        Reservation reservation = new Reservation(UUID.randomUUID(), spot(), user(), START, END);

        assertThatExceptionOfType(ReservationAlreadyStartedException.class)
                .isThrownBy(() -> reservation.cancel(START));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("overlaps() detects intersecting time windows and ignores merely adjacent ones")
    void overlapsDetectsIntersectingWindows() {
        ParkingSpot spot = spot();
        Reservation reservation = new Reservation(UUID.randomUUID(), spot, user(), START, END);
        Reservation overlapping = new Reservation(UUID.randomUUID(), spot, new User(2L, "bob", false, false),
                START.plusMinutes(30), END.plusMinutes(30));
        Reservation adjacent = new Reservation(UUID.randomUUID(), spot, new User(3L, "carol", false, false),
                END, END.plusHours(1));

        assertThat(reservation.overlaps(overlapping)).isTrue();
        assertThat(reservation.overlaps(adjacent)).isFalse();
    }

    @Test
    @DisplayName("equality and hashCode are based on id only")
    void equalityIsBasedOnId() {
        UUID id = UUID.randomUUID();
        ParkingSpot spot = spot();
        Reservation first = new Reservation(id, spot, user(), START, END);
        Reservation second = new Reservation(id, spot, new User(2L, "someone-else", false, false),
                START.plusHours(5), END.plusHours(5));

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }

    @Test
    @DisplayName("constructing with an explicit status does not throw")
    void constructingWithExplicitStatusSucceeds() {
        assertThatCode(() ->
                new Reservation(UUID.randomUUID(), spot(), user(), START, END, ReservationStatus.CANCELLED))
                .doesNotThrowAnyException();
    }
}

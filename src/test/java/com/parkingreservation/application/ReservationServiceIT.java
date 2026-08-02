package com.parkingreservation.application;

import com.parkingreservation.domain.model.ParkingType;
import com.parkingreservation.domain.model.Reservation;
import com.parkingreservation.domain.model.ReservationAlreadyStartedException;
import com.parkingreservation.domain.model.ReservationStatus;
import com.parkingreservation.domain.policy.ReservationPolicyViolationException;
import com.parkingreservation.infrastructure.persistence.ParkingSpotEntity;
import com.parkingreservation.infrastructure.persistence.ParkingSpotRepository;
import com.parkingreservation.infrastructure.persistence.ReservationEntity;
import com.parkingreservation.infrastructure.persistence.ReservationRepository;
import com.parkingreservation.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * End-to-end tests for the application layer reservation 
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class ReservationServiceIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private static LocalDateTime tomorrowAt(int hour) {
        return LocalDateTime.now().plusDays(1).withHour(hour).withMinute(0).withSecond(0).withNano(0);
    }

    private UUID givenParkingSpot(String code, ParkingType type) {
        return parkingSpotRepository.saveAndFlush(new ParkingSpotEntity(code, type)).getId();
    }

    @Test
    @DisplayName("reserving a standard spot with no conflicts persists an ACTIVE reservation")
    void reservesAStandardParkingSpot() {
        UUID spotId = givenParkingSpot("A-1", ParkingType.STANDARD);

        Reservation reservation = reservationService.reserve(spotId, "alice", tomorrowAt(9), tomorrowAt(11));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(reservationRepository.findByParkingSpot_Id(spotId)).hasSize(1);
    }

    @Test
    @DisplayName("reserving an EV spot is validated by the EV policy, not the standard one")
    void reservesAnEvParkingSpot() {
        UUID spotId = givenParkingSpot("EV-1", ParkingType.EV);

        Reservation reservation = reservationService.reserve(spotId, "bob", tomorrowAt(9), tomorrowAt(11));

        assertThat(reservation.parkingSpot().type()).isEqualTo(ParkingType.EV);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("rejects a reservation that overlaps an existing active one for the same spot, and persists nothing new")
    void rejectsOverlappingReservationForSameSpot() {
        UUID spotId = givenParkingSpot("A-2", ParkingType.STANDARD);
        reservationService.reserve(spotId, "alice", tomorrowAt(9), tomorrowAt(11));

        assertThatExceptionOfType(ReservationPolicyViolationException.class).isThrownBy(() ->
                reservationService.reserve(spotId, "bob", tomorrowAt(10), tomorrowAt(12)));

        assertThat(reservationRepository.findByParkingSpot_Id(spotId)).hasSize(1);
    }

    @Test
    @DisplayName("accepts a reservation for a different spot even if the time window overlaps")
    void acceptsOverlappingReservationForDifferentSpot() {
        UUID spotA = givenParkingSpot("A-3", ParkingType.STANDARD);
        UUID spotB = givenParkingSpot("A-4", ParkingType.STANDARD);
        reservationService.reserve(spotA, "alice", tomorrowAt(9), tomorrowAt(11));

        Reservation reservation = reservationService.reserve(spotB, "bob", tomorrowAt(9), tomorrowAt(11));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("reserving an unknown parking spot throws ParkingSpotNotFoundException")
    void reservingUnknownParkingSpotThrows() {
        assertThatExceptionOfType(ParkingSpotNotFoundException.class).isThrownBy(() ->
                reservationService.reserve(UUID.randomUUID(), "alice", tomorrowAt(9), tomorrowAt(11)));
    }

    @Test
    @DisplayName("finds only the reservations that belong to the requested parking spot")
    void findsReservationsForASpecificSpot() {
        UUID spotA = givenParkingSpot("B-1", ParkingType.STANDARD);
        UUID spotB = givenParkingSpot("B-2", ParkingType.STANDARD);
        reservationService.reserve(spotA, "alice", tomorrowAt(9), tomorrowAt(11));
        reservationService.reserve(spotB, "bob", tomorrowAt(9), tomorrowAt(11));

        List<Reservation> forSpotA = reservationService.findReservationsForSpot(spotA);

        assertThat(forSpotA).hasSize(1);
        assertThat(forSpotA.get(0).requester()).isEqualTo("alice");
    }

    @Test
    @DisplayName("querying reservations for an unknown parking spot throws ParkingSpotNotFoundException")
    void findingReservationsForUnknownSpotThrows() {
        assertThatExceptionOfType(ParkingSpotNotFoundException.class).isThrownBy(() ->
                reservationService.findReservationsForSpot(UUID.randomUUID()));
    }

    @Test
    @DisplayName("cancelling before the start time marks the reservation CANCELLED")
    void cancelsAReservationBeforeItStarts() {
        UUID spotId = givenParkingSpot("C-1", ParkingType.STANDARD);
        Reservation reservation = reservationService.reserve(spotId, "alice", tomorrowAt(9), tomorrowAt(11));

        reservationService.cancel(reservation.id(), tomorrowAt(9).minusHours(1));

        ReservationEntity persisted = reservationRepository.findById(reservation.id()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelling at or after the start time throws and leaves the reservation ACTIVE")
    void cancellingAfterStartThrowsAndLeavesReservationActive() {
        // c2 is a standard spot so the standard policy applies
        UUID spotId = givenParkingSpot("C-2", ParkingType.STANDARD);
        Reservation reservation = reservationService.reserve(spotId, "alice", tomorrowAt(9), tomorrowAt(11));

        assertThatExceptionOfType(ReservationAlreadyStartedException.class).isThrownBy(() ->
                reservationService.cancel(reservation.id(), tomorrowAt(9)));

        ReservationEntity persisted = reservationRepository.findById(reservation.id()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("cancelling an unknown reservation throws ReservationNotFoundException")
    void cancellingUnknownReservationThrows() {
        assertThatExceptionOfType(ReservationNotFoundException.class).isThrownBy(() ->
                reservationService.cancel(UUID.randomUUID(), LocalDateTime.now()));
    }

    @Test
    @DisplayName("a cancelled reservation no longer blocks a new request for the same slot")
    void cancelledReservationFreesUpTheSlot() {
        UUID spotId = givenParkingSpot("C-3", ParkingType.STANDARD);
        Reservation reservation = reservationService.reserve(spotId, "alice", tomorrowAt(9), tomorrowAt(11));
        reservationService.cancel(reservation.id(), tomorrowAt(9).minusHours(1));

        Reservation replacement = reservationService.reserve(spotId, "bob", tomorrowAt(9), tomorrowAt(11));

        assertThat(replacement.status()).isEqualTo(ReservationStatus.ACTIVE);
    }
}

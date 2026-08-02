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
import com.parkingreservation.infrastructure.persistence.UserEntity;
import com.parkingreservation.infrastructure.persistence.UserRepository;
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

    private static final Long UNKNOWN_PARKING_SPOT_ID = -1L;
    private static final Long UNKNOWN_USER_ID = -1L;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    private static LocalDateTime tomorrowAt(int hour) {
        return LocalDateTime.now().plusDays(1).withHour(hour).withMinute(0).withSecond(0).withNano(0);
    }

    private Long givenParkingSpot(String code, ParkingType type) {
        return parkingSpotRepository.saveAndFlush(new ParkingSpotEntity(code, type)).getId();
    }

    private Long givenUser(String name, boolean electricVehicleOwner, boolean handicappedPermitHolder) {
        return userRepository.saveAndFlush(new UserEntity(name, electricVehicleOwner, handicappedPermitHolder)).getId();
    }

    @Test
    @DisplayName("reserving a standard spot with no conflicts persists an ACTIVE reservation")
    void reservesAStandardParkingSpot() {
        Long spotId = givenParkingSpot("B-1", ParkingType.STANDARD);
        Long userId = givenUser("alice", false, false);

        Reservation reservation = reservationService.reserve(spotId, userId, tomorrowAt(9), tomorrowAt(11));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(reservationRepository.findByParkingSpot_Id(spotId)).hasSize(1);
    }

    @Test
    @DisplayName("reserving an EV spot is validated by the EV policy, not the standard one")
    void reservesAnEvParkingSpot() {
        Long spotId = givenParkingSpot("B-EV-1", ParkingType.EV);
        Long userId = givenUser("bob", true, false);

        Reservation reservation = reservationService.reserve(spotId, userId, tomorrowAt(9), tomorrowAt(11));

        assertThat(reservation.parkingSpot().type()).isEqualTo(ParkingType.EV);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("rejects an EV reservation when the requester is not registered as an electric vehicle owner")
    void rejectsEvReservationForNonElectricVehicleOwner() {
        Long spotId = givenParkingSpot("B-EV-2", ParkingType.EV);
        Long userId = givenUser("bob", false, false);

        assertThatExceptionOfType(ReservationPolicyViolationException.class).isThrownBy(() ->
                reservationService.reserve(spotId, userId, tomorrowAt(9), tomorrowAt(11)));

        assertThat(reservationRepository.findByParkingSpot_Id(spotId)).isEmpty();
    }

    @Test
    @DisplayName("reserving a handicapped spot is validated by the handicapped policy")
    void reservesAHandicappedParkingSpot() {
        Long spotId = givenParkingSpot("B-HANDI-1", ParkingType.HANDICAPPED);
        Long userId = givenUser("carol", false, true);

        Reservation reservation = reservationService.reserve(spotId, userId, tomorrowAt(9), tomorrowAt(11));

        assertThat(reservation.parkingSpot().type()).isEqualTo(ParkingType.HANDICAPPED);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("rejects a handicapped reservation when the requester does not hold a handicapped permit")
    void rejectsHandicappedReservationForNonPermitHolder() {
        Long spotId = givenParkingSpot("B-HANDI-2", ParkingType.HANDICAPPED);
        Long userId = givenUser("carol", false, false);

        assertThatExceptionOfType(ReservationPolicyViolationException.class).isThrownBy(() ->
                reservationService.reserve(spotId, userId, tomorrowAt(9), tomorrowAt(11)));

        assertThat(reservationRepository.findByParkingSpot_Id(spotId)).isEmpty();
    }

    @Test
    @DisplayName("rejects a reservation that overlaps an existing active one for the same spot, and persists nothing new")
    void rejectsOverlappingReservationForSameSpot() {
        Long spotId = givenParkingSpot("B-2", ParkingType.STANDARD);
        Long alice = givenUser("alice", false, false);
        Long bob = givenUser("bob", false, false);
        reservationService.reserve(spotId, alice, tomorrowAt(9), tomorrowAt(11));

        assertThatExceptionOfType(ReservationPolicyViolationException.class).isThrownBy(() ->
                reservationService.reserve(spotId, bob, tomorrowAt(10), tomorrowAt(12)));

        assertThat(reservationRepository.findByParkingSpot_Id(spotId)).hasSize(1);
    }

    @Test
    @DisplayName("accepts a reservation for a different spot even if the time window overlaps")
    void acceptsOverlappingReservationForDifferentSpot() {
        Long spotA = givenParkingSpot("A-3", ParkingType.STANDARD);
        Long spotB = givenParkingSpot("A-4", ParkingType.STANDARD);
        Long alice = givenUser("alice", false, false);
        Long bob = givenUser("bob", false, false);
        reservationService.reserve(spotA, alice, tomorrowAt(9), tomorrowAt(11));

        Reservation reservation = reservationService.reserve(spotB, bob, tomorrowAt(9), tomorrowAt(11));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("reserving an unknown parking spot throws ParkingSpotNotFoundException")
    void reservingUnknownParkingSpotThrows() {
        Long userId = givenUser("alice", false, false);

        assertThatExceptionOfType(ParkingSpotNotFoundException.class).isThrownBy(() ->
                reservationService.reserve(UNKNOWN_PARKING_SPOT_ID, userId, tomorrowAt(9), tomorrowAt(11)));
    }

    @Test
    @DisplayName("reserving with an unknown user throws UserNotFoundException")
    void reservingWithUnknownUserThrows() {
        Long spotId = givenParkingSpot("B-3", ParkingType.STANDARD);

        assertThatExceptionOfType(UserNotFoundException.class).isThrownBy(() ->
                reservationService.reserve(spotId, UNKNOWN_USER_ID, tomorrowAt(9), tomorrowAt(11)));
    }

    @Test
    @DisplayName("auto-assign gives an electric vehicle owner an EV spot when one is free")
    void autoAssignGivesElectricVehicleOwnerAnEvSpotWhenAvailable() {
        Long userId = givenUser("erin", true, false);

        Reservation reservation = reservationService.reserveAutoAssign(userId, tomorrowAt(9), tomorrowAt(11));

        assertThat(reservation.parkingSpot().type()).isEqualTo(ParkingType.EV);
    }

    @Test
    @DisplayName("auto-assign falls back to a standard spot for an electric vehicle owner when no EV spot is free")
    void autoAssignFallsBackToStandardForElectricVehicleOwnerWhenNoEvSpotFree() {
        LocalDateTime start = tomorrowAt(9);
        LocalDateTime end = tomorrowAt(11);
        for (ParkingSpotEntity evSpot : parkingSpotRepository.findByTypeOrderByCode(ParkingType.EV)) {
            reservationService.reserve(evSpot.getId(), givenUser("filler-" + evSpot.getId(), true, false), start, end);
        }
        Long userId = givenUser("erin", true, false);

        Reservation reservation = reservationService.reserveAutoAssign(userId, start, end);

        assertThat(reservation.parkingSpot().type()).isEqualTo(ParkingType.STANDARD);
    }

    @Test
    @DisplayName("auto-assign gives a handicapped-permit holder a HANDICAPPED spot")
    void autoAssignGivesHandicappedPermitHolderAHandicappedSpot() {
        Long userId = givenUser("erin", false, true);

        Reservation reservation = reservationService.reserveAutoAssign(userId, tomorrowAt(9), tomorrowAt(11));

        assertThat(reservation.parkingSpot().type()).isEqualTo(ParkingType.HANDICAPPED);
    }

    @Test
    @DisplayName("auto-assign never falls back to standard for a handicapped-permit holder, even if standard is free")
    void autoAssignDoesNotFallBackForHandicappedPermitHolderWhenNoHandicappedSpotFree() {
        LocalDateTime start = tomorrowAt(9);
        LocalDateTime end = tomorrowAt(11);
        for (ParkingSpotEntity handicappedSpot : parkingSpotRepository.findByTypeOrderByCode(ParkingType.HANDICAPPED)) {
            reservationService.reserve(handicappedSpot.getId(),
                    givenUser("filler-" + handicappedSpot.getId(), false, true), start, end);
        }
        Long userId = givenUser("erin", false, true);

        assertThatExceptionOfType(NoAvailableParkingSpotException.class).isThrownBy(() ->
                reservationService.reserveAutoAssign(userId, start, end));
    }

    @Test
    @DisplayName("auto-assign treats a user with both attributes as handicapped-first")
    void autoAssignPrioritizesHandicappedOverElectricVehicle() {
        Long userId = givenUser("dave-like", true, true);

        Reservation reservation = reservationService.reserveAutoAssign(userId, tomorrowAt(9), tomorrowAt(11));

        assertThat(reservation.parkingSpot().type()).isEqualTo(ParkingType.HANDICAPPED);
    }

    @Test
    @DisplayName("auto-assign gives a user with neither attribute a standard spot")
    void autoAssignGivesPlainUserAStandardSpot() {
        Long userId = givenUser("erin", false, false);

        Reservation reservation = reservationService.reserveAutoAssign(userId, tomorrowAt(9), tomorrowAt(11));

        assertThat(reservation.parkingSpot().type()).isEqualTo(ParkingType.STANDARD);
    }

    @Test
    @DisplayName("auto-assign with an unknown user throws UserNotFoundException")
    void autoAssignWithUnknownUserThrows() {
        assertThatExceptionOfType(UserNotFoundException.class).isThrownBy(() ->
                reservationService.reserveAutoAssign(UNKNOWN_USER_ID, tomorrowAt(9), tomorrowAt(11)));
    }

    @Test
    @DisplayName("finds only the reservations that belong to the requested parking spot")
    void findsReservationsForASpecificSpot() {
        Long spotA = givenParkingSpot("B-1", ParkingType.STANDARD);
        Long spotB = givenParkingSpot("B-2", ParkingType.STANDARD);
        Long alice = givenUser("alice", false, false);
        Long bob = givenUser("bob", false, false);
        reservationService.reserve(spotA, alice, tomorrowAt(9), tomorrowAt(11));
        reservationService.reserve(spotB, bob, tomorrowAt(9), tomorrowAt(11));

        List<Reservation> forSpotA = reservationService.findReservationsForSpot(spotA);

        assertThat(forSpotA).hasSize(1);
        assertThat(forSpotA.get(0).requester().name()).isEqualTo("alice");
    }

    @Test
    @DisplayName("querying reservations for an unknown parking spot throws ParkingSpotNotFoundException")
    void findingReservationsForUnknownSpotThrows() {
        assertThatExceptionOfType(ParkingSpotNotFoundException.class).isThrownBy(() ->
                reservationService.findReservationsForSpot(UNKNOWN_PARKING_SPOT_ID));
    }

    @Test
    @DisplayName("cancelling before the start time marks the reservation CANCELLED")
    void cancelsAReservationBeforeItStarts() {
        Long spotId = givenParkingSpot("C-1", ParkingType.STANDARD);
        Long userId = givenUser("alice", false, false);
        Reservation reservation = reservationService.reserve(spotId, userId, tomorrowAt(9), tomorrowAt(11));

        reservationService.cancel(reservation.id(), tomorrowAt(9).minusHours(1));

        ReservationEntity persisted = reservationRepository.findById(reservation.id()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelling at or after the start time throws and leaves the reservation ACTIVE")
    void cancellingAfterStartThrowsAndLeavesReservationActive() {
        // c2 is a standard spot so the standard policy applies
        Long spotId = givenParkingSpot("C-2", ParkingType.STANDARD);
        Long userId = givenUser("alice", false, false);
        Reservation reservation = reservationService.reserve(spotId, userId, tomorrowAt(9), tomorrowAt(11));

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
        Long spotId = givenParkingSpot("C-3", ParkingType.STANDARD);
        Long alice = givenUser("alice", false, false);
        Long bob = givenUser("bob", false, false);
        Reservation reservation = reservationService.reserve(spotId, alice, tomorrowAt(9), tomorrowAt(11));
        reservationService.cancel(reservation.id(), tomorrowAt(9).minusHours(1));

        Reservation replacement = reservationService.reserve(spotId, bob, tomorrowAt(9), tomorrowAt(11));

        assertThat(replacement.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("listAvailability marks a spot occupied only while an ACTIVE reservation covers the given instant")
    void listAvailabilityReflectsCurrentOccupancy() {
        Long spotId = givenParkingSpot("D-1", ParkingType.STANDARD);
        Long userId = givenUser("alice", false, false);
        LocalDateTime asOf = LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0);
        reservationService.reserve(spotId, userId, asOf.minusMinutes(30), asOf.plusMinutes(30));

        assertThat(occupiedNow(reservationService.listAvailability(asOf), spotId)).isTrue();
        assertThat(occupiedNow(reservationService.listAvailability(asOf.minusHours(2)), spotId)).isFalse();
    }

    private boolean occupiedNow(List<ParkingSpotAvailability> availabilities, Long spotId) {
        return availabilities.stream()
                .filter(availability -> availability.spot().id().equals(spotId))
                .findFirst()
                .orElseThrow()
                .occupiedNow();
    }
}

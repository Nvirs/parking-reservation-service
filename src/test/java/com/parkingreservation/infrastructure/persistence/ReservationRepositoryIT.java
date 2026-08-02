package com.parkingreservation.infrastructure.persistence;

import com.parkingreservation.domain.model.ParkingType;
import com.parkingreservation.domain.model.ReservationStatus;
import com.parkingreservation.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReservationRepositoryIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private static LocalDateTime tomorrowAt(int hour) {
        return LocalDateTime.now().plusDays(1).withHour(hour).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    void findsReservationsForASpecificParkingSpotOnly() {
        ParkingSpotEntity spotA = parkingSpotRepository.saveAndFlush(new ParkingSpotEntity("A-1", ParkingType.STANDARD));
        ParkingSpotEntity spotB = parkingSpotRepository.saveAndFlush(new ParkingSpotEntity("B-1", ParkingType.STANDARD));

        reservationRepository.saveAndFlush(new ReservationEntity(
                spotA, "alice", tomorrowAt(9), tomorrowAt(11), ReservationStatus.ACTIVE));
        reservationRepository.saveAndFlush(new ReservationEntity(
                spotB, "bob", tomorrowAt(9), tomorrowAt(11), ReservationStatus.ACTIVE));

        List<ReservationEntity> forSpotA = reservationRepository.findByParkingSpot_Id(spotA.getId());

        assertThat(forSpotA).hasSize(1);
        assertThat(forSpotA.get(0).getRequester()).isEqualTo("alice");
    }

    @Test
    void rejectsAReservationWhereStartIsNotBeforeEnd() {
        ParkingSpotEntity spot = parkingSpotRepository.saveAndFlush(new ParkingSpotEntity("D-4", ParkingType.STANDARD));
        LocalDateTime sameInstant = tomorrowAt(9);

        assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() ->
                reservationRepository.saveAndFlush(
                        new ReservationEntity(spot, "carol", sameInstant, sameInstant, ReservationStatus.ACTIVE)));
    }

    @Test
    void cascadesDeleteOfReservationsWhenTheirParkingSpotIsRemoved() {
        ParkingSpotEntity spot = parkingSpotRepository.saveAndFlush(new ParkingSpotEntity("E-5", ParkingType.STANDARD));
        ReservationEntity reservation = reservationRepository.saveAndFlush(new ReservationEntity(
                spot, "dave", tomorrowAt(9), tomorrowAt(10), ReservationStatus.ACTIVE));

        parkingSpotRepository.delete(spot);
        parkingSpotRepository.flush();
        entityManager.clear();

        assertThat(reservationRepository.findById(reservation.getId())).isEmpty();
    }
}

package com.parkingreservation.infrastructure.persistence;

import com.parkingreservation.domain.model.ParkingType;
import com.parkingreservation.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ParkingSpotRepositoryIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    @Test
    void savesAndReloadsAParkingSpotWithGeneratedIdAndTimestamps() {
        ParkingSpotEntity saved = parkingSpotRepository.saveAndFlush(new ParkingSpotEntity("TEST-STD-1", ParkingType.STANDARD));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<ParkingSpotEntity> reloaded = parkingSpotRepository.findById(saved.getId());

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getCode()).isEqualTo("TEST-STD-1");
        assertThat(reloaded.get().getType()).isEqualTo(ParkingType.STANDARD);
    }

    @Test
    void findsAParkingSpotByCode() {
        parkingSpotRepository.saveAndFlush(new ParkingSpotEntity("B-2", ParkingType.EV));

        Optional<ParkingSpotEntity> found = parkingSpotRepository.findByCode("B-2");

        assertThat(found).isPresent();
        assertThat(found.get().getType()).isEqualTo(ParkingType.EV);
    }

    @Test
    void returnsEmptyWhenCodeDoesNotExist() {
        assertThat(parkingSpotRepository.findByCode("does-not-exist")).isEmpty();
    }

    @Test
    void rejectsADuplicateParkingSpotCode() {
        parkingSpotRepository.saveAndFlush(new ParkingSpotEntity("C-3", ParkingType.STANDARD));

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> parkingSpotRepository.saveAndFlush(new ParkingSpotEntity("C-3", ParkingType.EV)));
    }

    @Test
    void findsParkingSpotsByTypeOrderedByCode() {
        parkingSpotRepository.saveAndFlush(new ParkingSpotEntity("ZZ-EV-2", ParkingType.EV));
        parkingSpotRepository.saveAndFlush(new ParkingSpotEntity("ZZ-EV-1", ParkingType.EV));
        parkingSpotRepository.saveAndFlush(new ParkingSpotEntity("ZZ-STD-1", ParkingType.STANDARD));

        List<ParkingSpotEntity> evSpots = parkingSpotRepository.findByTypeOrderByCode(ParkingType.EV);

        assertThat(evSpots).allSatisfy(spot -> assertThat(spot.getType()).isEqualTo(ParkingType.EV));
        assertThat(evSpots.stream().map(ParkingSpotEntity::getCode).filter(code -> code.startsWith("ZZ-EV")).toList())
                .containsExactly("ZZ-EV-1", "ZZ-EV-2");
    }
}

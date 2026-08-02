package com.parkingreservation.infrastructure.migration;

import com.parkingreservation.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

//verifies the raw schema produced by V1__init_parking_schema.sql directly
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FlywayMigrationIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long insertParkingSpot() {
        String code = "SPOT-" + UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO parking_spots (code, type) VALUES (?, 'STANDARD')", code);
        return jdbcTemplate.queryForObject("SELECT id FROM parking_spots WHERE code = ?", Long.class, code);
    }

    @Test
    void migrationCreatesTheExpectedTablesAndIndexes() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name IN ('parking_spots', 'reservations')",
                Integer.class);
        assertThat(tableCount).isEqualTo(2);

        Integer indexCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE tablename = 'reservations' AND indexname IN (" +
                        "'idx_reservations_parking_spot_id', " +
                        "'idx_reservations_start_end_time', " +
                        "'idx_reservations_spot_start_end_time')",
                Integer.class);
        assertThat(indexCount).isEqualTo(3);
    }

    @Test
    void parkingSpotTypeCheckConstraintRejectsUnknownTypes() {
        assertThatExceptionOfType(DataAccessException.class).isThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO parking_spots (code, type) VALUES (?, 'MOTORCYCLE')",
                        "BAD-TYPE-" + UUID.randomUUID()));
    }

    @Test
    void reservationStatusCheckConstraintRejectsUnknownStatuses() {
        Long spotId = insertParkingSpot();

        assertThatExceptionOfType(DataAccessException.class).isThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO reservations (parking_spot_id, requester, start_time, end_time, status) " +
                                "VALUES (?, 'alice', now() + interval '1 day', now() + interval '2 days', 'PENDING')",
                        spotId));
    }

    @Test
    void reservationTimeRangeCheckConstraintRejectsStartNotBeforeEnd() {
        Long spotId = insertParkingSpot();

        assertThatExceptionOfType(DataAccessException.class).isThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO reservations (parking_spot_id, requester, start_time, end_time, status) " +
                                "VALUES (?, 'alice', now() + interval '2 days', now() + interval '1 day', 'ACTIVE')",
                        spotId));
    }

    @Test
    void reservationForeignKeyRejectsUnknownParkingSpot() {
        assertThatExceptionOfType(DataAccessException.class).isThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO reservations (parking_spot_id, requester, start_time, end_time, status) " +
                                "VALUES (?, 'alice', now() + interval '1 day', now() + interval '2 days', 'ACTIVE')",
                        -1L));
    }
}

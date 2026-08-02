package com.parkingreservation.infrastructure.persistence;

import com.parkingreservation.domain.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<ReservationEntity, UUID> {

    List<ReservationEntity> findByParkingSpot_Id(Long parkingSpotId);

    // reservations that are active
    List<ReservationEntity> findByStatusAndStartTimeLessThanEqualAndEndTimeAfter(
            ReservationStatus status, LocalDateTime startTimeInclusive, LocalDateTime endTimeExclusiveBoundary);
}

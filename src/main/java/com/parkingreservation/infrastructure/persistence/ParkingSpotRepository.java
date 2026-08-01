package com.parkingreservation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpotEntity, UUID> {

    Optional<ParkingSpotEntity> findByCode(String code);
}

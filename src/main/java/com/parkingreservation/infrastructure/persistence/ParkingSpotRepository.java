package com.parkingreservation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpotEntity, Long> {

    Optional<ParkingSpotEntity> findByCode(String code);
}

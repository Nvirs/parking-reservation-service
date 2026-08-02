package com.parkingreservation.infrastructure.persistence;

import com.parkingreservation.domain.model.ParkingType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpotEntity, Long> {

    Optional<ParkingSpotEntity> findByCode(String code);

    List<ParkingSpotEntity> findByTypeOrderByCode(ParkingType type);
}

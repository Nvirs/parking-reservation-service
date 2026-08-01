package com.parkingreservation.application;

import com.parkingreservation.domain.model.ParkingSpot;
import com.parkingreservation.domain.model.ParkingType;
import com.parkingreservation.domain.model.Reservation;
import com.parkingreservation.domain.model.ReservationStatus;
import com.parkingreservation.domain.policy.ReservationPolicy;
import com.parkingreservation.infrastructure.persistence.ParkingSpotEntity;
import com.parkingreservation.infrastructure.persistence.ParkingSpotRepository;
import com.parkingreservation.infrastructure.persistence.ReservationEntity;
import com.parkingreservation.infrastructure.persistence.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class ReservationService {

    private final ParkingSpotRepository parkingSpotRepository;
    private final ReservationRepository reservationRepository;
    private final Map<ParkingType, ReservationPolicy> policiesByType;

    public ReservationService(ParkingSpotRepository parkingSpotRepository,
                               ReservationRepository reservationRepository,
                               List<ReservationPolicy> policies) {
        this.parkingSpotRepository = parkingSpotRepository;
        this.reservationRepository = reservationRepository;
        this.policiesByType = policies.stream()
                .collect(Collectors.toMap(ReservationPolicy::supportedType, Function.identity()));
    }

    // reserves a parking spot for the given requester and time window
    @Transactional
    public Reservation reserve(UUID parkingSpotId, String requester, LocalDateTime startTime, LocalDateTime endTime) {
        ParkingSpotEntity spotEntity = parkingSpotRepository.findById(parkingSpotId)
                .orElseThrow(() -> new ParkingSpotNotFoundException(parkingSpotId));
        ParkingSpot spot = toDomain(spotEntity);

        Reservation candidate = new Reservation(null, spot, requester, startTime, endTime);
        List<Reservation> existingReservations = reservationRepository.findByParkingSpot_Id(parkingSpotId).stream()
                .map(this::toDomain)
                .toList();
         
        // validate the reservation against the policy for this parking type                
        policyFor(spot.type()).validate(candidate, existingReservations);

        ReservationEntity entity = new ReservationEntity(
                spotEntity, requester, startTime, endTime, ReservationStatus.ACTIVE);
        return toDomain(reservationRepository.save(entity));
    }
 
    @Transactional(readOnly = true)
    public List<Reservation> findReservationsForSpot(UUID parkingSpotId) {
        if (!parkingSpotRepository.existsById(parkingSpotId)) {
            throw new ParkingSpotNotFoundException(parkingSpotId);
        }
        return reservationRepository.findByParkingSpot_Id(parkingSpotId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Transactional
    public void cancel(UUID reservationId, LocalDateTime now) {
        ReservationEntity entity = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        Reservation reservation = toDomain(entity);
        reservation.cancel(now);
        entity.setStatus(reservation.status());
    }

    private ReservationPolicy policyFor(ParkingType type) {
        ReservationPolicy policy = policiesByType.get(type);
        if (policy == null) {
            throw new IllegalStateException("No reservation policy registered for parking type " + type);
        }
        return policy;
    }

    private ParkingSpot toDomain(ParkingSpotEntity entity) {
        return new ParkingSpot(entity.getId(), entity.getCode(), entity.getType());
    }

    private Reservation toDomain(ReservationEntity entity) {
        return new Reservation(entity.getId(), toDomain(entity.getParkingSpot()), entity.getRequester(),
                entity.getStartTime(), entity.getEndTime(), entity.getStatus());
    }
}

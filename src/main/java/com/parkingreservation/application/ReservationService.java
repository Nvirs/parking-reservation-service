package com.parkingreservation.application;

import com.parkingreservation.domain.model.ParkingSpot;
import com.parkingreservation.domain.model.ParkingType;
import com.parkingreservation.domain.model.Reservation;
import com.parkingreservation.domain.model.ReservationStatus;
import com.parkingreservation.domain.model.User;
import com.parkingreservation.domain.policy.ReservationPolicy;
import com.parkingreservation.domain.policy.ReservationPolicyViolationException;
import com.parkingreservation.infrastructure.persistence.ParkingSpotEntity;
import com.parkingreservation.infrastructure.persistence.ParkingSpotRepository;
import com.parkingreservation.infrastructure.persistence.ReservationEntity;
import com.parkingreservation.infrastructure.persistence.ReservationRepository;
import com.parkingreservation.infrastructure.persistence.UserEntity;
import com.parkingreservation.infrastructure.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class ReservationService {

    private final ParkingSpotRepository parkingSpotRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final Map<ParkingType, ReservationPolicy> policiesByType;

    public ReservationService(ParkingSpotRepository parkingSpotRepository,
                               ReservationRepository reservationRepository,
                               UserRepository userRepository,
                               List<ReservationPolicy> policies) {
        this.parkingSpotRepository = parkingSpotRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.policiesByType = policies.stream()
                .collect(Collectors.toMap(ReservationPolicy::supportedType, Function.identity()));
    }

    // reserves a specific parking spot for the given requester and time window
    @Transactional
    public Reservation reserve(Long parkingSpotId, Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        ParkingSpotEntity spotEntity = parkingSpotRepository.findById(parkingSpotId)
                .orElseThrow(() -> new ParkingSpotNotFoundException(parkingSpotId));
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return createReservation(spotEntity, userEntity, startTime, endTime);
    }

    // picks a suitable parking spot for the requester and reserves it for the given time window
    @Transactional
    public Reservation reserveAutoAssign(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        User requester = toDomain(userEntity);

        for (ParkingType candidateType : candidateTypesFor(requester)) {
            for (ParkingSpotEntity spotEntity : parkingSpotRepository.findByTypeOrderByCode(candidateType)) {
                try {
                    return createReservation(spotEntity, userEntity, startTime, endTime);
                } catch (ReservationPolicyViolationException e) {
                    // spot is taken for the requested window; try the next candidate
                }
            }
        }
        throw new NoAvailableParkingSpotException(userId);
    }

    @Transactional(readOnly = true)
    public List<Reservation> findReservationsForSpot(Long parkingSpotId) {
        if (!parkingSpotRepository.existsById(parkingSpotId)) {
            throw new ParkingSpotNotFoundException(parkingSpotId);
        }
        return reservationRepository.findByParkingSpot_Id(parkingSpotId).stream()
                .map(this::toDomain)
                .toList();
    }

    // lists every parking spot together with whether it is occupied by an active reservation right now
    
    @Transactional(readOnly = true)
    public List<ParkingSpotAvailability> listAvailability(LocalDateTime asOf) {
        Set<Long> occupiedSpotIds = reservationRepository
                .findByStatusAndStartTimeLessThanEqualAndEndTimeAfter(ReservationStatus.ACTIVE, asOf, asOf).stream()
                .map(entity -> entity.getParkingSpot().getId())
                .collect(Collectors.toSet());

        return parkingSpotRepository.findAll().stream()
                .map(entity -> new ParkingSpotAvailability(toDomain(entity), occupiedSpotIds.contains(entity.getId())))
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

    private Reservation createReservation(ParkingSpotEntity spotEntity, UserEntity userEntity,
                                           LocalDateTime startTime, LocalDateTime endTime) {
        ParkingSpot spot = toDomain(spotEntity);
        User requester = toDomain(userEntity);

        Reservation candidate = new Reservation(null, spot, requester, startTime, endTime);
        List<Reservation> existingReservations = reservationRepository.findByParkingSpot_Id(spotEntity.getId()).stream()
                .map(this::toDomain)
                .toList();

        // validate the reservation against the policy for this parking type
        policyFor(spot.type()).validate(candidate, existingReservations);

        ReservationEntity entity = new ReservationEntity(
                spotEntity, userEntity, startTime, endTime, ReservationStatus.ACTIVE);
        return toDomain(reservationRepository.save(entity));
    }

    private List<ParkingType> candidateTypesFor(User requester) {
        if (requester.handicappedPermitHolder()) {
            return List.of(ParkingType.HANDICAPPED);
        }
        if (requester.electricVehicleOwner()) {
            return List.of(ParkingType.EV, ParkingType.STANDARD);
        }
        return List.of(ParkingType.STANDARD);
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

    private User toDomain(UserEntity entity) {
        return new User(entity.getId(), entity.getName(),
                entity.isElectricVehicleOwner(), entity.isHandicappedPermitHolder());
    }

    private Reservation toDomain(ReservationEntity entity) {
        return new Reservation(entity.getId(), toDomain(entity.getParkingSpot()), toDomain(entity.getUser()),
                entity.getStartTime(), entity.getEndTime(), entity.getStatus());
    }
}

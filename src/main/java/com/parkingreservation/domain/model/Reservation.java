package com.parkingreservation.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Reservation {

    private final UUID id;
    private final ParkingSpot parkingSpot;
    private final String requester;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private ReservationStatus status;

    public Reservation(UUID id, ParkingSpot parkingSpot, String requester,
                        LocalDateTime startTime, LocalDateTime endTime) {
        this(id, parkingSpot, requester, startTime, endTime, ReservationStatus.ACTIVE);
    }
    // constructor with status parameter
    public Reservation(UUID id, ParkingSpot parkingSpot, String requester,
                        LocalDateTime startTime, LocalDateTime endTime, ReservationStatus status) {
        this.parkingSpot = Objects.requireNonNull(parkingSpot, "parkingSpot must not be null");
        this.requester = requireNonBlank(requester, "requester must not be blank");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
        this.id = id;
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public void cancel(LocalDateTime now) {
        if (!now.isBefore(startTime)) {
            throw new ReservationAlreadyStartedException(
                    "Reservation %s cannot be cancelled because it has already started".formatted(id));
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public boolean isActive() {
        return status == ReservationStatus.ACTIVE;
    }

    public boolean overlaps(Reservation other) {
        return startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
    }

    public UUID id() {
        return id;
    }

    public ParkingSpot parkingSpot() {
        return parkingSpot;
    }

    public String requester() {
        return requester;
    }

    public LocalDateTime startTime() {
        return startTime;
    }

    public LocalDateTime endTime() {
        return endTime;
    }

    public ReservationStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reservation other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Reservation{id=%s, parkingSpot=%s, requester='%s', startTime=%s, endTime=%s, status=%s}"
                .formatted(id, parkingSpot, requester, startTime, endTime, status);
    }
}

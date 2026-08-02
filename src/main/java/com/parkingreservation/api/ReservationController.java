package com.parkingreservation.api;

import com.parkingreservation.api.dto.AutoAssignReservationRequest;
import com.parkingreservation.api.dto.CreateReservationRequest;
import com.parkingreservation.api.dto.ErrorResponse;
import com.parkingreservation.api.dto.ReservationResponse;
import com.parkingreservation.application.ReservationService;
import com.parkingreservation.domain.model.Reservation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Reservations", description = "Create, cancel and list parking spot reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservations")
    @Operation(summary = "Create a reservation",
            description = "Reserves a parking spot for the given requester and time window")
    @ApiResponse(responseCode = "201", description = "Reservation created")
    @ApiResponse(responseCode = "400", description = "Invalid request payload",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Parking spot not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Reservation conflicts with policy or an existing reservation",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        Reservation reservation = reservationService.reserve(
                request.parkingSpotId(), request.userId(), request.startTime(), request.endTime());
        ReservationResponse body = ReservationResponse.from(reservation);
        return ResponseEntity.created(URI.create("/api/reservations/" + reservation.id())).body(body);
    }

    @PostMapping("/reservations/auto-assign")
    @Operation(summary = "Auto-assign a reservation",
            description = "Picks a suitable free parking spot for the user and time window automatically: " +
                    "a handicapped-permit holder only gets a HANDICAPPED spot, an electric vehicle owner " +
                    "prefers EV but falls back to STANDARD, everyone else only considers STANDARD")
    @ApiResponse(responseCode = "201", description = "Reservation created")
    @ApiResponse(responseCode = "400", description = "Invalid request payload",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "No suitable parking spot is free for the requested window",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ReservationResponse> autoAssign(@Valid @RequestBody AutoAssignReservationRequest request) {
        Reservation reservation = reservationService.reserveAutoAssign(
                request.userId(), request.startTime(), request.endTime());
        ReservationResponse body = ReservationResponse.from(reservation);
        return ResponseEntity.created(URI.create("/api/reservations/" + reservation.id())).body(body);
    }

    @PostMapping("/reservations/{reservationId}/cancel")
    @Operation(summary = "Cancel a reservation",
            description = "Cancels a reservation, provided it has not started yet")
    @ApiResponse(responseCode = "204", description = "Reservation cancelled")
    @ApiResponse(responseCode = "404", description = "Reservation not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Reservation has already started",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> cancel(
            @Parameter(description = "Identifier of the reservation to cancel") @PathVariable UUID reservationId) {
        reservationService.cancel(reservationId, LocalDateTime.now());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/parking-spots/{parkingSpotId}/reservations")
    @Operation(summary = "List reservations for a parking spot",
            description = "Returns all reservations, active and cancelled, for the given parking spot")
    @ApiResponse(responseCode = "200", description = "Reservations retrieved")
    @ApiResponse(responseCode = "404", description = "Parking spot not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<ReservationResponse>> listForSpot(
            @Parameter(description = "Identifier of the parking spot") @PathVariable Long parkingSpotId) {
        List<ReservationResponse> reservations = reservationService.findReservationsForSpot(parkingSpotId).stream()
                .map(ReservationResponse::from)
                .toList();
        return ResponseEntity.ok(reservations);
    }
}

package com.parkingreservation.api;

import com.parkingreservation.api.dto.ParkingSpotResponse;
import com.parkingreservation.application.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Parking spots", description = "List parking spots and their current occupancy")
public class ParkingSpotController {

    private final ReservationService reservationService;

    public ParkingSpotController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/parking-spots")
    @Operation(summary = "List parking spots",
            description = "Returns every parking spot together with whether it is occupied right now")
    @ApiResponse(responseCode = "200", description = "Parking spots retrieved")
    public ResponseEntity<List<ParkingSpotResponse>> list() {
        List<ParkingSpotResponse> spots = reservationService.listAvailability(LocalDateTime.now()).stream()
                .map(ParkingSpotResponse::from)
                .toList();
        return ResponseEntity.ok(spots);
    }
}

package com.parkingreservation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkingreservation.api.dto.AutoAssignReservationRequest;
import com.parkingreservation.api.dto.CreateReservationRequest;
import com.parkingreservation.application.NoAvailableParkingSpotException;
import com.parkingreservation.application.ParkingSpotNotFoundException;
import com.parkingreservation.application.ReservationNotFoundException;
import com.parkingreservation.application.ReservationService;
import com.parkingreservation.application.UserNotFoundException;
import com.parkingreservation.domain.model.ParkingSpot;
import com.parkingreservation.domain.model.ParkingType;
import com.parkingreservation.domain.model.Reservation;
import com.parkingreservation.domain.model.ReservationAlreadyStartedException;
import com.parkingreservation.domain.model.ReservationStatus;
import com.parkingreservation.domain.model.User;
import com.parkingreservation.domain.policy.ReservationPolicyViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for {@link ReservationController} covering bean validation, happy paths,
 * and the {@link GlobalExceptionHandler} status mappings (400/404/409).
 */
@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

    private static final Long PARKING_SPOT_ID = 1L;
    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationService reservationService;

    private static LocalDateTime tomorrowAt(int hour) {
        return LocalDateTime.now().plusDays(1).withHour(hour).withMinute(0).withSecond(0).withNano(0);
    }

    private static Reservation reservation(UUID id, LocalDateTime start, LocalDateTime end, ReservationStatus status) {
        ParkingSpot spot = new ParkingSpot(PARKING_SPOT_ID, "A-1", ParkingType.STANDARD);
        User requester = new User(USER_ID, "alice", false, false);
        return new Reservation(id, spot, requester, start, end, status);
    }

    //create happy path

    @Test
    @DisplayName("POST /api/reservations returns 201 with the created reservation")
    void createReturnsCreatedReservation() throws Exception {
        UUID reservationId = UUID.randomUUID();
        LocalDateTime start = tomorrowAt(9);
        LocalDateTime end = tomorrowAt(11);
        given(reservationService.reserve(eq(PARKING_SPOT_ID), eq(USER_ID), eq(start), eq(end)))
                .willReturn(reservation(reservationId, start, end, ReservationStatus.ACTIVE));

        CreateReservationRequest request = new CreateReservationRequest(PARKING_SPOT_ID, USER_ID, start, end);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/reservations/" + reservationId))
                .andExpect(jsonPath("$.id").value(reservationId.toString()))
                .andExpect(jsonPath("$.parkingSpotId").value(PARKING_SPOT_ID))
                .andExpect(jsonPath("$.parkingSpotCode").value("A-1"))
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.userName").value("alice"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    //create: bean validation (400)

    @Test
    @DisplayName("POST /api/reservations rejects a null parkingSpotId with 400 and never calls the service")
    void createRejectsNullParkingSpotId() throws Exception {
        CreateReservationRequest request =
                new CreateReservationRequest(null, USER_ID, tomorrowAt(9), tomorrowAt(11));

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details", hasItem(containsString("parkingSpotId"))));

        verifyNoInteractions(reservationService);
    }

    @Test
    @DisplayName("POST /api/reservations rejects a null userId with 400")
    void createRejectsNullUserId() throws Exception {
        CreateReservationRequest request =
                new CreateReservationRequest(PARKING_SPOT_ID, null, tomorrowAt(9), tomorrowAt(11));

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("userId"))));

        verifyNoInteractions(reservationService);
    }

    @Test
    @DisplayName("POST /api/reservations rejects a null startTime with 400")
    void createRejectsNullStartTime() throws Exception {
        CreateReservationRequest request =
                new CreateReservationRequest(PARKING_SPOT_ID, USER_ID, null, tomorrowAt(11));

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("startTime"))));

        verifyNoInteractions(reservationService);
    }

    @Test
    @DisplayName("POST /api/reservations rejects a startTime in the past with 400")
    void createRejectsPastStartTime() throws Exception {
        LocalDateTime pastStart = LocalDateTime.now().minusDays(1);
        CreateReservationRequest request =
                new CreateReservationRequest(PARKING_SPOT_ID, USER_ID, pastStart, tomorrowAt(11));

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("startTime"))));

        verifyNoInteractions(reservationService);
    }

    // create: exception mappings (404, 409)

    @Test
    @DisplayName("POST /api/reservations returns 404 when the parking spot does not exist")
    void createReturns404WhenParkingSpotNotFound() throws Exception {
        LocalDateTime start = tomorrowAt(9);
        LocalDateTime end = tomorrowAt(11);
        given(reservationService.reserve(eq(PARKING_SPOT_ID), eq(USER_ID), eq(start), eq(end)))
                .willThrow(new ParkingSpotNotFoundException(PARKING_SPOT_ID));

        CreateReservationRequest request = new CreateReservationRequest(PARKING_SPOT_ID, USER_ID, start, end);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/reservations"))
                .andExpect(jsonPath("$.message", containsString(PARKING_SPOT_ID.toString())));
    }

    @Test
    @DisplayName("POST /api/reservations returns 404 when the user does not exist")
    void createReturns404WhenUserNotFound() throws Exception {
        LocalDateTime start = tomorrowAt(9);
        LocalDateTime end = tomorrowAt(11);
        given(reservationService.reserve(eq(PARKING_SPOT_ID), eq(USER_ID), eq(start), eq(end)))
                .willThrow(new UserNotFoundException(USER_ID));

        CreateReservationRequest request = new CreateReservationRequest(PARKING_SPOT_ID, USER_ID, start, end);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString(USER_ID.toString())));
    }

    @Test
    @DisplayName("POST /api/reservations returns 409 when the reservation conflicts with policy")
    void createReturns409WhenPolicyViolated() throws Exception {
        LocalDateTime start = tomorrowAt(9);
        LocalDateTime end = tomorrowAt(11);
        given(reservationService.reserve(eq(PARKING_SPOT_ID), eq(USER_ID), eq(start), eq(end)))
                .willThrow(new ReservationPolicyViolationException("Requested time slot conflicts"));

        CreateReservationRequest request = new CreateReservationRequest(PARKING_SPOT_ID, USER_ID, start, end);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Requested time slot conflicts"));
    }

    // auto-assign

    @Test
    @DisplayName("POST /api/reservations/auto-assign returns 201 with the created reservation")
    void autoAssignReturnsCreatedReservation() throws Exception {
        UUID reservationId = UUID.randomUUID();
        LocalDateTime start = tomorrowAt(9);
        LocalDateTime end = tomorrowAt(11);
        given(reservationService.reserveAutoAssign(eq(USER_ID), eq(start), eq(end)))
                .willReturn(reservation(reservationId, start, end, ReservationStatus.ACTIVE));

        AutoAssignReservationRequest request = new AutoAssignReservationRequest(USER_ID, start, end);

        mockMvc.perform(post("/api/reservations/auto-assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/reservations/" + reservationId))
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/reservations/auto-assign returns 409 when no suitable spot is free")
    void autoAssignReturns409WhenNoSpotAvailable() throws Exception {
        LocalDateTime start = tomorrowAt(9);
        LocalDateTime end = tomorrowAt(11);
        given(reservationService.reserveAutoAssign(eq(USER_ID), eq(start), eq(end)))
                .willThrow(new NoAvailableParkingSpotException(USER_ID));

        AutoAssignReservationRequest request = new AutoAssignReservationRequest(USER_ID, start, end);

        mockMvc.perform(post("/api/reservations/auto-assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // cancel

    @Test
    @DisplayName("POST /api/reservations/{id}/cancel returns 204 on success")
    void cancelReturnsNoContent() throws Exception {
        UUID reservationId = UUID.randomUUID();

        mockMvc.perform(post("/api/reservations/{reservationId}/cancel", reservationId))
                .andExpect(status().isNoContent());

        verify(reservationService).cancel(eq(reservationId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("POST /api/reservations/{id}/cancel returns 404 when the reservation does not exist")
    void cancelReturns404WhenReservationNotFound() throws Exception {
        UUID reservationId = UUID.randomUUID();
        willThrow(new ReservationNotFoundException(reservationId))
                .given(reservationService).cancel(eq(reservationId), any(LocalDateTime.class));

        mockMvc.perform(post("/api/reservations/{reservationId}/cancel", reservationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/reservations/{id}/cancel returns 409 when the reservation already started")
    void cancelReturns409WhenAlreadyStarted() throws Exception {
        UUID reservationId = UUID.randomUUID();
        willThrow(new ReservationAlreadyStartedException("Reservation " + reservationId + " cannot be cancelled"))
                .given(reservationService).cancel(eq(reservationId), any(LocalDateTime.class));

        mockMvc.perform(post("/api/reservations/{reservationId}/cancel", reservationId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    //list reservations for a spot

    @Test
    @DisplayName("GET /api/parking-spots/{id}/reservations returns 200 with the spot's reservations")
    void listForSpotReturnsReservations() throws Exception {
        LocalDateTime start = tomorrowAt(9);
        LocalDateTime end = tomorrowAt(11);
        Reservation active = reservation(UUID.randomUUID(), start, end, ReservationStatus.ACTIVE);
        Reservation cancelled = reservation(UUID.randomUUID(), start.plusDays(1), end.plusDays(1), ReservationStatus.CANCELLED);
        given(reservationService.findReservationsForSpot(PARKING_SPOT_ID)).willReturn(List.of(active, cancelled));

        mockMvc.perform(get("/api/parking-spots/{parkingSpotId}/reservations", PARKING_SPOT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].status").value("CANCELLED"));
    }

    @Test
    @DisplayName("GET /api/parking-spots/{id}/reservations returns 404 when the parking spot does not exist")
    void listForSpotReturns404WhenParkingSpotNotFound() throws Exception {
        given(reservationService.findReservationsForSpot(PARKING_SPOT_ID))
                .willThrow(new ParkingSpotNotFoundException(PARKING_SPOT_ID));

        mockMvc.perform(get("/api/parking-spots/{parkingSpotId}/reservations", PARKING_SPOT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}

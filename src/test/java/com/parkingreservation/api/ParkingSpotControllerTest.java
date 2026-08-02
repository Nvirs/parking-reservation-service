package com.parkingreservation.api;

import com.parkingreservation.application.ParkingSpotAvailability;
import com.parkingreservation.application.ReservationService;
import com.parkingreservation.domain.model.ParkingSpot;
import com.parkingreservation.domain.model.ParkingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParkingSpotController.class)
class ParkingSpotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationService reservationService;

    @Test
    @DisplayName("GET /api/parking-spots returns 200 with every spot and its current occupancy")
    void listReturnsParkingSpotsWithOccupancy() throws Exception {
        ParkingSpot free = new ParkingSpot(1L, "A-1", ParkingType.STANDARD);
        ParkingSpot occupied = new ParkingSpot(2L, "EV-1", ParkingType.EV);
        given(reservationService.listAvailability(any())).willReturn(List.of(
                new ParkingSpotAvailability(free, false),
                new ParkingSpotAvailability(occupied, true)));

        mockMvc.perform(get("/api/parking-spots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("A-1"))
                .andExpect(jsonPath("$[0].type").value("STANDARD"))
                .andExpect(jsonPath("$[0].occupiedNow").value(false))
                .andExpect(jsonPath("$[1].code").value("EV-1"))
                .andExpect(jsonPath("$[1].occupiedNow").value(true));
    }
}

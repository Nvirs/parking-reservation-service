package com.parkingreservation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkingreservation.api.dto.CreateUserRequest;
import com.parkingreservation.infrastructure.persistence.UserEntity;
import com.parkingreservation.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("GET /api/users returns 200 with every registered user")
    void listReturnsUsers() throws Exception {
        UserEntity alice = new UserEntity("alice", false, false);
        UserEntity dave = new UserEntity("dave", true, true);
        given(userRepository.findAll()).willReturn(List.of(alice, dave));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("alice"))
                .andExpect(jsonPath("$[0].electricVehicleOwner").value(false))
                .andExpect(jsonPath("$[0].handicappedPermitHolder").value(false))
                .andExpect(jsonPath("$[1].name").value("dave"))
                .andExpect(jsonPath("$[1].electricVehicleOwner").value(true))
                .andExpect(jsonPath("$[1].handicappedPermitHolder").value(true));
    }

    @Test
    @DisplayName("POST /api/users returns 201 with the created user")
    void createReturnsCreatedUser() throws Exception {
        given(userRepository.save(any(UserEntity.class))).willReturn(new UserEntity("erin", true, false));

        CreateUserRequest request = new CreateUserRequest("erin", true, false);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("erin"))
                .andExpect(jsonPath("$.electricVehicleOwner").value(true))
                .andExpect(jsonPath("$.handicappedPermitHolder").value(false));
    }

    @Test
    @DisplayName("POST /api/users rejects a blank name with 400 and never calls the repository")
    void createRejectsBlankName() throws Exception {
        CreateUserRequest request = new CreateUserRequest("  ", false, false);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("name"))));

        verifyNoInteractions(userRepository);
    }
}

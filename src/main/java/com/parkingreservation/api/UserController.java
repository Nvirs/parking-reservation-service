package com.parkingreservation.api;

import com.parkingreservation.api.dto.CreateUserRequest;
import com.parkingreservation.api.dto.UserResponse;
import com.parkingreservation.infrastructure.persistence.UserEntity;
import com.parkingreservation.infrastructure.persistence.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Users", description = "Register and list users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    @Operation(summary = "List users", description = "Returns every registered user")
    @ApiResponse(responseCode = "200", description = "Users retrieved")
    public ResponseEntity<List<UserResponse>> list() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/users")
    @Operation(summary = "Register a user", description = "Creates a new user with their name and eligibility flags")
    @ApiResponse(responseCode = "201", description = "User created")
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserEntity entity = userRepository.save(
                new UserEntity(request.name(), request.electricVehicleOwner(), request.handicappedPermitHolder()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(entity));
    }
}

package com.parkingreservation.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to register a new user")
public record CreateUserRequest(

        @NotBlank(message = "name must not be blank")
        @Size(max = 255, message = "name must be at most 255 characters")
        @Schema(description = "User's name", example = "alice", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Whether the user is registered as an electric vehicle owner",
                example = "false", defaultValue = "false")
        boolean electricVehicleOwner,

        @Schema(description = "Whether the user holds a handicapped permit",
                example = "false", defaultValue = "false")
        boolean handicappedPermitHolder
) {
}

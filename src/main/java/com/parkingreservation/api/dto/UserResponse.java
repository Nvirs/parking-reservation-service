package com.parkingreservation.api.dto;

import com.parkingreservation.infrastructure.persistence.UserEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User details returned by the API")
public record UserResponse(

        @Schema(description = "User identifier")
        Long id,

        @Schema(description = "User's name", example = "alice")
        String name,

        @Schema(description = "Whether the user is registered as an electric vehicle owner")
        boolean electricVehicleOwner,

        @Schema(description = "Whether the user holds a handicapped permit")
        boolean handicappedPermitHolder
) {

    public static UserResponse from(UserEntity entity) {
        return new UserResponse(entity.getId(), entity.getName(),
                entity.isElectricVehicleOwner(), entity.isHandicappedPermitHolder());
    }
}

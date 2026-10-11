package com.example.hotelreservation.dto;

import com.example.hotelreservation.enums.RoomStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoomRequestDto(
        @NotBlank(message = "roomNumber is required") @Size(max = 20) String roomNumber,
        @NotNull(message = "floorNumber is required") @Min(0) Integer floorNumber,
        @NotNull(message = "roomTypeId is required") Long roomTypeId,
        RoomStatus status) {
}

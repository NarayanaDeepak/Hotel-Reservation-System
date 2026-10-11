package com.example.hotelreservation.dto;

import com.example.hotelreservation.enums.RoomStatus;
import jakarta.validation.constraints.NotNull;

public record RoomStatusRequestDto(@NotNull(message = "status is required") RoomStatus status) {
}

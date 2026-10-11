package com.example.hotelreservation.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateReservationRequestDto(
        @NotNull(message = "customerId is required") Long customerId,
        @NotNull(message = "roomId is required") Long roomId,
        @NotNull(message = "checkInDate is required") @FutureOrPresent(message = "checkInDate cannot be in the past") LocalDate checkInDate,
        @NotNull(message = "checkOutDate is required") LocalDate checkOutDate,
        @Min(value = 1, message = "guests must be at least 1") int guests,
        @Size(max = 500) String specialRequests) {
}

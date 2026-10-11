package com.example.hotelreservation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;

public record RoomTypeRequestDto(
        @NotBlank(message = "name is required") @Size(max = 80) String name,
        @Size(max = 500) String description,
        @NotNull(message = "pricePerNight is required")
        @DecimalMin(value = "0.01", message = "pricePerNight must be greater than 0") BigDecimal pricePerNight,
        @Min(value = 1, message = "capacity must be at least 1") int capacity,
        Set<Long> amenityIds) {
}

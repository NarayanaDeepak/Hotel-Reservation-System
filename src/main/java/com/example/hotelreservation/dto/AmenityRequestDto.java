package com.example.hotelreservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AmenityRequestDto(
        @NotBlank(message = "name is required") @Size(max = 80) String name,
        @Size(max = 255) String description) {
}

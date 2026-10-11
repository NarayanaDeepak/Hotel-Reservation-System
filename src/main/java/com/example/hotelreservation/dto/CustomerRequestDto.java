package com.example.hotelreservation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequestDto(
        @NotBlank(message = "fullName is required") @Size(max = 120) String fullName,
        @NotBlank(message = "email is required") @Email(message = "email is invalid") String email,
        @Pattern(regexp = "^[+]?[0-9 -]{7,20}$", message = "phone is invalid") String phone) {
}

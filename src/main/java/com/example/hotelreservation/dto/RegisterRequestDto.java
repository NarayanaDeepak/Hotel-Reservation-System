package com.example.hotelreservation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @NotBlank(message = "username is required") @Size(min = 4, max = 60, message = "username must be 4-60 characters") String username,
        @NotBlank(message = "password is required") @Size(min = 8, max = 72, message = "password must be 8-72 characters") String password,
        @NotBlank(message = "fullName is required") @Size(max = 120) String fullName,
        @NotBlank(message = "email is required") @Email(message = "email is invalid") String email,
        @Pattern(regexp = "^[+]?[0-9 -]{7,20}$", message = "phone is invalid") String phone) {
}

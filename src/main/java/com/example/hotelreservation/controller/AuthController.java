package com.example.hotelreservation.controller;

import com.example.hotelreservation.dto.CustomerResponseDto;
import com.example.hotelreservation.dto.RegisterRequestDto;
import com.example.hotelreservation.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final CustomerService customerService;

    @PostMapping("/register")
    @Operation(summary = "Self-register a guest account and customer profile")
    public ResponseEntity<CustomerResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.register(request));
    }
}

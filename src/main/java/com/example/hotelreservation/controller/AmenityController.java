package com.example.hotelreservation.controller;

import com.example.hotelreservation.dto.AmenityRequestDto;
import com.example.hotelreservation.dto.AmenityResponseDto;
import com.example.hotelreservation.service.AmenityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/amenities")
@RequiredArgsConstructor
@Tag(name = "Amenities")
public class AmenityController {

    private final AmenityService amenityService;

    @PostMapping
    public ResponseEntity<AmenityResponseDto> create(@Valid @RequestBody AmenityRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(amenityService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<AmenityResponseDto>> findAll() {
        return ResponseEntity.ok(amenityService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AmenityResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(amenityService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AmenityResponseDto> update(@PathVariable Long id,
                                                     @Valid @RequestBody AmenityRequestDto request) {
        return ResponseEntity.ok(amenityService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        amenityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

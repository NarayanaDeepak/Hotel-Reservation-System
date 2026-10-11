package com.example.hotelreservation.controller;

import com.example.hotelreservation.dto.RoomTypeRequestDto;
import com.example.hotelreservation.dto.RoomTypeResponseDto;
import com.example.hotelreservation.service.RoomTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room-types")
@RequiredArgsConstructor
@Tag(name = "Room types")
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @PostMapping
    public ResponseEntity<RoomTypeResponseDto> create(@Valid @RequestBody RoomTypeRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomTypeService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<RoomTypeResponseDto>> findAll() {
        return ResponseEntity.ok(roomTypeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomTypeResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(roomTypeService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomTypeResponseDto> update(@PathVariable Long id,
                                                      @Valid @RequestBody RoomTypeRequestDto request) {
        return ResponseEntity.ok(roomTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

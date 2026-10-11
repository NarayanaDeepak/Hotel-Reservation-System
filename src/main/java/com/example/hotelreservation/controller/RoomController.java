package com.example.hotelreservation.controller;

import com.example.hotelreservation.dto.PageResponse;
import com.example.hotelreservation.dto.RoomRequestDto;
import com.example.hotelreservation.dto.RoomResponseDto;
import com.example.hotelreservation.dto.RoomStatusRequestDto;
import com.example.hotelreservation.enums.RoomStatus;
import com.example.hotelreservation.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Validated
@Tag(name = "Rooms")
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponseDto> create(@Valid @RequestBody RoomRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(request));
    }

    @GetMapping
    @Operation(summary = "List / filter rooms (paginated, sortable)")
    public ResponseEntity<PageResponse<RoomResponseDto>> search(
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) RoomStatus status,
            @PageableDefault(size = 10, sort = "roomNumber") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                roomService.search(roomTypeId, minPrice, maxPrice, minCapacity, status, pageable)));
    }

    @GetMapping("/availability")
    @Operation(summary = "Rooms free for every night in [checkIn, checkOut)")
    public ResponseEntity<PageResponse<RoomResponseDto>> availability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") @Min(1) int guests,
            @RequestParam(required = false) Long roomTypeId,
            @PageableDefault(size = 10, sort = "roomNumber") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                roomService.findAvailable(checkIn, checkOut, guests, roomTypeId, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomResponseDto> update(@PathVariable Long id, @Valid @RequestBody RoomRequestDto request) {
        return ResponseEntity.ok(roomService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Put a room into / out of maintenance")
    public ResponseEntity<RoomResponseDto> updateStatus(@PathVariable Long id,
                                                        @Valid @RequestBody RoomStatusRequestDto request) {
        return ResponseEntity.ok(roomService.updateStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

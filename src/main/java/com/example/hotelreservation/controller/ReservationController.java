package com.example.hotelreservation.controller;

import com.example.hotelreservation.dto.*;
import com.example.hotelreservation.enums.ReservationStatus;
import com.example.hotelreservation.service.PaymentService;
import com.example.hotelreservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations")
public class ReservationController {

    private static final String ADMIN_OR_OWNER = "hasRole('ADMIN') or @ownership.isReservationOwner(#id, authentication)";

    private final ReservationService reservationService;
    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @ownership.isCustomer(#request.customerId(), authentication)")
    @Operation(summary = "Create a reservation (validates dates, capacity and overlap)")
    public ResponseEntity<ReservationResponseDto> create(@Valid @RequestBody CreateReservationRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ReservationResponseDto>> findAll(
            @RequestParam(required = false) ReservationStatus status,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(reservationService.findAll(status, pageable)));
    }

    @GetMapping("/report")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reservation count per status (native query)")
    public ResponseEntity<Map<String, Long>> report() {
        return ResponseEntity.ok(reservationService.countByStatus());
    }

    @GetMapping("/{id}")
    @PreAuthorize(ADMIN_OR_OWNER)
    public ResponseEntity<ReservationResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ADMIN_OR_OWNER)
    public ResponseEntity<ReservationResponseDto> update(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateReservationRequestDto request) {
        return ResponseEntity.ok(reservationService.update(id, request));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize(ADMIN_OR_OWNER)
    @Operation(summary = "Cancel a reservation and refund its payments")
    public ResponseEntity<ReservationResponseDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.cancel(id));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize(ADMIN_OR_OWNER)
    public ResponseEntity<PaymentResponseDto> pay(@PathVariable Long id, @Valid @RequestBody PaymentRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.pay(id, request));
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize(ADMIN_OR_OWNER)
    public ResponseEntity<List<PaymentResponseDto>> payments(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.findByReservation(id));
    }
}

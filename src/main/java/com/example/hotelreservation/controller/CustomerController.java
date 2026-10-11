package com.example.hotelreservation.controller;

import com.example.hotelreservation.dto.CustomerRequestDto;
import com.example.hotelreservation.dto.CustomerResponseDto;
import com.example.hotelreservation.dto.PageResponse;
import com.example.hotelreservation.dto.ReservationResponseDto;
import com.example.hotelreservation.service.CustomerService;
import com.example.hotelreservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers")
public class CustomerController {

	private final CustomerService customerService;
	private final ReservationService reservationService;

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<PageResponse<CustomerResponseDto>> findAll(
			@PageableDefault(size = 10, sort = "id") Pageable pageable) {
		return ResponseEntity.ok(PageResponse.from(customerService.findAll(pageable)));
	}

	@GetMapping("/me")
	@Operation(summary = "Profile of the logged-in guest")
	public ResponseEntity<CustomerResponseDto> me(Authentication authentication) {
		return ResponseEntity.ok(customerService.findByUsername(authentication.getName()));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN') or @ownership.isCustomer(#id, authentication)")
	public ResponseEntity<CustomerResponseDto> findById(@PathVariable Long id) {
		return ResponseEntity.ok(customerService.findById(id));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN') or @ownership.isCustomer(#id, authentication)")
	public ResponseEntity<CustomerResponseDto> update(@PathVariable Long id,
			@Valid @RequestBody CustomerRequestDto request) {
		return ResponseEntity.ok(customerService.update(id, request));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<String> delete(@PathVariable Long id) {

		customerService.delete(id);

		return ResponseEntity.ok("Customer with ID " + id + " was deleted successfully.");
	}

	@GetMapping("/{id}/reservations")
	@PreAuthorize("hasRole('ADMIN') or @ownership.isCustomer(#id, authentication)")
	@Operation(summary = "Customer reservation history (newest first by default)")
	public ResponseEntity<PageResponse<ReservationResponseDto>> history(@PathVariable Long id,
			@PageableDefault(size = 10, sort = "checkInDate", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(PageResponse.from(reservationService.findByCustomer(id, pageable)));
	}
}

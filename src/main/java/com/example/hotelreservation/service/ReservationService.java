package com.example.hotelreservation.service;

import com.example.hotelreservation.dto.CreateReservationRequestDto;
import com.example.hotelreservation.dto.ReservationResponseDto;
import com.example.hotelreservation.dto.UpdateReservationRequestDto;
import com.example.hotelreservation.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ReservationService {
    ReservationResponseDto create(CreateReservationRequestDto request);

    ReservationResponseDto findById(Long id);

    Page<ReservationResponseDto> findAll(ReservationStatus status, Pageable pageable);

    /** Customer history. */
    Page<ReservationResponseDto> findByCustomer(Long customerId, Pageable pageable);

    ReservationResponseDto update(Long id, UpdateReservationRequestDto request);

    ReservationResponseDto cancel(Long id);

    Map<String, Long> countByStatus();
}

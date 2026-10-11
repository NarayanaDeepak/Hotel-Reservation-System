package com.example.hotelreservation.service;

import com.example.hotelreservation.dto.RoomRequestDto;
import com.example.hotelreservation.dto.RoomResponseDto;
import com.example.hotelreservation.enums.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RoomService {
    RoomResponseDto create(RoomRequestDto request);

    Page<RoomResponseDto> search(Long roomTypeId, BigDecimal minPrice, BigDecimal maxPrice,
                                 Integer minCapacity, RoomStatus status, Pageable pageable);

    Page<RoomResponseDto> findAvailable(LocalDate checkIn, LocalDate checkOut, int guests,
                                        Long roomTypeId, Pageable pageable);

    RoomResponseDto findById(Long id);

    RoomResponseDto update(Long id, RoomRequestDto request);

    RoomResponseDto updateStatus(Long id, RoomStatus status);

    void delete(Long id);
}

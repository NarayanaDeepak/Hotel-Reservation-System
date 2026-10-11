package com.example.hotelreservation.dto;

import com.example.hotelreservation.enums.RoomStatus;

import java.math.BigDecimal;

public record RoomResponseDto(Long id, String roomNumber, int floorNumber, RoomStatus status,
                              Long roomTypeId, String roomTypeName, BigDecimal pricePerNight, int capacity) {
}

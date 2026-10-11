package com.example.hotelreservation.dto;

import java.math.BigDecimal;
import java.util.List;

public record RoomTypeResponseDto(Long id, String name, String description, BigDecimal pricePerNight,
                                  int capacity, List<AmenityResponseDto> amenities) {
}

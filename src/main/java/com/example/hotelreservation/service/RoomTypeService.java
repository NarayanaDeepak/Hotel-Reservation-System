package com.example.hotelreservation.service;

import com.example.hotelreservation.dto.RoomTypeRequestDto;
import com.example.hotelreservation.dto.RoomTypeResponseDto;

import java.util.List;

public interface RoomTypeService {
    RoomTypeResponseDto create(RoomTypeRequestDto request);

    List<RoomTypeResponseDto> findAll();

    RoomTypeResponseDto findById(Long id);

    RoomTypeResponseDto update(Long id, RoomTypeRequestDto request);

    void delete(Long id);
}

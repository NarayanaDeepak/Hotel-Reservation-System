package com.example.hotelreservation.service;

import com.example.hotelreservation.dto.AmenityRequestDto;
import com.example.hotelreservation.dto.AmenityResponseDto;

import java.util.List;

public interface AmenityService {
    AmenityResponseDto create(AmenityRequestDto request);

    List<AmenityResponseDto> findAll();

    AmenityResponseDto findById(Long id);

    AmenityResponseDto update(Long id, AmenityRequestDto request);

    void delete(Long id);
}

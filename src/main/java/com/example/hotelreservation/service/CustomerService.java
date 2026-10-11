package com.example.hotelreservation.service;

import com.example.hotelreservation.dto.CustomerRequestDto;
import com.example.hotelreservation.dto.CustomerResponseDto;
import com.example.hotelreservation.dto.RegisterRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

	CustomerResponseDto register(RegisterRequestDto request);

    Page<CustomerResponseDto> findAll(Pageable pageable);

    CustomerResponseDto findById(Long id);

    CustomerResponseDto findByUsername(String username);

    CustomerResponseDto update(Long id, CustomerRequestDto request);

    void delete(Long id);
}

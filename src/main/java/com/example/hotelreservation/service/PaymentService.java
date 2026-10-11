package com.example.hotelreservation.service;

import com.example.hotelreservation.dto.PaymentRequestDto;
import com.example.hotelreservation.dto.PaymentResponseDto;

import java.util.List;

public interface PaymentService {
    PaymentResponseDto pay(Long reservationId, PaymentRequestDto request);

    List<PaymentResponseDto> findByReservation(Long reservationId);
}

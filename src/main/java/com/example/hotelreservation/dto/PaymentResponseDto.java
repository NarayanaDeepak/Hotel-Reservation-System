package com.example.hotelreservation.dto;

import com.example.hotelreservation.enums.PaymentMethod;
import com.example.hotelreservation.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponseDto(Long id, Long reservationId, BigDecimal amount, PaymentMethod method,
                                 TransactionStatus status, String transactionRef, LocalDateTime paidAt) {
}

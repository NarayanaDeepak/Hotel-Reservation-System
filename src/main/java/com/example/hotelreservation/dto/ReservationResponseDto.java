package com.example.hotelreservation.dto;

import com.example.hotelreservation.enums.PaymentStatus;
import com.example.hotelreservation.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationResponseDto(Long id, Long customerId, String customerName,
                                     Long roomId, String roomNumber, String roomTypeName,
                                     LocalDate checkInDate, LocalDate checkOutDate, long nights, int guests,
                                     BigDecimal totalAmount, BigDecimal amountPaid,
                                     ReservationStatus status, PaymentStatus paymentStatus,
                                     String specialRequests, LocalDateTime createdAt) {
}

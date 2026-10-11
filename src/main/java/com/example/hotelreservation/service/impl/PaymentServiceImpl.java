package com.example.hotelreservation.service.impl;

import com.example.hotelreservation.dto.PaymentRequestDto;
import com.example.hotelreservation.dto.PaymentResponseDto;
import com.example.hotelreservation.entity.Payment;
import com.example.hotelreservation.entity.Reservation;
import com.example.hotelreservation.enums.ReservationStatus;
import com.example.hotelreservation.enums.TransactionStatus;
import com.example.hotelreservation.exception.BadRequestException;
import com.example.hotelreservation.exception.InvalidStatusTransitionException;
import com.example.hotelreservation.exception.ResourceNotFoundException;
import com.example.hotelreservation.mapper.HotelMapper;
import com.example.hotelreservation.repository.PaymentRepository;
import com.example.hotelreservation.repository.ReservationRepository;
import com.example.hotelreservation.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final HotelMapper mapper;

    @Override
    @Transactional
    public PaymentResponseDto pay(Long reservationId, PaymentRequestDto request) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId));
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new InvalidStatusTransitionException("Payments are only accepted for CONFIRMED reservations (current: "
                    + reservation.getStatus() + ")");
        }
        BigDecimal outstanding = reservation.getTotalAmount().subtract(reservation.getAmountPaid());
        if (request.amount().compareTo(outstanding) > 0) {
            throw new BadRequestException("Amount exceeds the outstanding balance of " + outstanding);
        }

        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setAmount(request.amount());
        payment.setMethod(request.method());
        payment.setStatus(TransactionStatus.SUCCESS);
        payment.setTransactionRef("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        payment.setPaidAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        reservation.setAmountPaid(reservation.getAmountPaid().add(request.amount()));
        reservation.refreshPaymentStatus();
        reservationRepository.save(reservation);
        log.info("Payment {} of {} accepted for reservation {} -> {}", saved.getTransactionRef(), request.amount(),
                reservationId, reservation.getPaymentStatus());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> findByReservation(Long reservationId) {
        if (!reservationRepository.existsById(reservationId)) {
            throw new ResourceNotFoundException("Reservation", reservationId);
        }
        return paymentRepository.findByReservationIdOrderByPaidAtAsc(reservationId).stream()
                .map(mapper::toResponse).toList();
    }
}

package com.example.hotelreservation.service;

import com.example.hotelreservation.dto.PaymentRequestDto;
import com.example.hotelreservation.dto.PaymentResponseDto;
import com.example.hotelreservation.entity.Payment;
import com.example.hotelreservation.entity.Reservation;
import com.example.hotelreservation.enums.PaymentMethod;
import com.example.hotelreservation.enums.PaymentStatus;
import com.example.hotelreservation.enums.ReservationStatus;
import com.example.hotelreservation.exception.BadRequestException;
import com.example.hotelreservation.exception.InvalidStatusTransitionException;
import com.example.hotelreservation.exception.ResourceNotFoundException;
import com.example.hotelreservation.mapper.HotelMapper;
import com.example.hotelreservation.repository.PaymentRepository;
import com.example.hotelreservation.repository.ReservationRepository;
import com.example.hotelreservation.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ReservationRepository reservationRepository;

    private PaymentServiceImpl service;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        service = new PaymentServiceImpl(paymentRepository, reservationRepository, new HotelMapper());
        reservation = new Reservation();
        reservation.setId(7L);
        reservation.setTotalAmount(new BigDecimal("3000.00"));
        reservation.setAmountPaid(BigDecimal.ZERO);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPaymentStatus(PaymentStatus.UNPAID);
    }

    @Test
    void pay_partialAmount_marksPartiallyPaid() {
        when(reservationRepository.findById(7L)).thenReturn(Optional.of(reservation));
        when(paymentRepository.save(any(Payment.class))).then(returnsFirstArg());

        PaymentResponseDto result = service.pay(7L, new PaymentRequestDto(new BigDecimal("1000.00"), PaymentMethod.UPI));

        assertThat(result.amount()).isEqualByComparingTo("1000.00");
        assertThat(result.transactionRef()).startsWith("TXN-");
        assertThat(reservation.getAmountPaid()).isEqualByComparingTo("1000.00");
        assertThat(reservation.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
    }

    @Test
    void pay_remainingBalance_marksPaid() {
        reservation.setAmountPaid(new BigDecimal("1000.00"));
        reservation.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        when(reservationRepository.findById(7L)).thenReturn(Optional.of(reservation));
        when(paymentRepository.save(any(Payment.class))).then(returnsFirstArg());

        service.pay(7L, new PaymentRequestDto(new BigDecimal("2000.00"), PaymentMethod.CARD));

        assertThat(reservation.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void pay_moreThanOutstanding_throwsBadRequest() {
        when(reservationRepository.findById(7L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.pay(7L, new PaymentRequestDto(new BigDecimal("3000.01"), PaymentMethod.CASH)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("outstanding");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void pay_cancelledReservation_throwsInvalidTransition() {
        reservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findById(7L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.pay(7L, new PaymentRequestDto(new BigDecimal("10.00"), PaymentMethod.CARD)))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void findByReservation_unknownReservation_throwsNotFound() {
        when(reservationRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.findByReservation(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}

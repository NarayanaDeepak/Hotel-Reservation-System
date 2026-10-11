package com.example.hotelreservation.service;

import com.example.hotelreservation.dto.CreateReservationRequestDto;
import com.example.hotelreservation.dto.ReservationResponseDto;
import com.example.hotelreservation.dto.UpdateReservationRequestDto;
import com.example.hotelreservation.entity.*;
import com.example.hotelreservation.enums.PaymentMethod;
import com.example.hotelreservation.enums.PaymentStatus;
import com.example.hotelreservation.enums.ReservationStatus;
import com.example.hotelreservation.enums.RoomStatus;
import com.example.hotelreservation.enums.TransactionStatus;
import com.example.hotelreservation.exception.BadRequestException;
import com.example.hotelreservation.exception.InvalidStatusTransitionException;
import com.example.hotelreservation.exception.ResourceNotFoundException;
import com.example.hotelreservation.exception.RoomNotAvailableException;
import com.example.hotelreservation.mapper.HotelMapper;
import com.example.hotelreservation.repository.CustomerRepository;
import com.example.hotelreservation.repository.PaymentRepository;
import com.example.hotelreservation.repository.ReservationRepository;
import com.example.hotelreservation.repository.RoomRepository;
import com.example.hotelreservation.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private PaymentRepository paymentRepository;

    private ReservationServiceImpl service;
    private Customer customer;
    private Room room;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        service = new ReservationServiceImpl(reservationRepository, customerRepository, roomRepository,
                paymentRepository, new HotelMapper());

        customer = new Customer();
        customer.setId(1L);
        customer.setFullName("Asha Rao");

        RoomType type = new RoomType();
        type.setId(1L);
        type.setName("Deluxe");
        type.setPricePerNight(new BigDecimal("1000.00"));
        type.setCapacity(2);

        room = new Room();
        room.setId(10L);
        room.setRoomNumber("101");
        room.setRoomType(type);
        room.setStatus(RoomStatus.AVAILABLE);
    }

    private CreateReservationRequestDto request(LocalDate in, LocalDate out, int guests) {
        return new CreateReservationRequestDto(1L, 10L, in, out, guests, null);
    }

    private void stubCustomerAndRoom() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(roomRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(room));
    }

    private Reservation existing(ReservationStatus status, LocalDate in, LocalDate out) {
        Reservation r = new Reservation();
        r.setId(5L);
        r.setCustomer(customer);
        r.setRoom(room);
        r.setCheckInDate(in);
        r.setCheckOutDate(out);
        r.setGuests(2);
        r.setTotalAmount(new BigDecimal("3000.00"));
        r.setStatus(status);
        return r;
    }

    // ------------------------------------------------------------------ create

    @Test
    void create_calculatesTotalFromNightsAndSavesConfirmedReservation() {
        stubCustomerAndRoom();
        when(reservationRepository.existsOverlap(eq(10L), any(), any(), isNull())).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setId(99L);
            return r;
        });

        ReservationResponseDto result = service.create(request(today.plusDays(5), today.plusDays(8), 2));

        assertThat(result.id()).isEqualTo(99L);
        assertThat(result.nights()).isEqualTo(3);
        assertThat(result.totalAmount()).isEqualByComparingTo("3000.00");
        assertThat(result.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(result.roomNumber()).isEqualTo("101");
    }

    @Test
    void create_overlappingStay_throwsRoomNotAvailableAndDoesNotSave() {
        stubCustomerAndRoom();
        when(reservationRepository.existsOverlap(eq(10L), any(), any(), isNull())).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(today.plusDays(5), today.plusDays(8), 2)))
                .isInstanceOf(RoomNotAvailableException.class)
                .hasMessageContaining("already booked");
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void create_checkOutNotAfterCheckIn_throwsBadRequest() {
        assertThatThrownBy(() -> service.create(request(today.plusDays(5), today.plusDays(5), 2)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("checkOutDate must be after checkInDate");
    }

    @Test
    void create_checkInInThePast_throwsBadRequest() {
        assertThatThrownBy(() -> service.create(request(today.minusDays(1), today.plusDays(2), 2)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("past");
    }

    @Test
    void create_stayLongerThanMaximum_throwsBadRequest() {
        assertThatThrownBy(() -> service.create(request(today.plusDays(1), today.plusDays(40), 2)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceed");
    }

    @Test
    void create_moreGuestsThanCapacity_throwsBadRequest() {
        stubCustomerAndRoom();

        assertThatThrownBy(() -> service.create(request(today.plusDays(5), today.plusDays(6), 3)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at most 2 guests");
    }

    @Test
    void create_roomUnderMaintenance_throwsRoomNotAvailable() {
        room.setStatus(RoomStatus.MAINTENANCE);
        stubCustomerAndRoom();

        assertThatThrownBy(() -> service.create(request(today.plusDays(5), today.plusDays(6), 1)))
                .isInstanceOf(RoomNotAvailableException.class)
                .hasMessageContaining("MAINTENANCE");
    }

    @Test
    void create_unknownCustomer_throwsNotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(today.plusDays(5), today.plusDays(6), 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------------ cancel

    @Test
    void cancel_futureConfirmedReservation_cancelsAndRefundsPayments() {
        Reservation reservation = existing(ReservationStatus.CONFIRMED, today.plusDays(5), today.plusDays(8));
        reservation.setAmountPaid(new BigDecimal("500.00"));
        reservation.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setAmount(new BigDecimal("500.00"));
        payment.setMethod(PaymentMethod.CARD);
        payment.setStatus(TransactionStatus.SUCCESS);

        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationIdOrderByPaidAtAsc(5L)).thenReturn(List.of(payment));
        when(reservationRepository.save(any(Reservation.class))).then(returnsFirstArg());

        ReservationResponseDto result = service.cancel(5L);

        assertThat(result.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(result.amountPaid()).isEqualByComparingTo("0");
        assertThat(payment.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
    }

    @Test
    void cancel_alreadyCompleted_throwsInvalidTransition() {
        Reservation reservation = existing(ReservationStatus.COMPLETED, today.minusDays(5), today.minusDays(2));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.cancel(5L)).isInstanceOf(InvalidStatusTransitionException.class);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancel_stayAlreadyStarted_throwsInvalidTransition() {
        Reservation reservation = existing(ReservationStatus.CONFIRMED, today, today.plusDays(2));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.cancel(5L))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("already started");
    }

    // ------------------------------------------------------------------ update / find

    @Test
    void update_cancelledReservation_throwsInvalidTransition() {
        Reservation reservation = existing(ReservationStatus.CANCELLED, today.plusDays(5), today.plusDays(8));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        UpdateReservationRequestDto update = new UpdateReservationRequestDto(10L, today.plusDays(6),
                today.plusDays(9), 2, null);
        assertThatThrownBy(() -> service.update(5L, update)).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void update_ignoresItsOwnBookingWhenCheckingOverlap_andRecalculatesTotal() {
        Reservation reservation = existing(ReservationStatus.CONFIRMED, today.plusDays(5), today.plusDays(8));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        when(roomRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(room));
        when(reservationRepository.existsOverlap(10L, today.plusDays(5), today.plusDays(7), 5L)).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).then(returnsFirstArg());

        ReservationResponseDto result = service.update(5L,
                new UpdateReservationRequestDto(10L, today.plusDays(5), today.plusDays(7), 2, "late arrival"));

        assertThat(result.totalAmount()).isEqualByComparingTo("2000.00");
        assertThat(result.specialRequests()).isEqualTo("late arrival");
    }

    @Test
    void findById_unknown_throwsNotFound() {
        when(reservationRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(404L)).isInstanceOf(ResourceNotFoundException.class);
    }
}

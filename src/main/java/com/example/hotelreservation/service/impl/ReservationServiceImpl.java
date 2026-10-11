package com.example.hotelreservation.service.impl;

import com.example.hotelreservation.dto.CreateReservationRequestDto;
import com.example.hotelreservation.dto.ReservationResponseDto;
import com.example.hotelreservation.dto.UpdateReservationRequestDto;
import com.example.hotelreservation.entity.Customer;
import com.example.hotelreservation.entity.Payment;
import com.example.hotelreservation.entity.Reservation;
import com.example.hotelreservation.entity.Room;
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
import com.example.hotelreservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    static final int MAX_NIGHTS = 30;

    private final ReservationRepository reservationRepository;
    private final CustomerRepository customerRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;
    private final HotelMapper mapper;

    @Override
    @Transactional
    public ReservationResponseDto create(CreateReservationRequestDto request) {
        validateDates(request.checkInDate(), request.checkOutDate());
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.customerId()));
        Room room = lockRoom(request.roomId());
        validateRoom(room, request.guests());
        ensureFree(room, request.checkInDate(), request.checkOutDate(), null);

        Reservation reservation = new Reservation();
        reservation.setCustomer(customer);
        reservation.setRoom(room);
        reservation.setCheckInDate(request.checkInDate());
        reservation.setCheckOutDate(request.checkOutDate());
        reservation.setGuests(request.guests());
        reservation.setSpecialRequests(request.specialRequests());
        reservation.setTotalAmount(totalFor(room, request.checkInDate(), request.checkOutDate()));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPaymentStatus(PaymentStatus.UNPAID);

        Reservation saved = reservationRepository.save(reservation);
        log.info("Reservation {} created: room {} {} -> {}", saved.getId(), room.getRoomNumber(),
                saved.getCheckInDate(), saved.getCheckOutDate());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponseDto findById(Long id) {
        return mapper.toResponse(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationResponseDto> findAll(ReservationStatus status, Pageable pageable) {
        Page<Reservation> page = status == null
                ? reservationRepository.findAll(pageable)
                : reservationRepository.findByStatus(status, pageable);
        return page.map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationResponseDto> findByCustomer(Long customerId, Pageable pageable) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer", customerId);
        }
        return reservationRepository.findByCustomerId(customerId, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public ReservationResponseDto update(Long id, UpdateReservationRequestDto request) {
        Reservation reservation = getEntity(id);
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new InvalidStatusTransitionException("Only CONFIRMED reservations can be modified (current: "
                    + reservation.getStatus() + ")");
        }
        validateDates(request.checkInDate(), request.checkOutDate());
        Room room = lockRoom(request.roomId());
        validateRoom(room, request.guests());
        ensureFree(room, request.checkInDate(), request.checkOutDate(), id);

        reservation.setRoom(room);
        reservation.setCheckInDate(request.checkInDate());
        reservation.setCheckOutDate(request.checkOutDate());
        reservation.setGuests(request.guests());
        reservation.setSpecialRequests(request.specialRequests());
        reservation.setTotalAmount(totalFor(room, request.checkInDate(), request.checkOutDate()));
        reservation.refreshPaymentStatus();
        log.info("Reservation {} updated", id);
        return mapper.toResponse(reservationRepository.save(reservation));
    }

    @Override
    @Transactional
    public ReservationResponseDto cancel(Long id) {
        Reservation reservation = getEntity(id);
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new InvalidStatusTransitionException("Reservation is already " + reservation.getStatus()
                    + " and cannot be cancelled");
        }
        if (!reservation.getCheckInDate().isAfter(LocalDate.now())) {
            throw new InvalidStatusTransitionException("The stay has already started and cannot be cancelled");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);

        List<Payment> payments = paymentRepository.findByReservationIdOrderByPaidAtAsc(id);
        boolean refunded = false;
        for (Payment payment : payments) {
            if (payment.getStatus() == TransactionStatus.SUCCESS) {
                payment.setStatus(TransactionStatus.REFUNDED);
                refunded = true;
            }
        }
        if (refunded) {
            paymentRepository.saveAll(payments);
            reservation.setAmountPaid(BigDecimal.ZERO);
            reservation.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        log.info("Reservation {} cancelled (refund issued: {})", id, refunded);
        return mapper.toResponse(reservationRepository.save(reservation));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByStatus() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (ReservationStatus status : ReservationStatus.values()) {
            result.put(status.name(), 0L);
        }
        reservationRepository.countByStatus().forEach(row -> result.put(row.getStatus(), row.getTotal()));
        return result;
    }

    // ---------------------------------------------------------------- helpers

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new BadRequestException("checkInDate and checkOutDate are required");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new BadRequestException("checkInDate cannot be in the past");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BadRequestException("checkOutDate must be after checkInDate");
        }
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > MAX_NIGHTS) {
            throw new BadRequestException("A stay cannot exceed " + MAX_NIGHTS + " nights");
        }
    }

    private Room lockRoom(Long roomId) {
        return roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
    }

    private void validateRoom(Room room, int guests) {
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new RoomNotAvailableException("Room " + room.getRoomNumber() + " is not open for booking ("
                    + room.getStatus() + ")");
        }
        if (guests > room.getRoomType().getCapacity()) {
            throw new BadRequestException("Room " + room.getRoomNumber() + " holds at most "
                    + room.getRoomType().getCapacity() + " guests");
        }
    }

    private void ensureFree(Room room, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        if (reservationRepository.existsOverlap(room.getId(), checkIn, checkOut, excludeReservationId)) {
            throw new RoomNotAvailableException("Room " + room.getRoomNumber()
                    + " is already booked between " + checkIn + " and " + checkOut);
        }
    }

    private BigDecimal totalFor(Room room, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return room.getRoomType().getPricePerNight().multiply(BigDecimal.valueOf(nights));
    }

    private Reservation getEntity(Long id) {
        return reservationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
    }
}

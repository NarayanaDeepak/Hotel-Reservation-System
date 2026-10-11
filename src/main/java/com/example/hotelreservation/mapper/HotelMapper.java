package com.example.hotelreservation.mapper;

import com.example.hotelreservation.dto.*;
import com.example.hotelreservation.entity.*;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Comparator;


@Component
public class HotelMapper {

    public AmenityResponseDto toResponse(Amenity a) {
        return new AmenityResponseDto(a.getId(), a.getName(), a.getDescription());
    }

    public RoomTypeResponseDto toResponse(RoomType t) {
        return new RoomTypeResponseDto(t.getId(), t.getName(), t.getDescription(), t.getPricePerNight(),
                t.getCapacity(),
                t.getAmenities().stream()
                        .sorted(Comparator.comparing(Amenity::getName))
                        .map(this::toResponse).toList());
    }

    public RoomResponseDto toResponse(Room r) {
        RoomType t = r.getRoomType();
        return new RoomResponseDto(r.getId(), r.getRoomNumber(), r.getFloorNumber(), r.getStatus(),
                t.getId(), t.getName(), t.getPricePerNight(), t.getCapacity());
    }

    public CustomerResponseDto toResponse(Customer c) {
        return new CustomerResponseDto(c.getId(), c.getFullName(), c.getEmail(), c.getPhone());
    }

    public ReservationResponseDto toResponse(Reservation r) {
        long nights = ChronoUnit.DAYS.between(r.getCheckInDate(), r.getCheckOutDate());
        return new ReservationResponseDto(r.getId(),
                r.getCustomer().getId(), r.getCustomer().getFullName(),
                r.getRoom().getId(), r.getRoom().getRoomNumber(), r.getRoom().getRoomType().getName(),
                r.getCheckInDate(), r.getCheckOutDate(), nights, r.getGuests(),
                r.getTotalAmount(), r.getAmountPaid(), r.getStatus(), r.getPaymentStatus(),
                r.getSpecialRequests(), r.getCreatedAt());
    }

    public PaymentResponseDto toResponse(Payment p) {
        return new PaymentResponseDto(p.getId(), p.getReservation().getId(), p.getAmount(), p.getMethod(),
                p.getStatus(), p.getTransactionRef(), p.getPaidAt());
    }
}

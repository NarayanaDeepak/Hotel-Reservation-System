package com.example.hotelreservation.repository;

import com.example.hotelreservation.entity.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = "reservation")
    List<Payment> findByReservationIdOrderByPaidAtAsc(Long reservationId);
}

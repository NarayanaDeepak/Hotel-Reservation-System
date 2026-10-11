package com.example.hotelreservation.entity;

import com.example.hotelreservation.enums.PaymentStatus;
import com.example.hotelreservation.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservations", indexes = {
        @Index(name = "idx_res_room_dates", columnList = "room_id, check_in_date, check_out_date"),
        @Index(name = "idx_res_customer", columnList = "customer_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Reservation extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /** Reservation -> Room (many-to-one). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    /** Check-out day is exclusive: a stay 10th -> 12th occupies the nights of the 10th and 11th. */
    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(nullable = false)
    private int guests;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(length = 500)
    private String specialRequests;

    @OneToMany(mappedBy = "reservation")
    private List<Payment> payments = new ArrayList<>();

    /** Recomputes paymentStatus from amountPaid vs totalAmount. */
    public void refreshPaymentStatus() {
        if (amountPaid == null || amountPaid.signum() <= 0) {
            paymentStatus = PaymentStatus.UNPAID;
        } else if (amountPaid.compareTo(totalAmount) >= 0) {
            paymentStatus = PaymentStatus.PAID;
        } else {
            paymentStatus = PaymentStatus.PARTIALLY_PAID;
        }
    }
}

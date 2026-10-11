package com.example.hotelreservation.service;

import com.example.hotelreservation.entity.Customer;
import com.example.hotelreservation.entity.Reservation;
import com.example.hotelreservation.entity.Room;
import com.example.hotelreservation.entity.RoomType;
import com.example.hotelreservation.enums.ReservationStatus;
import com.example.hotelreservation.repository.ReservationRepository;
import com.example.hotelreservation.scheduler.StayCompletionScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The "stay completion" rule (scheduler) and the @Version column it must respect. */
@DataJpaTest
class StayCompletionRuleTest {

    @Autowired
    private TestEntityManager em;
    @Autowired
    private ReservationRepository reservationRepository;

    private final LocalDate today = LocalDate.now();
    private Room room;
    private Customer customer;

    @BeforeEach
    void setUp() {
        RoomType type = new RoomType();
        type.setName("Deluxe");
        type.setPricePerNight(new BigDecimal("4000.00"));
        type.setCapacity(2);
        em.persist(type);
        room = new Room();
        room.setRoomNumber("201");
        room.setFloorNumber(2);
        room.setRoomType(type);
        em.persist(room);
        customer = new Customer();
        customer.setFullName("Test Guest");
        customer.setEmail("test@example.com");
        em.persistAndFlush(customer);
    }

    @Test
    void scheduler_completesOnlyConfirmedStaysThatEndedBeforeToday() {
        Reservation ended = reserve(today.minusDays(5), today.minusDays(2), ReservationStatus.CONFIRMED);
        Reservation checkingOutToday = reserve(today.minusDays(2), today, ReservationStatus.CONFIRMED);
        Reservation future = reserve(today.plusDays(3), today.plusDays(5), ReservationStatus.CONFIRMED);
        Reservation cancelled = reserve(today.minusDays(9), today.minusDays(7), ReservationStatus.CANCELLED);

        new StayCompletionScheduler(reservationRepository).completeFinishedStays();
        em.clear();

        assertThat(statusOf(ended)).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(statusOf(checkingOutToday)).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(statusOf(future)).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(statusOf(cancelled)).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void markCompleted_returnsNumberOfUpdatedRows() {
        reserve(today.minusDays(5), today.minusDays(2), ReservationStatus.CONFIRMED);
        reserve(today.minusDays(4), today.minusDays(1), ReservationStatus.CONFIRMED);
        reserve(today.plusDays(1), today.plusDays(2), ReservationStatus.CONFIRMED);

        assertThat(reservationRepository.markCompleted(today, LocalDateTime.now())).isEqualTo(2);
    }

    @Test
    void newReservation_startsAtVersionZero_andBulkUpdateIncrementsIt() {
        Reservation ended = reserve(today.minusDays(5), today.minusDays(2), ReservationStatus.CONFIRMED);
        assertThat(ended.getVersion()).isZero();

        reservationRepository.markCompleted(today, LocalDateTime.now());
        em.clear();

        assertThat(reservationRepository.findById(ended.getId()).orElseThrow().getVersion()).isEqualTo(1L);
    }

    @Test
    void staleCopy_cannotOverwriteBulkUpdate_optimisticLock() {
        Reservation reservation = reserve(today.minusDays(5), today.minusDays(2), ReservationStatus.CONFIRMED);
        em.detach(reservation); // an old copy (version 0), e.g. held by a slow request

        reservationRepository.markCompleted(today, LocalDateTime.now()); // DB row is now version 1

        reservation.setSpecialRequests("changed from a stale copy");
        assertThatThrownBy(() -> reservationRepository.saveAndFlush(reservation))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    private ReservationStatus statusOf(Reservation r) {
        return reservationRepository.findById(r.getId()).orElseThrow().getStatus();
    }

    private Reservation reserve(LocalDate in, LocalDate out, ReservationStatus status) {
        Reservation r = new Reservation();
        r.setCustomer(customer);
        r.setRoom(room);
        r.setCheckInDate(in);
        r.setCheckOutDate(out);
        r.setGuests(1);
        r.setTotalAmount(new BigDecimal("4000.00"));
        r.setStatus(status);
        return em.persistAndFlush(r);
    }
}

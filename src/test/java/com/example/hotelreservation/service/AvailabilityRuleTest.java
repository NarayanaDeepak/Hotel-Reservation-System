package com.example.hotelreservation.service;

import com.example.hotelreservation.entity.Customer;
import com.example.hotelreservation.entity.Reservation;
import com.example.hotelreservation.entity.Room;
import com.example.hotelreservation.entity.RoomType;
import com.example.hotelreservation.enums.ReservationStatus;
import com.example.hotelreservation.enums.RoomStatus;
import com.example.hotelreservation.repository.ReservationRepository;
import com.example.hotelreservation.repository.RoomRepository;
import com.example.hotelreservation.specification.RoomSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AvailabilityRuleTest {

    @Autowired
    private TestEntityManager em;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private RoomRepository roomRepository;

    private final LocalDate today = LocalDate.now();
    private RoomType deluxe;
    private Room room1;
    private Room room2;
    private Customer customer;

    @BeforeEach
    void setUp() {
        deluxe = persistType("Deluxe", "4000.00", 2);
        room1 = persistRoom("201", deluxe, RoomStatus.AVAILABLE);
        room2 = persistRoom("202", deluxe, RoomStatus.AVAILABLE);
        customer = new Customer();
        customer.setFullName("Test Guest");
        customer.setEmail("test@example.com");
        em.persist(customer);
        em.flush();
    }

    // ------------------------------------------------------------ overlap

    @Test
    void existsOverlap_detectsOverlapsButAllowsBackToBackStays() {
        Reservation existing = reserve(room1, today.plusDays(10), today.plusDays(15), ReservationStatus.CONFIRMED);

        assertThat(reservationRepository.existsOverlap(room1.getId(), today.plusDays(12), today.plusDays(14), null)).isTrue();
        assertThat(reservationRepository.existsOverlap(room1.getId(), today.plusDays(9), today.plusDays(11), null)).isTrue();
        assertThat(reservationRepository.existsOverlap(room1.getId(), today.plusDays(8), today.plusDays(20), null)).isTrue();
        // guest leaves on the 15th, next guest arrives on the 15th -> allowed
        assertThat(reservationRepository.existsOverlap(room1.getId(), today.plusDays(15), today.plusDays(18), null)).isFalse();
        assertThat(reservationRepository.existsOverlap(room1.getId(), today.plusDays(7), today.plusDays(10), null)).isFalse();
        // another room is unaffected
        assertThat(reservationRepository.existsOverlap(room2.getId(), today.plusDays(12), today.plusDays(14), null)).isFalse();
        // a reservation never conflicts with itself (used by update)
        assertThat(reservationRepository.existsOverlap(room1.getId(), today.plusDays(12), today.plusDays(14), existing.getId())).isFalse();
    }

    @Test
    void existsOverlap_ignoresCancelledReservations() {
        reserve(room1, today.plusDays(10), today.plusDays(15), ReservationStatus.CANCELLED);

        assertThat(reservationRepository.existsOverlap(room1.getId(), today.plusDays(11), today.plusDays(13), null)).isFalse();
    }

    // ------------------------------------------------------------ availability

    @Test
    void findAvailableRooms_excludesBookedMaintenanceAndTooSmallRooms() {
        reserve(room1, today.plusDays(10), today.plusDays(15), ReservationStatus.CONFIRMED);
        Room maintenance = persistRoom("203", deluxe, RoomStatus.MAINTENANCE);
        RoomType single = persistType("Single", "1500.00", 1);
        Room tiny = persistRoom("101", single, RoomStatus.AVAILABLE);

        Page<Room> forTwo = roomRepository.findAvailableRooms(
                today.plusDays(12), today.plusDays(13), 2, null, PageRequest.of(0, 10));
        List<Long> ids = forTwo.getContent().stream().map(Room::getId).toList();

        assertThat(ids).containsExactly(room2.getId());
        assertThat(ids).doesNotContain(room1.getId(), maintenance.getId(), tiny.getId());

        Page<Room> forOne = roomRepository.findAvailableRooms(today.plusDays(12), today.plusDays(13), 1, null,
                PageRequest.of(0, 10));
        assertThat(forOne.getContent()).extracting(Room::getId).containsExactlyInAnyOrder(room2.getId(), tiny.getId());

        Page<Room> filtered = roomRepository.findAvailableRooms(today.plusDays(12), today.plusDays(13), 1,
                single.getId(), PageRequest.of(0, 10));
        assertThat(filtered.getContent()).extracting(Room::getId).containsExactly(tiny.getId());
    }

    @Test
    void findAvailableRooms_roomBecomesFreeAfterCheckOut() {
        reserve(room1, today.plusDays(10), today.plusDays(15), ReservationStatus.CONFIRMED);

        Page<Room> result = roomRepository.findAvailableRooms(today.plusDays(15), today.plusDays(17), 2, null,
                PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Room::getId).contains(room1.getId(), room2.getId());
    }

    // ------------------------------------------------------------ history / report / specification

    @Test
    void findByCustomerId_returnsOnlyThatCustomersReservations() {
        reserve(room1, today.plusDays(1), today.plusDays(2), ReservationStatus.CONFIRMED);
        reserve(room1, today.plusDays(3), today.plusDays(4), ReservationStatus.CONFIRMED);
        Customer other = new Customer();
        other.setFullName("Other");
        other.setEmail("other@example.com");
        em.persistAndFlush(other);
        Reservation theirs = new Reservation();
        theirs.setCustomer(other);
        theirs.setRoom(room2);
        theirs.setCheckInDate(today.plusDays(1));
        theirs.setCheckOutDate(today.plusDays(2));
        theirs.setGuests(1);
        theirs.setTotalAmount(new BigDecimal("4000.00"));
        em.persistAndFlush(theirs);

        Page<Reservation> history = reservationRepository.findByCustomerId(customer.getId(), PageRequest.of(0, 10));

        assertThat(history.getTotalElements()).isEqualTo(2);
        assertThat(history.getContent()).allMatch(r -> r.getCustomer().getId().equals(customer.getId()));
    }

    @Test
    void countByStatus_nativeQueryGroupsRows() {
        reserve(room1, today.plusDays(1), today.plusDays(2), ReservationStatus.CONFIRMED);
        reserve(room1, today.plusDays(3), today.plusDays(4), ReservationStatus.CONFIRMED);
        reserve(room2, today.plusDays(3), today.plusDays(4), ReservationStatus.CANCELLED);

        var counts = reservationRepository.countByStatus();

        assertThat(counts).anySatisfy(c -> {
            assertThat(c.getStatus()).isEqualTo("CONFIRMED");
            assertThat(c.getTotal()).isEqualTo(2L);
        });
        assertThat(counts).anySatisfy(c -> {
            assertThat(c.getStatus()).isEqualTo("CANCELLED");
            assertThat(c.getTotal()).isEqualTo(1L);
        });
    }

    @Test
    void roomSpecification_filtersByPriceAndCapacity() {
        RoomType single = persistType("Single", "1500.00", 1);
        Room cheap = persistRoom("101", single, RoomStatus.AVAILABLE);

        Page<Room> cheapRooms = roomRepository.findAll(
                RoomSpecification.filter(null, null, new BigDecimal("2000.00"), null, null), PageRequest.of(0, 10));
        Page<Room> bigRooms = roomRepository.findAll(
                RoomSpecification.filter(null, null, null, 2, RoomStatus.AVAILABLE), PageRequest.of(0, 10));

        assertThat(cheapRooms.getContent()).extracting(Room::getId).containsExactly(cheap.getId());
        assertThat(bigRooms.getContent()).extracting(Room::getId).containsExactlyInAnyOrder(room1.getId(), room2.getId());
    }

    // ------------------------------------------------------------ helpers

    private RoomType persistType(String name, String price, int capacity) {
        RoomType t = new RoomType();
        t.setName(name);
        t.setPricePerNight(new BigDecimal(price));
        t.setCapacity(capacity);
        return em.persistAndFlush(t);
    }

    private Room persistRoom(String number, RoomType type, RoomStatus status) {
        Room r = new Room();
        r.setRoomNumber(number);
        r.setFloorNumber(1);
        r.setRoomType(type);
        r.setStatus(status);
        return em.persistAndFlush(r);
    }

    private Reservation reserve(Room room, LocalDate in, LocalDate out, ReservationStatus status) {
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

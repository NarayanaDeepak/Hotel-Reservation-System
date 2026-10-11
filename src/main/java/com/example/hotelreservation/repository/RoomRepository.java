package com.example.hotelreservation.repository;

import com.example.hotelreservation.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    boolean existsByRoomNumberIgnoreCase(String roomNumber);

    boolean existsByRoomNumberIgnoreCaseAndIdNot(String roomNumber, Long id);

    /** Pessimistic row lock: serialises concurrent bookings of the same room. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = "roomType")
    Page<Room> findAll(Specification<Room> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "roomType")
    Optional<Room> findById(Long id);

    @EntityGraph(attributePaths = "roomType")
    @Query("""
            select r from Room r
            where r.status = com.example.hotelreservation.enums.RoomStatus.AVAILABLE
              and r.roomType.capacity >= :guests
              and (:roomTypeId is null or r.roomType.id = :roomTypeId)
              and not exists (
                  select 1 from Reservation res
                  where res.room = r
                    and res.status = com.example.hotelreservation.enums.ReservationStatus.CONFIRMED
                    and res.checkInDate < :checkOut
                    and res.checkOutDate > :checkIn)
            """)
    Page<Room> findAvailableRooms(@Param("checkIn") LocalDate checkIn,
                                  @Param("checkOut") LocalDate checkOut,
                                  @Param("guests") int guests,
                                  @Param("roomTypeId") Long roomTypeId,
                                  Pageable pageable);
}

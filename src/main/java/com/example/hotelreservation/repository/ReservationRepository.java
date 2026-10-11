package com.example.hotelreservation.repository;

import com.example.hotelreservation.entity.Reservation;
import com.example.hotelreservation.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** Customer history (derived query). */
    @EntityGraph(attributePaths = {"customer", "room", "room.roomType"})
    Page<Reservation> findByCustomerId(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "room", "room.roomType"})
    Page<Reservation> findByStatus(ReservationStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"customer", "room", "room.roomType"})
    Page<Reservation> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"customer", "room", "room.roomType"})
    Optional<Reservation> findById(Long id);

    boolean existsByIdAndCustomerUserUsername(Long id, String username);

    boolean existsByRoomId(Long roomId);

    @Query("""
            select count(r) > 0 from Reservation r
            where r.room.id = :roomId
              and r.status = com.example.hotelreservation.enums.ReservationStatus.CONFIRMED
              and r.checkInDate < :checkOut
              and r.checkOutDate > :checkIn
              and (:excludeId is null or r.id <> :excludeId)
            """)
    boolean existsOverlap(@Param("roomId") Long roomId,
                          @Param("checkIn") LocalDate checkIn,
                          @Param("checkOut") LocalDate checkOut,
                          @Param("excludeId") Long excludeId);

    /** Bulk update used by the scheduler: stays whose check-out day is before today are over.
     *  JPQL bulk updates bypass Hibernate's version handling, so the version is incremented explicitly. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Reservation r
            set r.status = com.example.hotelreservation.enums.ReservationStatus.COMPLETED,
                r.updatedAt = :now,
                r.updatedBy = 'scheduler',
                r.version = r.version + 1
            where r.status = com.example.hotelreservation.enums.ReservationStatus.CONFIRMED
              and r.checkOutDate < :today
            """)
    int markCompleted(@Param("today") LocalDate today, @Param("now") LocalDateTime now);

    /** Native query example: report of reservations per status. */
    @Query(value = "select status as status, count(*) as total from reservations group by status",
            nativeQuery = true)
    List<StatusCount> countByStatus();

    interface StatusCount {
        String getStatus();

        Long getTotal();
    }
}

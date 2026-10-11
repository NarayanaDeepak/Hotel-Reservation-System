package com.example.hotelreservation.specification;

import com.example.hotelreservation.entity.Room;
import com.example.hotelreservation.entity.RoomType;
import com.example.hotelreservation.enums.RoomStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


public final class RoomSpecification {

    private RoomSpecification() {
    }

    public static Specification<Room> filter(Long roomTypeId, BigDecimal minPrice, BigDecimal maxPrice,
                                             Integer minCapacity, RoomStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Room, RoomType> type = root.join("roomType");
            if (roomTypeId != null) {
                predicates.add(cb.equal(type.get("id"), roomTypeId));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(type.get("pricePerNight"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(type.get("pricePerNight"), maxPrice));
            }
            if (minCapacity != null) {
                predicates.add(cb.greaterThanOrEqualTo(type.get("capacity"), minCapacity));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

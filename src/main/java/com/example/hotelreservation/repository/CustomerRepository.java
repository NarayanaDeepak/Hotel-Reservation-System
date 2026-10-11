package com.example.hotelreservation.repository;

import com.example.hotelreservation.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Optional<Customer> findByUserUsername(String username);

    /** Used by the ownership check in method security. */
    boolean existsByIdAndUserUsername(Long id, String username);
}

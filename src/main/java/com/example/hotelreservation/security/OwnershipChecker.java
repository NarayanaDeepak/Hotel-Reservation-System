package com.example.hotelreservation.security;

import com.example.hotelreservation.repository.CustomerRepository;
import com.example.hotelreservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;


@Component("ownership")
@RequiredArgsConstructor
public class OwnershipChecker {

    private final CustomerRepository customerRepository;
    private final ReservationRepository reservationRepository;

    public boolean isCustomer(Long customerId, Authentication authentication) {
        return customerId != null && authentication != null
                && customerRepository.existsByIdAndUserUsername(customerId, authentication.getName());
    }

    public boolean isReservationOwner(Long reservationId, Authentication authentication) {
        return reservationId != null && authentication != null
                && reservationRepository.existsByIdAndCustomerUserUsername(reservationId, authentication.getName());
    }
}

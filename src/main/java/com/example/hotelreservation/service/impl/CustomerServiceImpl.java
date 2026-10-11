package com.example.hotelreservation.service.impl;

import com.example.hotelreservation.dto.CustomerRequestDto;
import com.example.hotelreservation.dto.CustomerResponseDto;
import com.example.hotelreservation.dto.RegisterRequestDto;
import com.example.hotelreservation.entity.AppUser;
import com.example.hotelreservation.entity.Customer;
import com.example.hotelreservation.enums.Role;
import com.example.hotelreservation.exception.DuplicateResourceException;
import com.example.hotelreservation.exception.ResourceNotFoundException;
import com.example.hotelreservation.mapper.HotelMapper;
import com.example.hotelreservation.repository.AppUserRepository;
import com.example.hotelreservation.repository.CustomerRepository;
import com.example.hotelreservation.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final HotelMapper mapper;

    @Override
    @Transactional
    public CustomerResponseDto register(RegisterRequestDto request) {
        if (appUserRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already taken: " + request.username());
        }
        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }
        AppUser user = new AppUser();
        user.setUsername(request.username().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.GUEST);
        appUserRepository.save(user);

        Customer customer = new Customer();
        customer.setFullName(request.fullName().trim());
        customer.setEmail(request.email().trim().toLowerCase());
        customer.setPhone(request.phone());
        customer.setUser(user);
        log.info("Registered guest '{}'", user.getUsername());
        return mapper.toResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponseDto> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto findById(Long id) {
        return mapper.toResponse(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto findByUsername(String username) {
        return customerRepository.findByUserUsername(username).map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Customer for user", username));
    }

    @Override
    @Transactional
    public CustomerResponseDto update(Long id, CustomerRequestDto request) {
        Customer customer = getEntity(id);
        if (customerRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }
        customer.setFullName(request.fullName().trim());
        customer.setEmail(request.email().trim().toLowerCase());
        customer.setPhone(request.phone());
        return mapper.toResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Customer customer = getEntity(id);
        customerRepository.delete(customer);
        customerRepository.flush(); // customers with reservations cannot be deleted -> 409
        log.info("Deleted customer {}", id);
    }

    private Customer getEntity(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }
}

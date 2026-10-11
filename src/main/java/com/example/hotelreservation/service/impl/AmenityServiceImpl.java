package com.example.hotelreservation.service.impl;

import com.example.hotelreservation.dto.AmenityRequestDto;
import com.example.hotelreservation.dto.AmenityResponseDto;
import com.example.hotelreservation.entity.Amenity;
import com.example.hotelreservation.exception.DuplicateResourceException;
import com.example.hotelreservation.exception.ResourceNotFoundException;
import com.example.hotelreservation.mapper.HotelMapper;
import com.example.hotelreservation.repository.AmenityRepository;
import com.example.hotelreservation.service.AmenityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmenityServiceImpl implements AmenityService {

    private final AmenityRepository amenityRepository;
    private final HotelMapper mapper;

    @Override
    @Transactional
    @CacheEvict(value = "amenities", allEntries = true)
    public AmenityResponseDto create(AmenityRequestDto request) {
        if (amenityRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Amenity already exists: " + request.name());
        }
        Amenity amenity = new Amenity();
        amenity.setName(request.name().trim());
        amenity.setDescription(request.description());
        log.info("Creating amenity '{}'", amenity.getName());
        return mapper.toResponse(amenityRepository.save(amenity));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("amenities")
    public List<AmenityResponseDto> findAll() {
        log.debug("Loading amenities from database (cache miss)");
        return amenityRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "amenities", key = "#id")
    public AmenityResponseDto findById(Long id) {
        return mapper.toResponse(getEntity(id));
    }

    /** Room types embed amenities, so both caches are evicted. */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "amenities", allEntries = true),
            @CacheEvict(value = "roomTypes", allEntries = true)})
    public AmenityResponseDto update(Long id, AmenityRequestDto request) {
        Amenity amenity = getEntity(id);
        if (amenityRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new DuplicateResourceException("Amenity already exists: " + request.name());
        }
        amenity.setName(request.name().trim());
        amenity.setDescription(request.description());
        return mapper.toResponse(amenityRepository.save(amenity));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "amenities", allEntries = true),
            @CacheEvict(value = "roomTypes", allEntries = true)})
    public void delete(Long id) {
        Amenity amenity = getEntity(id);
        amenityRepository.delete(amenity);
        amenityRepository.flush(); // surface FK violations here so the handler returns 409
        log.info("Deleted amenity {}", id);
    }

    private Amenity getEntity(Long id) {
        return amenityRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Amenity", id));
    }
}

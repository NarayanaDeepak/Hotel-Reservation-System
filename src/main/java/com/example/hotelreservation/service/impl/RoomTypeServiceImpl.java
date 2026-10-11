package com.example.hotelreservation.service.impl;

import com.example.hotelreservation.dto.RoomTypeRequestDto;
import com.example.hotelreservation.dto.RoomTypeResponseDto;
import com.example.hotelreservation.entity.Amenity;
import com.example.hotelreservation.entity.RoomType;
import com.example.hotelreservation.exception.DuplicateResourceException;
import com.example.hotelreservation.exception.ResourceNotFoundException;
import com.example.hotelreservation.mapper.HotelMapper;
import com.example.hotelreservation.repository.AmenityRepository;
import com.example.hotelreservation.repository.RoomTypeRepository;
import com.example.hotelreservation.service.RoomTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final AmenityRepository amenityRepository;
    private final HotelMapper mapper;

    @Override
    @Transactional
    @CacheEvict(value = "roomTypes", allEntries = true)
    public RoomTypeResponseDto create(RoomTypeRequestDto request) {
        if (roomTypeRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Room type already exists: " + request.name());
        }
        RoomType type = new RoomType();
        apply(type, request);
        log.info("Creating room type '{}'", type.getName());
        return mapper.toResponse(roomTypeRepository.save(type));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("roomTypes")
    public List<RoomTypeResponseDto> findAll() {
        log.debug("Loading room types from database (cache miss)");
        return roomTypeRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "roomTypes", key = "#id")
    public RoomTypeResponseDto findById(Long id) {
        return mapper.toResponse(getEntity(id));
    }

    @Override
    @Transactional
    @CacheEvict(value = "roomTypes", allEntries = true)
    public RoomTypeResponseDto update(Long id, RoomTypeRequestDto request) {
        RoomType type = getEntity(id);
        if (roomTypeRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new DuplicateResourceException("Room type already exists: " + request.name());
        }
        apply(type, request);
        return mapper.toResponse(roomTypeRepository.save(type));
    }

    @Override
    @Transactional
    @CacheEvict(value = "roomTypes", allEntries = true)
    public void delete(Long id) {
        RoomType type = getEntity(id);
        roomTypeRepository.delete(type);
        roomTypeRepository.flush(); // FK violation (rooms still use this type) -> 409
        log.info("Deleted room type {}", id);
    }

    private void apply(RoomType type, RoomTypeRequestDto request) {
        type.setName(request.name().trim());
        type.setDescription(request.description());
        type.setPricePerNight(request.pricePerNight());
        type.setCapacity(request.capacity());
        Set<Amenity> amenities = new HashSet<>();
        if (request.amenityIds() != null && !request.amenityIds().isEmpty()) {
            List<Amenity> found = amenityRepository.findAllById(request.amenityIds());
            if (found.size() != request.amenityIds().size()) {
                throw new ResourceNotFoundException("Amenity", request.amenityIds());
            }
            amenities.addAll(found);
        }
        type.setAmenities(amenities);
    }

    private RoomType getEntity(Long id) {
        return roomTypeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("RoomType", id));
    }
}

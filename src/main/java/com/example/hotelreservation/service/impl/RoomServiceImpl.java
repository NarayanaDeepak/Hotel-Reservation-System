package com.example.hotelreservation.service.impl;

import com.example.hotelreservation.dto.RoomRequestDto;
import com.example.hotelreservation.dto.RoomResponseDto;
import com.example.hotelreservation.entity.Room;
import com.example.hotelreservation.entity.RoomType;
import com.example.hotelreservation.enums.RoomStatus;
import com.example.hotelreservation.exception.BadRequestException;
import com.example.hotelreservation.exception.DuplicateResourceException;
import com.example.hotelreservation.exception.ResourceNotFoundException;
import com.example.hotelreservation.mapper.HotelMapper;
import com.example.hotelreservation.repository.RoomRepository;
import com.example.hotelreservation.repository.RoomTypeRepository;
import com.example.hotelreservation.service.RoomService;
import com.example.hotelreservation.specification.RoomSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final HotelMapper mapper;

    @Override
    @Transactional
    public RoomResponseDto create(RoomRequestDto request) {
        if (roomRepository.existsByRoomNumberIgnoreCase(request.roomNumber())) {
            throw new DuplicateResourceException("Room number already exists: " + request.roomNumber());
        }
        Room room = new Room();
        apply(room, request);
        log.info("Creating room {}", room.getRoomNumber());
        return mapper.toResponse(roomRepository.save(room));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoomResponseDto> search(Long roomTypeId, BigDecimal minPrice, BigDecimal maxPrice,
                                        Integer minCapacity, RoomStatus status, Pageable pageable) {
        return roomRepository.findAll(RoomSpecification.filter(roomTypeId, minPrice, maxPrice, minCapacity, status),
                pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoomResponseDto> findAvailable(LocalDate checkIn, LocalDate checkOut, int guests,
                                               Long roomTypeId, Pageable pageable) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new BadRequestException("checkOut must be after checkIn");
        }
        return roomRepository.findAvailableRooms(checkIn, checkOut, guests, roomTypeId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponseDto findById(Long id) {
        return mapper.toResponse(getEntity(id));
    }

    @Override
    @Transactional
    public RoomResponseDto update(Long id, RoomRequestDto request) {
        Room room = getEntity(id);
        if (roomRepository.existsByRoomNumberIgnoreCaseAndIdNot(request.roomNumber(), id)) {
            throw new DuplicateResourceException("Room number already exists: " + request.roomNumber());
        }
        apply(room, request);
        return mapper.toResponse(roomRepository.save(room));
    }

    @Override
    @Transactional
    public RoomResponseDto updateStatus(Long id, RoomStatus status) {
        Room room = getEntity(id);
        room.setStatus(status);
        log.info("Room {} status -> {}", room.getRoomNumber(), status);
        return mapper.toResponse(roomRepository.save(room));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Room room = getEntity(id);
        roomRepository.delete(room);
        roomRepository.flush(); // rooms with reservations cannot be deleted -> 409
        log.info("Deleted room {}", id);
    }

    private void apply(Room room, RoomRequestDto request) {
        RoomType type = roomTypeRepository.findById(request.roomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("RoomType", request.roomTypeId()));
        room.setRoomNumber(request.roomNumber().trim());
        room.setFloorNumber(request.floorNumber());
        room.setRoomType(type);
        if (request.status() != null) {
            room.setStatus(request.status());
        }
    }

    private Room getEntity(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Room", id));
    }
}

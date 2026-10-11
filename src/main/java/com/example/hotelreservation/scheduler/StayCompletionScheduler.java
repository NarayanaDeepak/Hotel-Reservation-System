package com.example.hotelreservation.scheduler;

import com.example.hotelreservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Component
@RequiredArgsConstructor
public class StayCompletionScheduler {

    private final ReservationRepository reservationRepository;

    @Scheduled(fixedRateString = "${hotel.scheduler.complete-stays-rate-ms}")
    @Transactional
    public void completeFinishedStays() {
        reservationRepository.markCompleted(LocalDate.now(), LocalDateTime.now());
    }
}

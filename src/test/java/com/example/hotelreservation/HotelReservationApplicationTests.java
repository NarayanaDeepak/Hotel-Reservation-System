package com.example.hotelreservation;

import com.example.hotelreservation.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class HotelReservationApplicationTests {

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void contextLoads_andDemoUsersAreSeeded() {
        assertThat(appUserRepository.findByUsername("admin")).isPresent();
        assertThat(appUserRepository.findByUsername("guest")).isPresent();
    }
}

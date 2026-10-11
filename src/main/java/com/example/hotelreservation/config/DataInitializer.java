package com.example.hotelreservation.config;

import com.example.hotelreservation.entity.*;
import com.example.hotelreservation.enums.Role;
import com.example.hotelreservation.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;


@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "hotel.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final CustomerRepository customerRepository;
    private final AmenityRepository amenityRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${hotel.seed.admin-username:admin}")
    private String adminUsername;
    @Value("${hotel.seed.admin-password:admin123}")
    private String adminPassword;
    @Value("${hotel.seed.guest-username:guest}")
    private String guestUsername;
    @Value("${hotel.seed.guest-password:guest123}")
    private String guestPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (appUserRepository.count() > 0) {
            return;
        }
        createUser(adminUsername, adminPassword, Role.ADMIN);
        AppUser guest = createUser(guestUsername, guestPassword, Role.GUEST);

        Customer customer = new Customer();
        customer.setFullName("Demo Guest");
        customer.setEmail("guest@example.com");
        customer.setPhone("9999999999");
        customer.setUser(guest);
        customerRepository.save(customer);

        Amenity wifi = amenity("WiFi", "High-speed wireless internet");
        Amenity ac = amenity("Air Conditioning", "Individually controlled AC");
        Amenity minibar = amenity("Mini Bar", "Stocked mini bar");
        Amenity breakfast = amenity("Breakfast", "Complimentary breakfast");
        Amenity seaView = amenity("Sea View", "Balcony with sea view");

        RoomType standard = roomType("Standard", "Comfortable room for two", "2500.00", 2, Set.of(wifi, ac));
        RoomType deluxe = roomType("Deluxe", "Spacious room with extras", "4200.00", 3, Set.of(wifi, ac, minibar, breakfast));
        RoomType suite = roomType("Suite", "Suite with living area and view", "8000.00", 4,
                Set.of(wifi, ac, minibar, breakfast, seaView));

        room("101", 1, standard);
        room("102", 1, standard);
        room("103", 1, standard);
        room("201", 2, deluxe);
        room("202", 2, deluxe);
        room("301", 3, suite);
        log.info("Seeded demo data. Users: {}/{} (ADMIN) and {}/{} (GUEST) - change these before real use.",
                adminUsername, "****", guestUsername, "****");
    }

    private AppUser createUser(String username, String rawPassword, Role role) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        return appUserRepository.save(user);
    }

    private Amenity amenity(String name, String description) {
        Amenity a = new Amenity();
        a.setName(name);
        a.setDescription(description);
        return amenityRepository.save(a);
    }

    private RoomType roomType(String name, String description, String price, int capacity, Set<Amenity> amenities) {
        RoomType t = new RoomType();
        t.setName(name);
        t.setDescription(description);
        t.setPricePerNight(new BigDecimal(price));
        t.setCapacity(capacity);
        t.setAmenities(new HashSet<>(amenities));
        return roomTypeRepository.save(t);
    }

    private void room(String number, int floor, RoomType type) {
        Room r = new Room();
        r.setRoomNumber(number);
        r.setFloorNumber(floor);
        r.setRoomType(type);
        roomRepository.save(r);
    }
}

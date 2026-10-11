package com.example.hotelreservation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.hotelreservation.dto.ApiErrorResponse;
import com.example.hotelreservation.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * HTTP-level tests of every controller: the status code and body of each ResponseEntity,
 * the security rules (401 / 403 / 200 per role) and the full reservation lifecycle.
 * Runs the whole application on in-memory H2 with the seeded admin / guest users.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResponseEntityTest {

    // ================= shared helpers =================

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    static final String PASSWORD = "Passw0rd!";

    RequestPostProcessor admin() {
        return httpBasic("admin", "admin123");
    }

    RequestPostProcessor user(String username) {
        return httpBasic(username, PASSWORD);
    }

    String unique() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    Map<String, Object> map(Object... keyValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put((String) keyValues[i], keyValues[i + 1]);
        }
        return m;
    }

    /** POST JSON, assert the status and return the parsed response body. */
    JsonNode postJson(String url, Object body, RequestPostProcessor auth, int expectedStatus) throws Exception {
        return read(mockMvc.perform(MockMvcRequestBuilders.post(url).with(auth)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString());
    }

    JsonNode getJson(String url, RequestPostProcessor auth, int expectedStatus) throws Exception {
        return read(mockMvc.perform(MockMvcRequestBuilders.get(url).with(auth))
                .andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString());
    }

    JsonNode patchJson(String url, RequestPostProcessor auth, int expectedStatus) throws Exception {
        return read(mockMvc.perform(MockMvcRequestBuilders.patch(url).with(auth))
                .andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString());
    }

    private JsonNode read(String content) throws Exception {
        return content == null || content.isBlank() ? objectMapper.nullNode() : objectMapper.readTree(content);
    }

    /** Registers a guest and returns the created customer's id. */
    long registerGuest(String username) throws Exception {
        return postJson("/api/auth/register", map("username", username, "password", PASSWORD,
                "fullName", "Guest " + username, "email", username + "@example.com", "phone", "9876543210"),
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous(),
                201).get("id").asLong();
    }

    /** Admin creates a room type (price 1000, capacity 2) plus one room; returns {typeId, roomId}. */
    long[] createRoom() throws Exception {
        String s = unique();
        long typeId = postJson("/api/room-types", map("name", "Type-" + s, "description", "it", "pricePerNight", 1000,
                "capacity", 2, "amenityIds", java.util.List.of()), admin(), 201).get("id").asLong();
        long roomId = postJson("/api/rooms", map("roomNumber", "R" + s, "floorNumber", 5, "roomTypeId", typeId),
                admin(), 201).get("id").asLong();
        return new long[]{typeId, roomId};
    }

    // ================= security rules =================

    private String roomTypeJson() throws Exception {
        return objectMapper.writeValueAsString(map("name", "Sec-" + unique(), "pricePerNight", 1200,
                "capacity", 2, "amenityIds", List.of()));
    }

    @Test
    void publicCatalogue_isReadableWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/room-types")).andExpect(status().isOk());
        mockMvc.perform(get("/api/amenities")).andExpect(status().isOk());
        mockMvc.perform(get("/api/rooms")).andExpect(status().isOk());
        mockMvc.perform(get("/api/rooms/availability").param("checkIn", "2099-01-10").param("checkOut", "2099-01-12"))
                .andExpect(status().isOk());
    }

    @Test
    void writingCatalogue_withoutLogin_returns401WithJsonBody() throws Exception {
        mockMvc.perform(post("/api/room-types").contentType(MediaType.APPLICATION_JSON).content(roomTypeJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/room-types"));
    }

    @Test
    void writingCatalogue_asGuest_returns403() throws Exception {
        mockMvc.perform(post("/api/room-types").with(httpBasic("guest", "guest123"))
                        .contentType(MediaType.APPLICATION_JSON).content(roomTypeJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void writingCatalogue_asAdmin_returns201() throws Exception {
        mockMvc.perform(post("/api/room-types").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(roomTypeJson()))
                .andExpect(status().isCreated());
    }

    @Test
    void wrongPassword_returns401() throws Exception {
        mockMvc.perform(get("/api/customers/me").with(httpBasic("admin", "not-the-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerList_isAdminOnly() throws Exception {
        mockMvc.perform(get("/api/customers")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/customers").with(httpBasic("guest", "guest123"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/customers").with(admin())).andExpect(status().isOk());
    }

    @Test
    void actuator_healthIsPublic_restIsAdminOnly() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics").with(httpBasic("guest", "guest123"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/metrics").with(admin())).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/caches").with(admin()))
                .andExpect(status().isOk());
    }

    @Test
    void register_thenLoginAndReadOwnProfile() throws Exception {
        String username = "reg" + unique();
        registerGuest(username);

        mockMvc.perform(get("/api/customers/me").with(user(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(username + "@example.com"));
    }

    @Test
    void register_withWeakPasswordAndBadEmail_returns400WithFieldDetails() throws Exception {
        String body = objectMapper.writeValueAsString(map("username", "abcd", "password", "short",
                "fullName", "X", "email", "not-an-email"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details", hasItem("password: password must be 8-72 characters")))
                .andExpect(jsonPath("$.details", hasItem("email: email is invalid")));
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        String username = "dup" + unique();
        registerGuest(username);

        String body = objectMapper.writeValueAsString(map("username", username, "password", PASSWORD,
                "fullName", "Again", "email", "other-" + username + "@example.com"));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    // ================= reservation lifecycle & error bodies =================

    private final LocalDate in = LocalDate.now().plusDays(5);
    private final LocalDate out = LocalDate.now().plusDays(8);

    private Object reservationBody(long customerId, long roomId, LocalDate checkIn, LocalDate checkOut, int guests) {
        return map("customerId", customerId, "roomId", roomId, "checkInDate", checkIn.toString(),
                "checkOutDate", checkOut.toString(), "guests", guests);
    }

    @Test
    void fullLifecycle_book_conflict_pay_history_cancel_rebook() throws Exception {
        long[] ids = createRoom();
        long typeId = ids[0];
        long roomId = ids[1];
        String guestName = "g" + unique();
        long customerId = registerGuest(guestName);

        // the room is listed as available for the dates
        JsonNode before = getJson("/api/rooms/availability?checkIn=" + in + "&checkOut=" + out + "&roomTypeId=" + typeId,
                user(guestName), 200);
        assertThat(before.get("totalElements").asLong()).isEqualTo(1);

        // guest books it: 3 nights x 1000
        JsonNode created = postJson("/api/reservations", reservationBody(customerId, roomId, in, out, 2), user(guestName), 201);
        long reservationId = created.get("id").asLong();
        assertThat(created.get("totalAmount").decimalValue()).isEqualByComparingTo("3000");
        assertThat(created.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(created.get("nights").asLong()).isEqualTo(3);

        // same room, overlapping dates -> 409 with the standard error body
        JsonNode conflict = postJson("/api/reservations",
                reservationBody(customerId, roomId, in.plusDays(1), out.plusDays(1), 1), user(guestName), 409);
        assertThat(conflict.get("message").asText()).contains("already booked");

        // no longer listed as available, but a later window still is
        JsonNode during = getJson("/api/rooms/availability?checkIn=" + in + "&checkOut=" + out + "&roomTypeId=" + typeId,
                user(guestName), 200);
        assertThat(during.get("totalElements").asLong()).isZero();
        JsonNode later = getJson("/api/rooms/availability?checkIn=" + out + "&checkOut=" + out.plusDays(2) + "&roomTypeId=" + typeId,
                user(guestName), 200);
        assertThat(later.get("totalElements").asLong()).isEqualTo(1);

        // payments: partial -> over-payment rejected -> remainder
        postJson("/api/reservations/" + reservationId + "/payments", map("amount", 1000, "method", "UPI"), user(guestName), 201);
        assertThat(getJson("/api/reservations/" + reservationId, user(guestName), 200).get("paymentStatus").asText())
                .isEqualTo("PARTIALLY_PAID");
        postJson("/api/reservations/" + reservationId + "/payments", map("amount", 3000, "method", "CARD"), user(guestName), 400);
        postJson("/api/reservations/" + reservationId + "/payments", map("amount", 2000, "method", "CARD"), user(guestName), 201);
        JsonNode paid = getJson("/api/reservations/" + reservationId, user(guestName), 200);
        assertThat(paid.get("paymentStatus").asText()).isEqualTo("PAID");
        assertThat(getJson("/api/reservations/" + reservationId + "/payments", user(guestName), 200)).hasSize(2);

        // customer history
        JsonNode history = getJson("/api/customers/" + customerId + "/reservations", user(guestName), 200);
        assertThat(history.get("totalElements").asLong()).isEqualTo(1);

        // cancel: reservation cancelled, money refunded, second cancel -> 409
        JsonNode cancelled = patchJson("/api/reservations/" + reservationId + "/cancel", user(guestName), 200);
        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(cancelled.get("paymentStatus").asText()).isEqualTo("REFUNDED");
        assertThat(getJson("/api/reservations/" + reservationId + "/payments", user(guestName), 200).get(0).get("status").asText())
                .isEqualTo("REFUNDED");
        patchJson("/api/reservations/" + reservationId + "/cancel", user(guestName), 409);

        // the dates are free again
        postJson("/api/reservations", reservationBody(customerId, roomId, in, out, 2), user(guestName), 201);

        // admin report (native query)
        JsonNode report = getJson("/api/reservations/report", admin(), 200);
        assertThat(report.has("CONFIRMED")).isTrue();
        assertThat(report.get("CANCELLED").asLong()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void guests_cannotTouchEachOthersData() throws Exception {
        long roomId = createRoom()[1];
        String owner = "o" + unique();
        String intruder = "i" + unique();
        long ownerId = registerGuest(owner);
        long intruderId = registerGuest(intruder);
        long reservationId = postJson("/api/reservations", reservationBody(ownerId, roomId, in, out, 1), user(owner), 201)
                .get("id").asLong();

        getJson("/api/reservations/" + reservationId, user(intruder), 403);
        patchJson("/api/reservations/" + reservationId + "/cancel", user(intruder), 403);
        postJson("/api/reservations/" + reservationId + "/payments", map("amount", 10, "method", "CASH"), user(intruder), 403);
        getJson("/api/customers/" + ownerId, user(intruder), 403);
        getJson("/api/customers/" + ownerId + "/reservations", user(intruder), 403);
        // booking in somebody else's name
        postJson("/api/reservations", reservationBody(ownerId, roomId, out.plusDays(3), out.plusDays(4), 1), user(intruder), 403);

        // the owner and an admin still can
        getJson("/api/reservations/" + reservationId, user(owner), 200);
        getJson("/api/reservations/" + reservationId, admin(), 200);
        getJson("/api/reservations", user(intruder), 403);
        assertThat(intruderId).isNotEqualTo(ownerId);
    }

    @Test
    void invalidReservationRequests_returnConsistentErrorBodies() throws Exception {
        long roomId = createRoom()[1];
        String guest = "v" + unique();
        long customerId = registerGuest(guest);

        // check-out before check-in (business rule) -> 400
        JsonNode badDates = postJson("/api/reservations", reservationBody(customerId, roomId, out, in, 1), user(guest), 400);
        assertThat(badDates.get("message").asText()).contains("checkOutDate must be after checkInDate");
        assertThat(badDates.get("path").asText()).isEqualTo("/api/reservations");

        // too many guests -> 400
        postJson("/api/reservations", reservationBody(customerId, roomId, in, out, 5), user(guest), 400);

        // bean validation: guests = 0 and missing roomId -> 400 with field details
        mockMvc.perform(MockMvcRequestBuilders.post("/api/reservations").with(user(guest)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(map("customerId", customerId,
                                "checkInDate", in.toString(), "checkOutDate", out.toString(), "guests", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem("roomId: roomId is required")))
                .andExpect(jsonPath("$.details", hasItem("guests: guests must be at least 1")));

        // malformed date -> 400, unknown room -> 404
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/availability").param("checkIn", "tomorrow").param("checkOut", "2099-01-01"))
                .andExpect(status().isBadRequest());
        postJson("/api/reservations", reservationBody(customerId, 999_999L, in, out, 1), user(guest), 404);
        getJson("/api/reservations/999999", admin(), 404);
    }

    @Test
    void maintenanceRoom_cannotBeBooked_andIsHiddenFromAvailability() throws Exception {
        long[] ids = createRoom();
        String guest = "m" + unique();
        long customerId = registerGuest(guest);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/rooms/" + ids[1] + "/status").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"MAINTENANCE\"}"))
                .andExpect(status().isOk());

        postJson("/api/reservations", reservationBody(customerId, ids[1], in, out, 1), user(guest), 409);
        JsonNode available = getJson("/api/rooms/availability?checkIn=" + in + "&checkOut=" + out + "&roomTypeId=" + ids[0],
                user(guest), 200);
        assertThat(available.get("totalElements").asLong()).isZero();
    }

    @Test
    void roomTypeCache_isEvictedOnWrite() throws Exception {
        String name = "Cache-" + unique();
        int before = getJson("/api/room-types", admin(), 200).size();
        getJson("/api/room-types", admin(), 200); // served from cache

        postJson("/api/room-types", map("name", name, "pricePerNight", 900, "capacity", 1,
                "amenityIds", java.util.List.of()), admin(), 201);

        assertThat(getJson("/api/room-types", admin(), 200)).hasSize(before + 1);
    }

    @Test
    void deletingRoomTypeInUse_returns409() throws Exception {
        long[] ids = createRoom();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/room-types/" + ids[0]).with(admin()))
                .andExpect(status().isConflict());
    }

    // ================= concurrency (@Version) -> 409 =================

    @Test
    void optimisticLockFailure_isReturnedAsConflictResponseEntity() {
        ResponseEntity<ApiErrorResponse> response = new GlobalExceptionHandler().optimisticLock(
                new ObjectOptimisticLockingFailureException("Reservation", 1L),
                new MockHttpServletRequest("PATCH", "/api/reservations/1/cancel"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("modified by another request");
        assertThat(response.getBody().path()).isEqualTo("/api/reservations/1/cancel");
    }
}

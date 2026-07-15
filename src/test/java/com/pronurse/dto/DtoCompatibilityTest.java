package com.pronurse.dto;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;
import com.pronurse.common.payload.ApiResponse;
import com.pronurse.nurse.dto.NurseSearchItem;
import com.pronurse.nurse.dto.NurseSearchResponse;
import com.pronurse.booking.dto.CreateBookingRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DtoCompatibilityTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testNurseSearchResponseSerialization() throws Exception {
        NurseSearchItem item = NurseSearchItem.builder()
                .id(12L)
                .name("Anita Sharma")
                .profile_image("https://example.com/uploads/nurses/anita.jpg")
                .gender("Female")
                .experience(8)
                .qualification("B.Sc Nursing")
                .specialization("ICU Care")
                .languages(List.of("Hindi", "English"))
                .city("Lucknow")
                .rating(4.8)
                .total_reviews(156)
                .consultation_fee(800.0)
                .availability(true)
                .next_available("2026-07-20 10:00 AM")
                .build();

        NurseSearchResponse response = NurseSearchResponse.builder()
                .status(true)
                .message("Nurses found successfully")
                .total(1)
                .page(1)
                .limit(10)
                .data(List.of(item))
                .build();

        String json = mapper.writeValueAsString(response);
        
        // Assert JSON values
        assertTrue(json.contains("\"status\":true"));
        assertTrue(json.contains("\"message\":\"Nurses found successfully\""));
        assertTrue(json.contains("\"total\":1"));
        assertTrue(json.contains("\"page\":1"));
        assertTrue(json.contains("\"limit\":10"));
        assertTrue(json.contains("\"name\":\"Anita Sharma\""));
        assertTrue(json.contains("\"profile_image\":\"https://example.com/uploads/nurses/anita.jpg\""));
        assertTrue(json.contains("\"languages\":[\"Hindi\",\"English\"]"));
        assertTrue(json.contains("\"experience\":8"));
        assertTrue(json.contains("\"rating\":4.8"));
        assertTrue(json.contains("\"consultation_fee\":800.0"));
    }

    @Test
    void testApiResponseSerialization() throws Exception {
        ApiResponse<String> apiResponse = new ApiResponse<>(true, "Success Message", "Some Data");
        String json = mapper.writeValueAsString(apiResponse);

        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"message\":\"Success Message\""));
        assertTrue(json.contains("\"data\":\"Some Data\""));
    }

    @Test
    void testCreateBookingRequestDeserialization() throws Exception {
        String json = "{\n" +
                "  \"selected_service_ids\": [1, 2],\n" +
                "  \"booking_date\": \"2026-07-20\",\n" +
                "  \"booking_time\": \"10:00 AM\",\n" +
                "  \"remarks\": \"Test remarks\",\n" +
                "  \"latitude\": \"28.6289\",\n" +
                "  \"longitude\": \"77.3649\",\n" +
                "  \"address\": \"Sector 62, Noida\",\n" +
                "  \"has_injection\": true\n" +
                "}";

        ObjectMapper customMapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .findAndAddModules()
                .build();

        CreateBookingRequest request = customMapper.readValue(json, CreateBookingRequest.class);

        assertNotNull(request);
        assertEquals(2, request.getSelectedServiceIds().size());
        assertEquals("2026-07-20", request.getBookingDate());
        assertEquals("10:00 AM", request.getBookingTime());
        assertEquals("Test remarks", request.getRemarks());
        assertEquals("28.6289", request.getLatitude());
        assertEquals("77.3649", request.getLongitude());
        assertEquals("Sector 62, Noida", request.getAddress());
        assertTrue(request.getHasInjection());
    }
}

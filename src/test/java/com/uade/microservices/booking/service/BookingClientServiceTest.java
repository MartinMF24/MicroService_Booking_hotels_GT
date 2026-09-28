package com.uade.microservices.booking.service;

import com.uade.microservices.booking.config.BookingRapidApiProperties;
import com.uade.microservices.booking.dto.rapidapi.BookingHotelRawDto;
import com.uade.microservices.booking.model.GranPremioTarget;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BookingClientServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private BookingRapidApiProperties properties;
    private ObjectMapper objectMapper;
    private BookingClientService clientService;

    @BeforeEach
    void setUp() {
        properties = new BookingRapidApiProperties();
        objectMapper = new ObjectMapper();
        clientService = new BookingClientService(restTemplate, properties, objectMapper);
    }

    @Test
    @DisplayName("Sin API Key configurada debe activar fallback representativo sin fallar")
    void shouldReturnSimulatedDataWhenApiKeyIsMissing() {
        properties.setKey("");

        List<BookingHotelRawDto> hotels = clientService.extractHotels(
                GranPremioTarget.SAO_PAULO,
                LocalDate.of(2026, 11, 4),
                LocalDate.of(2026, 11, 7)
        );

        assertNotNull(hotels);
        assertFalse(hotels.isEmpty());
        assertTrue(hotels.stream().anyMatch(h -> h.getHotelName().contains("São Paulo") || h.getHotelName().contains("Interlagos") || h.getHotelName().contains("Tangará")));
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Debe generar datos simulados específicos para cada uno de los 9 destinos de F1")
    void shouldGenerateSimulatedHotelsForAllTargets() {
        for (GranPremioTarget target : GranPremioTarget.values()) {
            List<BookingHotelRawDto> simulated = clientService.generateSimulatedHotels(target);
            assertNotNull(simulated);
            assertFalse(simulated.isEmpty());
            for (BookingHotelRawDto dto : simulated) {
                assertNotNull(dto.getHotelName());
                assertNotNull(dto.getStarRating());
                assertNotNull(dto.getMinTotalPrice());
            }
        }
    }
}


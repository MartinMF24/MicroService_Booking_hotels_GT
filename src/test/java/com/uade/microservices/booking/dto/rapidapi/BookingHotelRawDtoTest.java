package com.uade.microservices.booking.dto.rapidapi;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingHotelRawDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Debe deserializar correctamente la estructura anidada 'property' de RapidAPI y resolver el nombre real")
    void shouldDeserializeRapidApiNestedPropertyStructure() throws Exception {
        String json = """
        {
            "hotel_id": 91847,
            "accessibilityLabel": "Hotel Rey Arturo Burgos.\\n3 out of 5 stars.\\n8.2 Very good 2228 reviews.\\nPrice 321 USD.",
            "property": {
                "name": "Hotel Rey Arturo Burgos",
                "propertyClass": 3,
                "accuratePropertyClass": 3,
                "reviewScore": 8.2,
                "reviewCount": 2228,
                "priceBreakdown": {
                    "grossPrice": {
                        "value": 321.28,
                        "currency": "USD"
                    }
                },
                "photoUrls": [
                    "https://cf.bstatic.com/xdata/images/hotel/square500/683048303.jpg"
                ]
            }
        }
        """;

        BookingHotelRawDto dto = objectMapper.readValue(json, BookingHotelRawDto.class);

        assertNotNull(dto);
        assertEquals(91847L, dto.getHotelId());
        // El nombre debe ser el nombre real y NO "Hotel Destino 91847"
        assertEquals("Hotel Rey Arturo Burgos", dto.resolveNombre());
        assertEquals(3, dto.resolveEstrellas());
        assertEquals(BigDecimal.valueOf(321.28), dto.resolvePrecioBase());
        assertEquals("https://cf.bstatic.com/xdata/images/hotel/square500/683048303.jpg", dto.resolveImagenUrl());
    }

    @Test
    @DisplayName("Debe resolver el nombre desde accessibilityLabel cuando no hay property")
    void shouldResolveNameFromAccessibilityLabelWhenNoProperty() throws Exception {
        String json = """
        {
            "hotel_id": 12345,
            "accessibilityLabel": "Silken Gran Teatro.\\n4 out of 5 stars.\\n9.0 Superb.\\nPrice 450 USD."
        }
        """;

        BookingHotelRawDto dto = objectMapper.readValue(json, BookingHotelRawDto.class);

        assertNotNull(dto);
        assertEquals("Silken Gran Teatro", dto.resolveNombre());
    }

    @Test
    @DisplayName("Debe resolver el nombre desde formato plano tradicional si existe")
    void shouldResolveSimulatedName() {
        BookingHotelRawDto dto = new BookingHotelRawDto(100L, "Palácio Tangará", 5, 430.0, "https://pic.jpg");

        assertEquals("Palácio Tangará", dto.resolveNombre());
        assertEquals(5, dto.resolveEstrellas());
        assertEquals(BigDecimal.valueOf(430.0), dto.resolvePrecioBase());
        assertEquals("https://pic.jpg", dto.resolveImagenUrl());
    }
}

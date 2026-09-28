package com.uade.microservices.booking.adapter;

import com.uade.microservices.booking.dto.rapidapi.BookingHotelRawDto;
import com.uade.microservices.booking.model.GranPremioTarget;
import com.uade.microservices.booking.model.HabitacionHotel;
import com.uade.microservices.booking.model.Hotel;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingAdapterTest {

    private BookingAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new BookingAdapter();
    }

    @Test
    @DisplayName("Debe transformar un BookingHotelRawDto en Hotel y sus HabitacionHotel asociadas")
    void shouldTransformRawDtoToHotelEntity() {
        BookingHotelRawDto raw = new BookingHotelRawDto();
        raw.setHotelId(12345L);
        raw.setHotelName("Hotel Fasano São Paulo Jardins");
        raw.setStarRating(5);
        raw.setMinTotalPrice(350.0);
        raw.setMainPhotoUrl("https://example.com/fasano.jpg");
        raw.setHasFreeParking(true);

        Hotel hotel = adapter.toEntity(raw, GranPremioTarget.SAO_PAULO);

        assertNotNull(hotel);
        assertEquals("Hotel Fasano São Paulo Jardins", hotel.getNombre());
        assertEquals(GranPremioTarget.SAO_PAULO.getCiudadId(), hotel.getIdCiudad());
        assertEquals(5, hotel.getEstrellas());
        assertTrue(hotel.getOfreceTraslado());
        assertEquals("https://example.com/fasano.jpg", hotel.getImagenPrincipalUrl());
        assertNotNull(hotel.getDistanciaCircuitoKm());
        assertTrue(hotel.getDistanciaCircuitoKm().compareTo(BigDecimal.ZERO) > 0);

        List<HabitacionHotel> habitaciones = hotel.getHabitaciones();
        assertFalse(habitaciones.isEmpty());
        assertEquals(3, habitaciones.size());

        HabitacionHotel estandar = habitaciones.stream()
                .filter(h -> "Estándar".equalsIgnoreCase(h.getTipo()))
                .findFirst()
                .orElse(null);
        assertNotNull(estandar);
        assertEquals(BigDecimal.valueOf(350.0).setScale(2), estandar.getPrecioPorNocheUsd());
        assertTrue(estandar.getStockDisponible() > 0);
        assertEquals(hotel, estandar.getHotel());
    }

    @Test
    @DisplayName("Debe truncar nombres de hoteles excesivamente largos a 150 caracteres")
    void shouldTruncateExcessivelyLongHotelNames() {
        String veryLongName = "A".repeat(200);
        BookingHotelRawDto raw = new BookingHotelRawDto();
        raw.setHotelName(veryLongName);
        raw.setStarRating(3);
        raw.setMinTotalPrice(100.0);

        Hotel hotel = adapter.toEntity(raw, GranPremioTarget.MADRID);

        assertEquals(150, hotel.getNombre().length());
    }

    @Test
    @DisplayName("Debe transformar lista completa de DTOs")
    void shouldTransformList() {
        List<BookingHotelRawDto> rawList = List.of(
                new BookingHotelRawDto(1L, "Hotel 1", 4, 150.0, "https://pic1.jpg"),
                new BookingHotelRawDto(2L, "Hotel 2", 3, 90.0, "https://pic2.jpg")
        );

        List<Hotel> hotels = adapter.toEntityList(rawList, GranPremioTarget.BAKU);

        assertEquals(2, hotels.size());
        assertEquals("Hotel 1", hotels.get(0).getNombre());
        assertEquals("Hotel 2", hotels.get(1).getNombre());
    }
}


package com.uade.microservices.booking.service;

import com.uade.microservices.booking.adapter.BookingAdapter;
import com.uade.microservices.booking.dto.rapidapi.BookingHotelRawDto;
import com.uade.microservices.booking.dto.response.BookingSyncAllSummaryDto;
import com.uade.microservices.booking.dto.response.BookingSyncResultDto;
import com.uade.microservices.booking.model.Ciudad;
import com.uade.microservices.booking.model.GranPremioTarget;
import com.uade.microservices.booking.model.HabitacionHotel;
import com.uade.microservices.booking.model.Hotel;
import com.uade.microservices.booking.repository.CiudadRepository;
import com.uade.microservices.booking.repository.HotelRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingSyncServiceTest {

    @Mock
    private BookingClientService bookingClientService;

    @Mock
    private BookingAdapter bookingAdapter;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private CiudadRepository ciudadRepository;

    @InjectMocks
    private BookingSyncService bookingSyncService;

    private GranPremioTarget target;
    private BookingHotelRawDto rawDto;
    private Hotel transformedHotel;

    @BeforeEach
    void setUp() {
        target = GranPremioTarget.SAO_PAULO;
        rawDto = new BookingHotelRawDto(1001L, "Grand Mercure Interlagos", 4, 150.0, "https://pic.jpg");

        transformedHotel = new Hotel();
        transformedHotel.setIdCiudad(target.getCiudadId());
        transformedHotel.setNombre("Grand Mercure Interlagos");
        transformedHotel.setEstrellas(4);
        transformedHotel.setDistanciaCircuitoKm(BigDecimal.valueOf(5.20));
        transformedHotel.setOfreceTraslado(true);
        transformedHotel.setImagenPrincipalUrl("https://pic.jpg");

        HabitacionHotel hab = new HabitacionHotel();
        hab.setTipo("Single");
        hab.setPrecioPorNocheUsd(BigDecimal.valueOf(150.00));
        hab.setStockDisponible(10);
        transformedHotel.addHabitacion(hab);

        lenient().when(ciudadRepository.findFirstByNombreIgnoreCase(target.getNombreCiudad()))
                .thenReturn(Optional.of(new Ciudad(target.getCiudadId(), target.getNombreCiudad())));
    }

    @Test
    @DisplayName("UPSERT: Debe crear nuevo hotel si no existe en la base de datos")
    void shouldCreateNewHotelWhenItDoesNotExist() {
        when(bookingClientService.extractHotels(eq(target), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(rawDto));
        when(bookingAdapter.toEntityList(any(), eq(target), any(UUID.class)))
                .thenReturn(List.of(transformedHotel));
        when(hotelRepository.findWithHabitacionesByNombreIgnoreCaseAndIdCiudad(eq("Grand Mercure Interlagos"), eq(target.getCiudadId())))
                .thenReturn(Optional.empty());
        when(hotelRepository.save(any(Hotel.class)))
                .thenAnswer(inv -> {
                    Hotel h = inv.getArgument(0);
                    h.setIdHotel(UUID.randomUUID());
                    return h;
                });

        BookingSyncResultDto result = bookingSyncService.syncBookingData(target);

        assertNotNull(result);
        assertEquals(target, result.target());
        assertEquals(1, result.hotelesCreados());
        assertEquals(0, result.hotelesActualizados());
        assertEquals(1, result.habitacionesCreadas());
        assertEquals("SUCCESS", result.estado());
        verify(hotelRepository).save(transformedHotel);
    }

    @Test
    @DisplayName("UPSERT: Debe actualizar hotel y sus habitaciones si ya existe")
    void shouldUpdateExistingHotelAndRoomsWhenItAlreadyExists() {
        Hotel existingHotel = new Hotel();
        existingHotel.setIdHotel(UUID.randomUUID());
        existingHotel.setIdCiudad(target.getCiudadId());
        existingHotel.setNombre("Grand Mercure Interlagos");
        existingHotel.setEstrellas(3);
        existingHotel.setDistanciaCircuitoKm(BigDecimal.valueOf(5.00));
        existingHotel.setOfreceTraslado(false);
        existingHotel.setCreatedAt(OffsetDateTime.now().minusDays(10));

        HabitacionHotel existingHab = new HabitacionHotel();
        existingHab.setIdHabitacion(UUID.randomUUID());
        existingHab.setTipo("Single");
        existingHab.setPrecioPorNocheUsd(BigDecimal.valueOf(120.00));
        existingHab.setStockDisponible(2);
        existingHotel.addHabitacion(existingHab);

        when(bookingClientService.extractHotels(eq(target), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(rawDto));
        when(bookingAdapter.toEntityList(any(), eq(target), any(UUID.class)))
                .thenReturn(List.of(transformedHotel));
        when(hotelRepository.findWithHabitacionesByNombreIgnoreCaseAndIdCiudad(eq("Grand Mercure Interlagos"), eq(target.getCiudadId())))
                .thenReturn(Optional.of(existingHotel));
        when(hotelRepository.save(any(Hotel.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        BookingSyncResultDto result = bookingSyncService.syncBookingData(target);

        assertNotNull(result);
        assertEquals(0, result.hotelesCreados());
        assertEquals(1, result.hotelesActualizados());
        assertEquals(1, result.habitacionesActualizadas());
        assertEquals(0, result.habitacionesCreadas());

        assertEquals(4, existingHotel.getEstrellas());
        assertEquals(BigDecimal.valueOf(150.00), existingHab.getPrecioPorNocheUsd());
        assertEquals(10, existingHab.getStockDisponible());
    }

    @Test
    @DisplayName("MASIVO: Debe procesar los 19 destinos de F1 consolidando resultados")
    void shouldSyncAllTargetsConsolidatedSummary() {
        when(ciudadRepository.findFirstByNombreIgnoreCase(any()))
                .thenReturn(Optional.of(new Ciudad(UUID.randomUUID(), "Ciudad Test")));
        when(bookingClientService.extractHotels(any(GranPremioTarget.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(rawDto));
        when(bookingAdapter.toEntityList(any(), any(GranPremioTarget.class), any(UUID.class)))
                .thenReturn(List.of(transformedHotel));
        when(hotelRepository.findWithHabitacionesByNombreIgnoreCaseAndIdCiudad(any(), any()))
                .thenReturn(Optional.empty());
        when(hotelRepository.save(any(Hotel.class)))
                .thenAnswer(inv -> {
                    Hotel h = inv.getArgument(0);
                    h.setIdHotel(UUID.randomUUID());
                    return h;
                });

        BookingSyncAllSummaryDto summary = bookingSyncService.syncAllBookingData();

        assertNotNull(summary);
        assertEquals(19, summary.totalDestinosProcesados());
        assertEquals(19, summary.destinosExitosos());
        assertEquals(0, summary.destinosConError());
        assertEquals(19, summary.totalHotelesCreados());
        assertEquals(19, summary.resultados().size());
    }
}


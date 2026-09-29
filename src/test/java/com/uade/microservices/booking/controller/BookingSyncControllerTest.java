package com.uade.microservices.booking.controller;

import com.uade.microservices.booking.dto.response.BookingSyncAllSummaryDto;
import com.uade.microservices.booking.dto.response.BookingSyncResultDto;
import com.uade.microservices.booking.dto.response.HabitacionSyncSummaryDto;
import com.uade.microservices.booking.dto.response.HotelSyncSummaryDto;
import com.uade.microservices.booking.model.GranPremioTarget;
import com.uade.microservices.booking.service.BookingSyncService;
import com.uade.microservices.booking.shared.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingSyncControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookingSyncService bookingSyncService;

    @InjectMocks
    private BookingSyncController bookingSyncController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookingSyncController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/microservicios/sync-booking/{target} con destino válido debe retornar 200 OK y resultado")
    void testSyncBookingData_Success() throws Exception {
        GranPremioTarget target = GranPremioTarget.SAO_PAULO;

        HabitacionSyncSummaryDto hab = new HabitacionSyncSummaryDto(
                UUID.randomUUID(),
                "Single",
                BigDecimal.valueOf(150.00),
                10
        );

        HotelSyncSummaryDto hotel = new HotelSyncSummaryDto(
                UUID.randomUUID(),
                target.getCiudadId(),
                "Palácio Tangará",
                5,
                BigDecimal.valueOf(14.50),
                true,
                "https://example.com/photo.jpg",
                List.of(hab)
        );

        BookingSyncResultDto mockResult = new BookingSyncResultDto(
                target,
                target.getNombreCiudad(),
                target.getFechaCarrera(),
                target.getCheckinDate(),
                target.getCheckoutDate(),
                target.getDestId(),
                1,
                1,
                0,
                1,
                0,
                "SUCCESS",
                "Sincronización completada con éxito",
                OffsetDateTime.now(),
                List.of(hotel)
        );

        when(bookingSyncService.syncBookingData(eq(target))).thenReturn(mockResult);

        mockMvc.perform(post("/api/microservicios/sync-booking/SAO_PAULO")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.target").value("SAO_PAULO"))
                .andExpect(jsonPath("$.data.nombreCiudad").value("São Paulo"))
                .andExpect(jsonPath("$.data.fechaCarrera").value("2026-11-06"))
                .andExpect(jsonPath("$.data.checkinDate").value("2026-11-05"))
                .andExpect(jsonPath("$.data.checkoutDate").value("2026-11-09"))
                .andExpect(jsonPath("$.data.destId").value("-671824"))
                .andExpect(jsonPath("$.data.hotelesCreados").value(1))
                .andExpect(jsonPath("$.data.hotelesSincronizados[0].nombre").value("Palácio Tangará"))
                .andExpect(jsonPath("$.data.hotelesSincronizados[0].habitaciones[0].tipo").value("Single"));

        verify(bookingSyncService, times(1)).syncBookingData(target);
    }

    @Test
    @DisplayName("POST /api/microservicios/sync-booking/{target} con destino inválido debe retornar 400 Bad Request")
    void testSyncBookingData_InvalidTarget() throws Exception {
        mockMvc.perform(post("/api/microservicios/sync-booking/CIUDAD_INVENTADA")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Destino inválido")));

        verifyNoInteractions(bookingSyncService);
    }

    @Test
    @DisplayName("GET /api/microservicios/sync-booking/targets debe retornar el catálogo completo de 19 destinos")
    void testListAvailableTargets() throws Exception {
        mockMvc.perform(get("/api/microservicios/sync-booking/targets")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(19))
                .andExpect(jsonPath("$.data[0].target").value("MADRID"))
                .andExpect(jsonPath("$.data[0].checkinDate").value("2026-09-10"))
                .andExpect(jsonPath("$.data[0].checkoutDate").value("2026-09-14"));
    }

    @Test
    @DisplayName("POST /api/microservicios/sync-booking/all debe sincronizar todos los destinos y retornar consolidado")
    void testSyncAllBookingData() throws Exception {
        BookingSyncAllSummaryDto mockSummary = new BookingSyncAllSummaryDto(
                19, 19, 0, 57, 40, 17, 100, 20, OffsetDateTime.now(), List.of()
        );

        when(bookingSyncService.syncAllBookingData()).thenReturn(mockSummary);

        mockMvc.perform(post("/api/microservicios/sync-booking/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalDestinosProcesados").value(19))
                .andExpect(jsonPath("$.data.destinosExitosos").value(19))
                .andExpect(jsonPath("$.data.totalHotelesCreados").value(40));

        verify(bookingSyncService, times(1)).syncAllBookingData();
    }
}


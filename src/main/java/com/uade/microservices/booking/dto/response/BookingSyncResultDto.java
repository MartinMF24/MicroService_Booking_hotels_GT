package com.uade.microservices.booking.dto.response;

import com.uade.microservices.booking.model.GranPremioTarget;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record BookingSyncResultDto(
        GranPremioTarget target,
        String nombreCiudad,
        LocalDate fechaCarrera,
        LocalDate checkinDate,
        LocalDate checkoutDate,
        String destId,
        int totalHotelesExtraidos,
        int hotelesCreados,
        int hotelesActualizados,
        int habitacionesCreadas,
        int habitacionesActualizadas,
        String estado,
        String mensaje,
        OffsetDateTime timestamp,
        List<HotelSyncSummaryDto> hotelesSincronizados
) {
}


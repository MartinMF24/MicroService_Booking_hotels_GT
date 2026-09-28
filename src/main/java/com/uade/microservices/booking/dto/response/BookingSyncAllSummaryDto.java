package com.uade.microservices.booking.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO que consolida los resultados de la sincronización ETL masiva
 * para todos los destinos de Gran Premio de Fórmula 1.
 */
public record BookingSyncAllSummaryDto(
        int totalDestinosProcesados,
        int destinosExitosos,
        int destinosConError,
        int totalHotelesExtraidos,
        int totalHotelesCreados,
        int totalHotelesActualizados,
        int totalHabitacionesCreadas,
        int totalHabitacionesActualizadas,
        OffsetDateTime timestamp,
        List<BookingSyncResultDto> resultados
) {
}

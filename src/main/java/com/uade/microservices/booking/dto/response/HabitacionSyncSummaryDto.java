package com.uade.microservices.booking.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record HabitacionSyncSummaryDto(
        UUID idHabitacion,
        String tipo,
        BigDecimal precioPorNocheUsd,
        Integer stockDisponible
) {
}


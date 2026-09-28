package com.uade.microservices.booking.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record HotelSyncSummaryDto(
        UUID idHotel,
        UUID idCiudad,
        String nombre,
        Integer estrellas,
        BigDecimal distanciaCircuitoKm,
        Boolean ofreceTraslado,
        String imagenPrincipalUrl,
        List<HabitacionSyncSummaryDto> habitaciones
) {
}


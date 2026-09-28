package com.uade.microservices.booking.adapter;

import com.uade.microservices.booking.dto.rapidapi.BookingHotelRawDto;
import com.uade.microservices.booking.model.GranPremioTarget;
import com.uade.microservices.booking.model.HabitacionHotel;
import com.uade.microservices.booking.model.Hotel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Fase TRANSFORM del módulo ETL.
 * Implementa el Patrón Adapter para transformar los datos crudos y heterogéneos de Booking
 * en las entidades del dominio (Hotel y HabitacionHotel), normalizando tipos de datos,
 * restringiendo longitudes de columnas y simulando atributos faltantes (ej. distancia al circuito).
 */
@Component
public class BookingAdapter {

    private final Random random = new Random();

    /**
     * Transforma una lista de DTOs crudos de Booking a una lista de entidades Hotel usando el id_ciudad resuelto.
     */
    public List<Hotel> toEntityList(List<BookingHotelRawDto> rawDtos, GranPremioTarget target, UUID ciudadId) {
        if (rawDtos == null || rawDtos.isEmpty()) {
            return new ArrayList<>();
        }
        return rawDtos.stream()
                .map(rawDto -> toEntity(rawDto, target, ciudadId))
                .toList();
    }

    /**
     * Transforma una lista de DTOs crudos de Booking a una lista de entidades Hotel usando el id de fallback del target.
     */
    public List<Hotel> toEntityList(List<BookingHotelRawDto> rawDtos, GranPremioTarget target) {
        return toEntityList(rawDtos, target, target != null ? target.getCiudadId() : null);
    }

    /**
     * Transforma un DTO crudo individual en una entidad Hotel usando el id de fallback del target.
     */
    public Hotel toEntity(BookingHotelRawDto rawDto, GranPremioTarget target) {
        return toEntity(rawDto, target, target != null ? target.getCiudadId() : null);
    }

    /**
     * Transforma un DTO crudo individual en una entidad Hotel usando el id_ciudad resuelto.
     */
    public Hotel toEntity(BookingHotelRawDto rawDto, GranPremioTarget target, UUID ciudadId) {
        Hotel hotel = new Hotel();

        // 1. Asignar Ciudad asociada al Gran Premio (resuelto dinámicamente o fallback del enum)
        hotel.setIdCiudad(ciudadId != null ? ciudadId : (target != null ? target.getCiudadId() : null));

        // 2. Nombre formateado y truncado a 150 caracteres (según restricción de BD)
        String rawName = rawDto.resolveNombre();
        String safeName = rawName.length() > 150 ? rawName.substring(0, 150).trim() : rawName;
        hotel.setNombre(safeName);

        // 3. Estrellas (1 - 5)
        hotel.setEstrellas(rawDto.resolveEstrellas());

        // 4. Distancia al Circuito (km) - Simula o adapta distancia numérica (numeric(5, 2))
        BigDecimal distanciaKm = resolveDistanciaCircuito(rawDto, target);
        hotel.setDistanciaCircuitoKm(distanciaKm);

        // 5. Ofrece Traslado (Hoteles 4 y 5 estrellas o con parking tienen alta probabilidad de traslado)
        boolean traslado = (hotel.getEstrellas() >= 4) || Boolean.TRUE.equals(rawDto.getHasFreeParking());
        hotel.setOfreceTraslado(traslado);

        // 6. Imagen Principal
        hotel.setImagenPrincipalUrl(rawDto.resolveImagenUrl());

        // 7. Auditoría
        hotel.setCreatedAt(OffsetDateTime.now());

        // 8. Transformar e instanciar Habitaciones correspondientes
        List<HabitacionHotel> habitaciones = buildHabitacionesForHotel(hotel, rawDto);
        for (HabitacionHotel habitacion : habitaciones) {
            hotel.addHabitacion(habitacion);
        }

        return hotel;
    }

    /**
     * Calcula o simula una distancia coherente al autódromo / circuito de F1.
     * Retorna un BigDecimal con precisión 5 y escala 2 (máximo 999.99).
     */
    private BigDecimal resolveDistanciaCircuito(BookingHotelRawDto rawDto, GranPremioTarget target) {
        if (rawDto.getDistance() != null) {
            try {
                double parsed = Double.parseDouble(rawDto.getDistance().replaceAll("[^0-9.]", ""));
                if (parsed > 0 && parsed < 500) {
                    return BigDecimal.valueOf(parsed).setScale(2, RoundingMode.HALF_UP);
                }
            } catch (Exception ignored) {
                // Fallback a simulación
            }
        }

        double baseKm = switch (target) {
            case MADRID -> 14.50;
            case BAKU -> 2.30;
            case SINGAPUR -> 1.80;
            case AUSTIN -> 18.20;
            case CIUDAD_DE_MEXICO -> 8.40;
            case SAO_PAULO -> 16.50;
            case LAS_VEGAS -> 3.10;
            case LUSAIL -> 22.00;
            case ABU_DABI -> 4.20;
        };

        int hash = Math.abs(rawDto.resolveNombre().hashCode() % 50);
        double variance = (hash / 10.0) - 2.5;
        double finalDistance = Math.max(0.80, baseKm + variance);

        return BigDecimal.valueOf(finalDistance).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Genera las habitaciones estructuradas (Estándar, Deluxe, Suite) con precios y stock acordes.
     */
    private List<HabitacionHotel> buildHabitacionesForHotel(Hotel hotel, BookingHotelRawDto rawDto) {
        List<HabitacionHotel> habitaciones = new ArrayList<>();
        BigDecimal precioBase = rawDto.resolvePrecioBase().setScale(2, RoundingMode.HALF_UP);

        // 1. Habitación Estándar
        HabitacionHotel estandar = new HabitacionHotel();
        estandar.setTipo("Estándar");
        estandar.setPrecioPorNocheUsd(precioBase);
        estandar.setStockDisponible(8 + (Math.abs(hotel.getNombre().hashCode()) % 8));
        estandar.setCreatedAt(OffsetDateTime.now());
        habitaciones.add(estandar);

        // 2. Habitación Deluxe (un 35% más cara)
        BigDecimal precioDeluxe = precioBase.multiply(BigDecimal.valueOf(1.35)).setScale(2, RoundingMode.HALF_UP);
        HabitacionHotel deluxe = new HabitacionHotel();
        deluxe.setTipo("Deluxe");
        deluxe.setPrecioPorNocheUsd(precioDeluxe);
        deluxe.setStockDisponible(3 + (Math.abs(hotel.getNombre().hashCode()) % 6));
        deluxe.setCreatedAt(OffsetDateTime.now());
        habitaciones.add(deluxe);

        // 3. Suite Ejecutiva (un 80% más cara) para hoteles de 4 o 5 estrellas
        if (hotel.getEstrellas() != null && hotel.getEstrellas() >= 4) {
            BigDecimal precioSuite = precioBase.multiply(BigDecimal.valueOf(1.80)).setScale(2, RoundingMode.HALF_UP);
            HabitacionHotel suite = new HabitacionHotel();
            suite.setTipo("Suite Ejecutiva");
            suite.setPrecioPorNocheUsd(precioSuite);
            suite.setStockDisponible(1 + (Math.abs(hotel.getNombre().hashCode()) % 3));
            suite.setCreatedAt(OffsetDateTime.now());
            habitaciones.add(suite);
        }

        return habitaciones;
    }
}


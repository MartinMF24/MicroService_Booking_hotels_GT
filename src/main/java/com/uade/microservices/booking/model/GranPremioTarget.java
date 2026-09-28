package com.uade.microservices.booking.model;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Catálogo de destinos de Gran Premio de Fórmula 1 y fechas de carrera para 2026.
 * Incluye metadatos necesarios para el proceso ETL de Booking (dest_id, fechas y ciudad asociada).
 */
public enum GranPremioTarget {

    MADRID(
            "2026-09-11",
            "-391194",
            "city",
            "Madrid",
            UUID.fromString("11111111-1111-1111-1111-111111111101")
    ),
    BAKU(
            "2026-09-25",
            "-2422998",
            "city",
            "Bakú",
            UUID.fromString("22222222-2222-2222-2222-222222222202")
    ),
    SINGAPUR(
            "2026-10-09",
            "-114060",
            "city",
            "Singapur",
            UUID.fromString("33333333-3333-3333-3333-333333333303")
    ),
    AUSTIN(
            "2026-10-23",
            "20014288",
            "city",
            "Austin",
            UUID.fromString("44444444-4444-4444-4444-444444444404")
    ),
    CIUDAD_DE_MEXICO(
            "2026-10-30",
            "-1658079",
            "city",
            "Ciudad de México",
            UUID.fromString("55555555-5555-5555-5555-555555555505")
    ),
    SAO_PAULO(
            "2026-11-06",
            "-671824",
            "city",
            "São Paulo",
            UUID.fromString("66666666-6666-6666-6666-666666666606")
    ),
    LAS_VEGAS(
            "2026-11-19",
            "20079110",
            "city",
            "Las Vegas",
            UUID.fromString("77777777-7777-7777-7777-777777777707")
    ),
    LUSAIL(
            "2026-11-27",
            "-2092875",
            "city",
            "Lusail",
            UUID.fromString("88888888-8888-8888-8888-888888888808")
    ),
    ABU_DABI(
            "2026-12-04",
            "-782066",
            "city",
            "Abu Dabi",
            UUID.fromString("99999999-9999-9999-9999-999999999909")
    );

    private final LocalDate fechaCarrera;
    private final String destId;
    private final String destType;
    private final String nombreCiudad;
    private final UUID ciudadId;

    GranPremioTarget(String fechaCarrera, String destId, String destType, String nombreCiudad, UUID ciudadId) {
        this.fechaCarrera = LocalDate.parse(fechaCarrera);
        this.destId = destId;
        this.destType = destType;
        this.nombreCiudad = nombreCiudad;
        this.ciudadId = ciudadId;
    }

    public LocalDate getFechaCarrera() {
        return fechaCarrera;
    }

    public String getDestId() {
        return destId;
    }

    public String getDestType() {
        return destType;
    }

    public String getNombreCiudad() {
        return nombreCiudad;
    }

    public UUID getCiudadId() {
        return ciudadId;
    }

    /**
     * Calcula la fecha de check-in: 1 día antes de la fecha principal de la carrera.
     */
    public LocalDate getCheckinDate() {
        return this.fechaCarrera.minusDays(1);
    }

    /**
     * Calcula la fecha de check-out: 3 días después de la fecha de la carrera.
     */
    public LocalDate getCheckoutDate() {
        return this.fechaCarrera.plusDays(3);
    }

    /**
     * Busca un destino por nombre insensible a mayúsculas, minúsculas o guiones.
     */
    public static Optional<GranPremioTarget> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase().replace("-", "_");
        return Arrays.stream(values())
                .filter(target -> target.name().equals(normalized))
                .findFirst();
    }
}


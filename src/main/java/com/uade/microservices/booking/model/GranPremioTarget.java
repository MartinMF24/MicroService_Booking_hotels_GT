package com.uade.microservices.booking.model;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Catálogo de destinos de Gran Premio de Fórmula 1 y fechas de carrera para las temporadas 2026 y 2027.
 * Incluye metadatos necesarios para el proceso ETL de Booking (dest_id, fechas y ciudad asociada).
 */
public enum GranPremioTarget {

    MADRID(
            "2026-09-11",
            "-391194",
            "city",
            "Madrid",
            UUID.fromString("34053323-6e73-47d9-b306-b4dc8df920cb")
    ),
    BAKU(
            "2026-09-25",
            "-2422998",
            "city",
            "Bakú",
            UUID.fromString("a010620b-e30a-48c6-8b2a-21ec0b18bb90")
    ),
    SINGAPUR(
            "2026-10-09",
            "-114060",
            "city",
            "Singapur",
            UUID.fromString("1c3915ad-97c0-46cf-9a93-d73826bee539")
    ),
    AUSTIN(
            "2026-10-23",
            "20014288",
            "city",
            "Austin",
            UUID.fromString("6f9d5fbb-c1a3-492e-a625-1cb1942282a6")
    ),
    CIUDAD_DE_MEXICO(
            "2026-10-30",
            "-1658079",
            "city",
            "Ciudad de México",
            UUID.fromString("c3be3c61-bb7c-433b-8249-b59b1eba96c9")
    ),
    SAO_PAULO(
            "2026-11-06",
            "-671824",
            "city",
            "São Paulo",
            UUID.fromString("4501eeb9-010b-468a-9024-4343f698cf58")
    ),
    LAS_VEGAS(
            "2026-11-19",
            "20079110",
            "city",
            "Las Vegas",
            UUID.fromString("0ddcaf82-55ca-4c1c-af87-b777acca60d4")
    ),
    LUSAIL(
            "2026-11-27",
            "-2092875",
            "city",
            "Lusail",
            UUID.fromString("a81419b4-d2d2-4bc7-9255-7db766e71a32")
    ),
    ABU_DABI(
            "2026-12-04",
            "-782066",
            "city",
            "Abu Dabi",
            UUID.fromString("d028fc05-78cd-4dd8-96d0-9134ff624468")
    ),
    SAKHIR(
            "2027-03-12",
            "-784871",
            "city",
            "Sakhir",
            UUID.fromString("3e1f5f36-aae4-4499-a3bf-469698515167")
    ),
    YEDA(
            "2027-03-19",
            "-3096108",
            "city",
            "Yeda",
            UUID.fromString("f8dcf9af-df06-4f0c-a72e-ae8100c9a7d2")
    ),
    MELBOURNE(
            "2027-04-02",
            "-1586844",
            "city",
            "Melbourne",
            UUID.fromString("545e94e2-b90e-4b1d-9a65-ed9f85710b00")
    ),
    SUZUKA(
            "2027-04-09",
            "-244616",
            "city",
            "Suzuka",
            UUID.fromString("afc1ef81-973a-4a82-a904-4e9fbf0688ba")
    ),
    SHANGHAI(
            "2027-04-16",
            "-1924465",
            "city",
            "Shanghái",
            UUID.fromString("66215536-b554-4028-90b0-be74b485f913")
    ),
    MIAMI(
            "2027-04-30",
            "20023181",
            "city",
            "Miami",
            UUID.fromString("b1044436-1e90-4ea4-aa04-a6be47db01d2")
    ),
    MONTREAL(
            "2027-05-21",
            "-569541",
            "city",
            "Montreal",
            UUID.fromString("36851257-396b-48fa-9539-219a07248dc0")
    ),
    MONTECARLO(
            "2027-06-04",
            "-1451964",
            "city",
            "Montecarlo",
            UUID.fromString("4feae55b-e6e4-4d97-b62c-bc55a501af51")
    ),
    PORTIMAO(
            "2027-06-18",
            "-2173080",
            "city",
            "Portimão",
            UUID.fromString("05e3a519-00e1-47a3-8c56-c3e6f5a06514")
    ),
    SILVERSTONE(
            "2027-07-02",
            "-2607823",
            "city",
            "Silverstone",
            UUID.fromString("909dfe29-dd7d-41ba-a580-545d1443b2b6")
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
        String normalized = java.text.Normalizer.normalize(value.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toUpperCase()
                .replace("-", "_")
                .replace(" ", "_");

        if ("MONTE_CARLO".equals(normalized)) {
            normalized = "MONTECARLO";
        }

        final String targetName = normalized;
        return Arrays.stream(values())
                .filter(target -> target.name().equals(targetName))
                .findFirst();
    }
}


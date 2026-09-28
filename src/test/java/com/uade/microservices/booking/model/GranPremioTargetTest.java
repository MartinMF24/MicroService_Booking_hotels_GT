package com.uade.microservices.booking.model;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GranPremioTargetTest {

    @Test
    @DisplayName("Debe contener los 9 destinos de F1 requeridos para la temporada 2026")
    void shouldContainAllNineRequiredTargets() {
        assertEquals(9, GranPremioTarget.values().length);
        assertNotNull(GranPremioTarget.valueOf("MADRID"));
        assertNotNull(GranPremioTarget.valueOf("BAKU"));
        assertNotNull(GranPremioTarget.valueOf("SINGAPUR"));
        assertNotNull(GranPremioTarget.valueOf("AUSTIN"));
        assertNotNull(GranPremioTarget.valueOf("CIUDAD_DE_MEXICO"));
        assertNotNull(GranPremioTarget.valueOf("SAO_PAULO"));
        assertNotNull(GranPremioTarget.valueOf("LAS_VEGAS"));
        assertNotNull(GranPremioTarget.valueOf("LUSAIL"));
        assertNotNull(GranPremioTarget.valueOf("ABU_DABI"));
    }

    @Test
    @DisplayName("Debe tener configuradas las fechas exactas de carrera para cada Gran Premio")
    void shouldHaveCorrectRaceDates() {
        assertEquals(LocalDate.of(2026, 9, 11), GranPremioTarget.MADRID.getFechaCarrera());
        assertEquals(LocalDate.of(2026, 9, 25), GranPremioTarget.BAKU.getFechaCarrera());
        assertEquals(LocalDate.of(2026, 10, 9), GranPremioTarget.SINGAPUR.getFechaCarrera());
        assertEquals(LocalDate.of(2026, 10, 23), GranPremioTarget.AUSTIN.getFechaCarrera());
        assertEquals(LocalDate.of(2026, 10, 30), GranPremioTarget.CIUDAD_DE_MEXICO.getFechaCarrera());
        assertEquals(LocalDate.of(2026, 11, 6), GranPremioTarget.SAO_PAULO.getFechaCarrera());
        assertEquals(LocalDate.of(2026, 11, 19), GranPremioTarget.LAS_VEGAS.getFechaCarrera());
        assertEquals(LocalDate.of(2026, 11, 27), GranPremioTarget.LUSAIL.getFechaCarrera());
        assertEquals(LocalDate.of(2026, 12, 4), GranPremioTarget.ABU_DABI.getFechaCarrera());
    }

    @Test
    @DisplayName("Debe calcular check-in como 1 día antes y check-out como 3 días después de la carrera")
    void shouldCalculateCorrectCheckinAndCheckoutDates() {
        GranPremioTarget target = GranPremioTarget.SAO_PAULO;
        // Carrera: 2026-11-06
        assertEquals(LocalDate.of(2026, 11, 5), target.getCheckinDate()); // -1 día
        assertEquals(LocalDate.of(2026, 11, 9), target.getCheckoutDate()); // +3 días

        GranPremioTarget madrid = GranPremioTarget.MADRID;
        // Carrera: 2026-09-11
        assertEquals(LocalDate.of(2026, 9, 10), madrid.getCheckinDate());
        assertEquals(LocalDate.of(2026, 9, 14), madrid.getCheckoutDate());
    }

    @Test
    @DisplayName("Todos los destinos deben tener dest_id no nulo ni vacío para Booking")
    void shouldHaveValidDestIdForAllTargets() {
        for (GranPremioTarget target : GranPremioTarget.values()) {
            assertNotNull(target.getDestId());
            assertFalse(target.getDestId().isBlank());
            assertNotNull(target.getCiudadId());
            assertNotNull(target.getNombreCiudad());
        }
    }

    @Test
    @DisplayName("fromString debe resolver destinos insensible a mayúsculas, minúsculas y guiones")
    void shouldResolveFromStringFlexibly() {
        Optional<GranPremioTarget> opt1 = GranPremioTarget.fromString("sao_paulo");
        assertTrue(opt1.isPresent());
        assertEquals(GranPremioTarget.SAO_PAULO, opt1.get());

        Optional<GranPremioTarget> opt2 = GranPremioTarget.fromString("SAO-PAULO");
        assertTrue(opt2.isPresent());
        assertEquals(GranPremioTarget.SAO_PAULO, opt2.get());

        Optional<GranPremioTarget> opt3 = GranPremioTarget.fromString("ciudad-de-mexico");
        assertTrue(opt3.isPresent());
        assertEquals(GranPremioTarget.CIUDAD_DE_MEXICO, opt3.get());

        Optional<GranPremioTarget> optInvalid = GranPremioTarget.fromString("DESCONOCIDO");
        assertTrue(optInvalid.isEmpty());

        assertTrue(GranPremioTarget.fromString(null).isEmpty());
        assertTrue(GranPremioTarget.fromString("   ").isEmpty());
    }
}


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
    @DisplayName("Debe contener los 19 destinos de F1 para las temporadas 2026 y 2027")
    void shouldContainAllNineteenRequiredTargets() {
        assertEquals(19, GranPremioTarget.values().length);
        // Temporada 2026
        assertNotNull(GranPremioTarget.valueOf("MADRID"));
        assertNotNull(GranPremioTarget.valueOf("BAKU"));
        assertNotNull(GranPremioTarget.valueOf("SINGAPUR"));
        assertNotNull(GranPremioTarget.valueOf("AUSTIN"));
        assertNotNull(GranPremioTarget.valueOf("CIUDAD_DE_MEXICO"));
        assertNotNull(GranPremioTarget.valueOf("SAO_PAULO"));
        assertNotNull(GranPremioTarget.valueOf("LAS_VEGAS"));
        assertNotNull(GranPremioTarget.valueOf("LUSAIL"));
        assertNotNull(GranPremioTarget.valueOf("ABU_DABI"));
        // Temporada 2027
        assertNotNull(GranPremioTarget.valueOf("SAKHIR"));
        assertNotNull(GranPremioTarget.valueOf("YEDA"));
        assertNotNull(GranPremioTarget.valueOf("MELBOURNE"));
        assertNotNull(GranPremioTarget.valueOf("SUZUKA"));
        assertNotNull(GranPremioTarget.valueOf("SHANGHAI"));
        assertNotNull(GranPremioTarget.valueOf("MIAMI"));
        assertNotNull(GranPremioTarget.valueOf("MONTREAL"));
        assertNotNull(GranPremioTarget.valueOf("MONTECARLO"));
        assertNotNull(GranPremioTarget.valueOf("PORTIMAO"));
        assertNotNull(GranPremioTarget.valueOf("SILVERSTONE"));
    }

    @Test
    @DisplayName("Debe tener configuradas las fechas exactas de carrera para cada Gran Premio")
    void shouldHaveCorrectRaceDates() {
        // Temporada 2026
        assertEquals(LocalDate.of(2026, 9, 11), GranPremioTarget.MADRID.getFechaCarrera());
        assertEquals(LocalDate.of(2026, 9, 25), GranPremioTarget.BAKU.getFechaCarrera());
        assertEquals(LocalDate.of(2026, 10, 9), GranPremioTarget.SINGAPUR.getFechaCarrera());
        assertEquals(LocalDate.of(2026, 10, 23), GranPremioTarget.AUSTIN.getFechaCarrera());
        assertEquals(LocalDate.of(2026, 10, 30), GranPremioTarget.CIUDAD_DE_MEXICO.getFechaCarrera());
        assertEquals(LocalDate.of(2026, 11, 6), GranPremioTarget.SAO_PAULO.getFechaCarrera());
        assertEquals(LocalDate.of(2026, 11, 19), GranPremioTarget.LAS_VEGAS.getFechaCarrera());
        assertEquals(LocalDate.of(2026, 11, 27), GranPremioTarget.LUSAIL.getFechaCarrera());
        assertEquals(LocalDate.of(2026, 12, 4), GranPremioTarget.ABU_DABI.getFechaCarrera());

        // Temporada 2027
        assertEquals(LocalDate.of(2027, 3, 12), GranPremioTarget.SAKHIR.getFechaCarrera());
        assertEquals(LocalDate.of(2027, 3, 19), GranPremioTarget.YEDA.getFechaCarrera());
        assertEquals(LocalDate.of(2027, 4, 2), GranPremioTarget.MELBOURNE.getFechaCarrera());
        assertEquals(LocalDate.of(2027, 4, 9), GranPremioTarget.SUZUKA.getFechaCarrera());
        assertEquals(LocalDate.of(2027, 4, 16), GranPremioTarget.SHANGHAI.getFechaCarrera());
        assertEquals(LocalDate.of(2027, 4, 30), GranPremioTarget.MIAMI.getFechaCarrera());
        assertEquals(LocalDate.of(2027, 5, 21), GranPremioTarget.MONTREAL.getFechaCarrera());
        assertEquals(LocalDate.of(2027, 6, 4), GranPremioTarget.MONTECARLO.getFechaCarrera());
        assertEquals(LocalDate.of(2027, 6, 18), GranPremioTarget.PORTIMAO.getFechaCarrera());
        assertEquals(LocalDate.of(2027, 7, 2), GranPremioTarget.SILVERSTONE.getFechaCarrera());
    }

    @Test
    @DisplayName("Debe calcular check-in y check-out exactos para destinos 2026 y 2027")
    void shouldCalculateCorrectCheckinAndCheckoutDates() {
        // Sao Paulo 2026: Llegada 2026-11-05 — Salida 2026-11-09
        assertEquals(LocalDate.of(2026, 11, 5), GranPremioTarget.SAO_PAULO.getCheckinDate());
        assertEquals(LocalDate.of(2026, 11, 9), GranPremioTarget.SAO_PAULO.getCheckoutDate());

        // Sakhir (Bahréin): Llegada 11 de marzo de 2027 — Salida 15 de marzo de 2027
        assertEquals(LocalDate.of(2027, 3, 11), GranPremioTarget.SAKHIR.getCheckinDate());
        assertEquals(LocalDate.of(2027, 3, 15), GranPremioTarget.SAKHIR.getCheckoutDate());

        // Yeda (Arabia Saudita): Llegada 18 de marzo de 2027 — Salida 22 de marzo de 2027
        assertEquals(LocalDate.of(2027, 3, 18), GranPremioTarget.YEDA.getCheckinDate());
        assertEquals(LocalDate.of(2027, 3, 22), GranPremioTarget.YEDA.getCheckoutDate());

        // Melbourne (Australia): Llegada 1 de abril de 2027 — Salida 5 de abril de 2027
        assertEquals(LocalDate.of(2027, 4, 1), GranPremioTarget.MELBOURNE.getCheckinDate());
        assertEquals(LocalDate.of(2027, 4, 5), GranPremioTarget.MELBOURNE.getCheckoutDate());

        // Suzuka (Japón): Llegada 8 de abril de 2027 — Salida 12 de abril de 2027
        assertEquals(LocalDate.of(2027, 4, 8), GranPremioTarget.SUZUKA.getCheckinDate());
        assertEquals(LocalDate.of(2027, 4, 12), GranPremioTarget.SUZUKA.getCheckoutDate());

        // Shanghái (China): Llegada 15 de abril de 2027 — Salida 19 de abril de 2027
        assertEquals(LocalDate.of(2027, 4, 15), GranPremioTarget.SHANGHAI.getCheckinDate());
        assertEquals(LocalDate.of(2027, 4, 19), GranPremioTarget.SHANGHAI.getCheckoutDate());

        // Miami (Estados Unidos): Llegada 29 de abril de 2027 — Salida 3 de mayo de 2027
        assertEquals(LocalDate.of(2027, 4, 29), GranPremioTarget.MIAMI.getCheckinDate());
        assertEquals(LocalDate.of(2027, 5, 3), GranPremioTarget.MIAMI.getCheckoutDate());

        // Montreal (Canadá): Llegada 20 de mayo de 2027 — Salida 24 de mayo de 2027
        assertEquals(LocalDate.of(2027, 5, 20), GranPremioTarget.MONTREAL.getCheckinDate());
        assertEquals(LocalDate.of(2027, 5, 24), GranPremioTarget.MONTREAL.getCheckoutDate());

        // Montecarlo (Mónaco): Llegada 3 de junio de 2027 — Salida 7 de junio de 2027
        assertEquals(LocalDate.of(2027, 6, 3), GranPremioTarget.MONTECARLO.getCheckinDate());
        assertEquals(LocalDate.of(2027, 6, 7), GranPremioTarget.MONTECARLO.getCheckoutDate());

        // Portimão (Portugal): Llegada 17 de junio de 2027 — Salida 21 de junio de 2027
        assertEquals(LocalDate.of(2027, 6, 17), GranPremioTarget.PORTIMAO.getCheckinDate());
        assertEquals(LocalDate.of(2027, 6, 21), GranPremioTarget.PORTIMAO.getCheckoutDate());

        // Silverstone (Reino Unido): Llegada 1 de julio de 2027 — Salida 5 de julio de 2027
        assertEquals(LocalDate.of(2027, 7, 1), GranPremioTarget.SILVERSTONE.getCheckinDate());
        assertEquals(LocalDate.of(2027, 7, 5), GranPremioTarget.SILVERSTONE.getCheckoutDate());
    }

    @Test
    @DisplayName("Todos los 19 destinos deben tener dest_id no nulo ni vacío para Booking")
    void shouldHaveValidDestIdForAllTargets() {
        for (GranPremioTarget target : GranPremioTarget.values()) {
            assertNotNull(target.getDestId());
            assertFalse(target.getDestId().isBlank());
            assertNotNull(target.getCiudadId());
            assertNotNull(target.getNombreCiudad());
        }
    }

    @Test
    @DisplayName("fromString debe resolver destinos insensible a mayúsculas, minúsculas, tildes y guiones")
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

        // Nuevos destinos 2027
        Optional<GranPremioTarget> optSakhir = GranPremioTarget.fromString("sakhir");
        assertTrue(optSakhir.isPresent());
        assertEquals(GranPremioTarget.SAKHIR, optSakhir.get());

        Optional<GranPremioTarget> optShanghaiAccented = GranPremioTarget.fromString("shanghái");
        assertTrue(optShanghaiAccented.isPresent());
        assertEquals(GranPremioTarget.SHANGHAI, optShanghaiAccented.get());

        Optional<GranPremioTarget> optPortimaoAccented = GranPremioTarget.fromString("portimão");
        assertTrue(optPortimaoAccented.isPresent());
        assertEquals(GranPremioTarget.PORTIMAO, optPortimaoAccented.get());

        Optional<GranPremioTarget> optMonteCarlo = GranPremioTarget.fromString("monte-carlo");
        assertTrue(optMonteCarlo.isPresent());
        assertEquals(GranPremioTarget.MONTECARLO, optMonteCarlo.get());

        Optional<GranPremioTarget> optInvalid = GranPremioTarget.fromString("DESCONOCIDO");
        assertTrue(optInvalid.isEmpty());

        assertTrue(GranPremioTarget.fromString(null).isEmpty());
        assertTrue(GranPremioTarget.fromString("   ").isEmpty());
    }
}

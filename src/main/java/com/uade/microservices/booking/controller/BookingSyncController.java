package com.uade.microservices.booking.controller;

import com.uade.microservices.booking.dto.response.BookingSyncAllSummaryDto;
import com.uade.microservices.booking.dto.response.BookingSyncResultDto;
import com.uade.microservices.booking.model.GranPremioTarget;
import com.uade.microservices.booking.service.BookingSyncService;
import com.uade.microservices.booking.shared.response.ApiResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la ejecución manual del módulo ETL de Booking.
 * Provee endpoints individuales por destino (POST /{target}) y masivos (POST /all).
 */
@RestController
@RequestMapping("/api/microservicios/sync-booking")
public class BookingSyncController {

    private static final Logger log = LoggerFactory.getLogger(BookingSyncController.class);

    private final BookingSyncService bookingSyncService;

    public BookingSyncController(BookingSyncService bookingSyncService) {
        this.bookingSyncService = bookingSyncService;
    }

    /**
     * Endpoint para ejecutar la sincronización ETL masiva de hoteles de Booking
     * para TODOS los destinos de Gran Premio de Fórmula 1 configurados en el catálogo.
     * Carga cada ciudad en sus fechas correspondientes (-1 día check-in, +3 días check-out).
     *
     * @return ApiResponse con el consolidado general y la lista detallada por destino.
     */
    @PostMapping({"/all", "/sync-all", ""})
    public ResponseEntity<ApiResponse<BookingSyncAllSummaryDto>> syncAllBookingData() {
        log.info("Petición recibida para sincronización masiva de TODOS los destinos de Booking");
        BookingSyncAllSummaryDto summary = bookingSyncService.syncAllBookingData();
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Sincronización masiva completada: %d destinos exitosos de %d procesados",
                        summary.destinosExitosos(), summary.totalDestinosProcesados()),
                summary
        ));
    }

    /**
     * Endpoint para ejecutar manualmente la sincronización ETL de hoteles de Booking
     * para un Gran Premio específico.
     *
     * @param target Nombre del enum GranPremioTarget (ej: SAO_PAULO, MADRID, BAKU, etc.)
     * @return ApiResponse con los detalles de hoteles y habitaciones sincronizados.
     */
    @PostMapping("/{target}")
    public ResponseEntity<?> syncBookingData(@PathVariable("target") String target) {
        log.info("Petición recibida para sincronización manual de Booking: target={}", target);

        if ("all".equalsIgnoreCase(target) || "sync-all".equalsIgnoreCase(target)) {
            return syncAllBookingData();
        }

        Optional<GranPremioTarget> targetOpt = GranPremioTarget.fromString(target);
        if (targetOpt.isEmpty()) {
            List<String> validTargets = Arrays.stream(GranPremioTarget.values())
                    .map(Enum::name)
                    .toList();
            String errorMessage = String.format(
                    "Destino inválido: '%s'. Los destinos válidos son: %s",
                    target, validTargets
            );
            log.warn(errorMessage);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(errorMessage));
        }

        GranPremioTarget granPremio = targetOpt.get();
        BookingSyncResultDto result = bookingSyncService.syncBookingData(granPremio);

        return ResponseEntity.ok(ApiResponse.success(
                "Sincronización ETL de Booking ejecutada correctamente para " + granPremio.getNombreCiudad(),
                result
        ));
    }

    /**
     * Endpoint informativo auxiliar para listar todos los destinos disponibles en el catálogo
     * con sus fechas principales de carrera, check-in (-2 días) y check-out (+1 día).
     */
    @GetMapping("/targets")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listAvailableTargets() {
        List<Map<String, Object>> targets = Arrays.stream(GranPremioTarget.values())
                .map(t -> Map.<String, Object>of(
                        "target", t.name(),
                        "ciudad", t.getNombreCiudad(),
                        "destId", t.getDestId(),
                        "fechaCarrera", t.getFechaCarrera().toString(),
                        "checkinDate", t.getCheckinDate().toString(),
                        "checkoutDate", t.getCheckoutDate().toString()
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Catálogo de Gran Premios disponibles para sincronización", targets));
    }
}


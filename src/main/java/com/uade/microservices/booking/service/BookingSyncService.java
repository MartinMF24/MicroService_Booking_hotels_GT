package com.uade.microservices.booking.service;

import com.uade.microservices.booking.adapter.BookingAdapter;
import com.uade.microservices.booking.dto.rapidapi.BookingHotelRawDto;
import com.uade.microservices.booking.dto.response.BookingSyncAllSummaryDto;
import com.uade.microservices.booking.dto.response.BookingSyncResultDto;
import com.uade.microservices.booking.dto.response.HabitacionSyncSummaryDto;
import com.uade.microservices.booking.dto.response.HotelSyncSummaryDto;
import com.uade.microservices.booking.model.Ciudad;
import com.uade.microservices.booking.model.GranPremioTarget;
import com.uade.microservices.booking.model.HabitacionHotel;
import com.uade.microservices.booking.model.Hotel;
import com.uade.microservices.booking.repository.CiudadRepository;
import com.uade.microservices.booking.repository.HotelRepository;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio orquestador del módulo ETL de Booking (Extract, Transform, Load).
 * Contiene la lógica central del proceso y la operación de Upsert en PostgreSQL/Supabase.
 */
@Service
public class BookingSyncService {

    private static final Logger log = LoggerFactory.getLogger(BookingSyncService.class);

    private final BookingClientService bookingClientService;
    private final BookingAdapter bookingAdapter;
    private final HotelRepository hotelRepository;
    private final CiudadRepository ciudadRepository;

    public BookingSyncService(
            BookingClientService bookingClientService,
            BookingAdapter bookingAdapter,
            HotelRepository hotelRepository,
            CiudadRepository ciudadRepository
    ) {
        this.bookingClientService = bookingClientService;
        this.bookingAdapter = bookingAdapter;
        this.hotelRepository = hotelRepository;
        this.ciudadRepository = ciudadRepository;
    }

    /**
     * Ejecuta el proceso ETL para todos los destinos de Gran Premio disponibles en el catálogo 2026.
     * Sincroniza cada ciudad con sus fechas correspondientes (-1 día check-in, +3 días check-out)
     * e integra un manejo de excepciones individual para que un fallo no cancele a las demás ciudades.
     *
     * @return Resumen consolidado del proceso para todas las ciudades.
     */
    public BookingSyncAllSummaryDto syncAllBookingData() {
        log.info("Iniciando sincronización masiva de Booking para todos los destinos de Gran Premio...");

        List<BookingSyncResultDto> resultados = new ArrayList<>();
        int totalHotelesSincronizados = 0;
        int totalHotelesCreados = 0;
        int totalHotelesActualizados = 0;
        int totalHabitacionesCreadas = 0;
        int totalHabitacionesActualizadas = 0;
        int destinosExitosos = 0;
        int destinosConError = 0;

        for (GranPremioTarget target : GranPremioTarget.values()) {
            try {
                log.info("Sincronizando destino masivo: {} ({})", target.name(), target.getNombreCiudad());
                BookingSyncResultDto res = syncBookingData(target);
                resultados.add(res);

                totalHotelesSincronizados += res.totalHotelesExtraidos();
                totalHotelesCreados += res.hotelesCreados();
                totalHotelesActualizados += res.hotelesActualizados();
                totalHabitacionesCreadas += res.habitacionesCreadas();
                totalHabitacionesActualizadas += res.habitacionesActualizadas();

                if ("SUCCESS".equalsIgnoreCase(res.estado())) {
                    destinosExitosos++;
                } else {
                    destinosConError++;
                }
            } catch (Exception ex) {
                destinosConError++;
                log.error("Error al sincronizar el destino {}: {}", target.name(), ex.getMessage(), ex);
                resultados.add(new BookingSyncResultDto(
                        target,
                        target.getNombreCiudad(),
                        target.getFechaCarrera(),
                        target.getCheckinDate(),
                        target.getCheckoutDate(),
                        target.getDestId(),
                        0, 0, 0, 0, 0,
                        "ERROR",
                        "Error al sincronizar: " + ex.getMessage(),
                        OffsetDateTime.now(),
                        List.of()
                ));
            }
        }

        log.info("Sincronización masiva finalizada. Destinos procesados: {}, Exitosos: {}, Con error: {}, Hoteles creados: {}, Hoteles actualizados: {}",
                GranPremioTarget.values().length, destinosExitosos, destinosConError, totalHotelesCreados, totalHotelesActualizados);

        return new BookingSyncAllSummaryDto(
                GranPremioTarget.values().length,
                destinosExitosos,
                destinosConError,
                totalHotelesSincronizados,
                totalHotelesCreados,
                totalHotelesActualizados,
                totalHabitacionesCreadas,
                totalHabitacionesActualizadas,
                OffsetDateTime.now(),
                resultados
        );
    }

    /**
     * Ejecuta el pipeline ETL completo para el destino indicado:
     * 1. Consulta en la tabla 'ciudades' de la BD para obtener el id_ciudad real por nombre.
     * 2. EXTRACT: Consulta a la API de Booking (o fallback).
     * 3. TRANSFORM: Adaptación y enriquecimiento de datos asociando el id_ciudad real.
     * 4. LOAD (UPSERT): Persistencia atómica en la base de datos PostgreSQL.
     */
    @Transactional
    public BookingSyncResultDto syncBookingData(GranPremioTarget target) {
        log.info("Iniciando proceso ETL de Booking para el Gran Premio: {}", target.name());

        // 1. Resolver el id_ciudad dinámicamente desde la tabla 'ciudades'
        UUID ciudadId = resolveCiudadId(target);

        LocalDate checkinDate = target.getCheckinDate();
        LocalDate checkoutDate = target.getCheckoutDate();

        log.info("Fechas calculadas para {}: Carrera={}, Check-in={}, Check-out={}, CiudadId={}",
                target.name(), target.getFechaCarrera(), checkinDate, checkoutDate, ciudadId);

        // 2. EXTRACT
        List<BookingHotelRawDto> rawHotels = bookingClientService.extractHotels(target, checkinDate, checkoutDate);

        // 3. TRANSFORM (asociando el id_ciudad real de Supabase)
        List<Hotel> transformedHotels = bookingAdapter.toEntityList(rawHotels, target, ciudadId);

        // 4. LOAD (UPSERT)
        int hotelesCreados = 0;
        int hotelesActualizados = 0;
        int habitacionesCreadas = 0;
        int habitacionesActualizadas = 0;
        List<HotelSyncSummaryDto> summaries = new ArrayList<>();

        for (Hotel transformed : transformedHotels) {
            Optional<Hotel> existingOpt = hotelRepository.findWithHabitacionesByNombreIgnoreCaseAndIdCiudad(
                    transformed.getNombre(),
                    ciudadId
            );

            Hotel savedHotel;

            if (existingOpt.isPresent()) {
                // El hotel ya existe en la base de datos -> Actualizar metadatos y habitaciones
                Hotel existing = existingOpt.get();
                existing.setEstrellas(transformed.getEstrellas());
                existing.setDistanciaCircuitoKm(transformed.getDistanciaCircuitoKm());
                existing.setOfreceTraslado(transformed.getOfreceTraslado());
                existing.setImagenPrincipalUrl(transformed.getImagenPrincipalUrl());

                // Upsert en habitaciones
                for (HabitacionHotel nuevaHab : transformed.getHabitaciones()) {
                    Optional<HabitacionHotel> habExistenteOpt = existing.getHabitaciones().stream()
                            .filter(h -> h.getTipo().equalsIgnoreCase(nuevaHab.getTipo()))
                            .findFirst();

                    if (habExistenteOpt.isPresent()) {
                        HabitacionHotel habExistente = habExistenteOpt.get();
                        habExistente.setPrecioPorNocheUsd(nuevaHab.getPrecioPorNocheUsd());
                        habExistente.setStockDisponible(nuevaHab.getStockDisponible());
                        habitacionesActualizadas++;
                    } else {
                        existing.addHabitacion(nuevaHab);
                        habitacionesCreadas++;
                    }
                }

                savedHotel = hotelRepository.save(existing);
                hotelesActualizados++;
                log.debug("Hotel actualizado [UPSERT]: {} (id={})", savedHotel.getNombre(), savedHotel.getIdHotel());

            } else {
                // El hotel es nuevo -> Guardar nuevo hotel y sus habitaciones en cascada
                savedHotel = hotelRepository.save(transformed);
                hotelesCreados++;
                habitacionesCreadas += transformed.getHabitaciones().size();
                log.debug("Nuevo hotel registrado [UPSERT]: {} (id={})", savedHotel.getNombre(), savedHotel.getIdHotel());
            }

            summaries.add(toSummaryDto(savedHotel));
        }

        String estado = "SUCCESS";
        String mensaje = String.format(
                "Sincronización ETL de Booking para %s completada con éxito. Hoteles creados: %d, actualizados: %d. Habitaciones creadas: %d, actualizadas: %d.",
                target.getNombreCiudad(), hotelesCreados, hotelesActualizados, habitacionesCreadas, habitacionesActualizadas
        );

        log.info(mensaje);

        return new BookingSyncResultDto(
                target,
                target.getNombreCiudad(),
                target.getFechaCarrera(),
                checkinDate,
                checkoutDate,
                target.getDestId(),
                rawHotels.size(),
                hotelesCreados,
                hotelesActualizados,
                habitacionesCreadas,
                habitacionesActualizadas,
                estado,
                mensaje,
                OffsetDateTime.now(),
                summaries
        );
    }

    private HotelSyncSummaryDto toSummaryDto(Hotel hotel) {
        List<HabitacionSyncSummaryDto> habSummaries = hotel.getHabitaciones().stream()
                .map(h -> new HabitacionSyncSummaryDto(
                        h.getIdHabitacion(),
                        h.getTipo(),
                        h.getPrecioPorNocheUsd(),
                        h.getStockDisponible()
                ))
                .toList();

        return new HotelSyncSummaryDto(
                hotel.getIdHotel(),
                hotel.getIdCiudad(),
                hotel.getNombre(),
                hotel.getEstrellas(),
                hotel.getDistanciaCircuitoKm(),
                hotel.getOfreceTraslado(),
                hotel.getImagenPrincipalUrl(),
                habSummaries
        );
    }

    /**
     * Busca la ciudad en la tabla 'ciudades' de Supabase por su nombre.
     * Soporta coincidencia exacta insensible a mayúsculas/minúsculas, variantes sin tildes, alias y búsqueda flexible.
     */
    private UUID resolveCiudadId(GranPremioTarget target) {
        String nombreCiudad = target.getNombreCiudad();

        // 1. Intento por nombre exacto insensible a mayúsculas/minúsculas
        Optional<Ciudad> ciudadOpt = ciudadRepository.findFirstByNombreIgnoreCase(nombreCiudad);
        if (ciudadOpt.isPresent()) {
            UUID id = ciudadOpt.get().getIdCiudad();
            log.info("Ciudad encontrada en BD para {}: '{}' (id_ciudad={})", target.name(), ciudadOpt.get().getNombre(), id);
            return id;
        }

        // 2. Intento por versión sin acentos / tildes
        String normalized = stripAccents(nombreCiudad);
        if (!normalized.equalsIgnoreCase(nombreCiudad)) {
            ciudadOpt = ciudadRepository.findFirstByNombreIgnoreCase(normalized);
            if (ciudadOpt.isPresent()) {
                UUID id = ciudadOpt.get().getIdCiudad();
                log.info("Ciudad encontrada en BD para {} vía sin tildes '{}': '{}' (id_ciudad={})",
                        target.name(), normalized, ciudadOpt.get().getNombre(), id);
                return id;
            }
        }

        // 3. Intento por alias conocidos
        List<String> aliases = getCityAliases(target);
        for (String alias : aliases) {
            ciudadOpt = ciudadRepository.findFirstByNombreIgnoreCase(alias);
            if (ciudadOpt.isPresent()) {
                UUID id = ciudadOpt.get().getIdCiudad();
                log.info("Ciudad encontrada en BD para {} vía alias '{}': '{}' (id_ciudad={})",
                        target.name(), alias, ciudadOpt.get().getNombre(), id);
                return id;
            }
        }

        // 4. Intento por búsqueda parcial (LIKE)
        List<Ciudad> partialMatches = ciudadRepository.searchByTermIgnoreCase(normalized);
        if (!partialMatches.isEmpty()) {
            UUID id = partialMatches.get(0).getIdCiudad();
            log.info("Ciudad encontrada en BD para {} vía búsqueda parcial '{}': '{}' (id_ciudad={})",
                    target.name(), normalized, partialMatches.get(0).getNombre(), id);
            return id;
        }

        // 5. Fallback a id preconfigurado en el enum si existiera
        if (target.getCiudadId() != null) {
            log.warn("No se encontró la ciudad '{}' en la tabla 'ciudades'. Usando id predeterminado de fallback: {}",
                    nombreCiudad, target.getCiudadId());
            return target.getCiudadId();
        }

        throw new IllegalStateException("No se encontró la ciudad '" + nombreCiudad
                + "' en la tabla 'ciudades' de la base de datos.");
    }

    private String stripAccents(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private List<String> getCityAliases(GranPremioTarget target) {
        return switch (target) {
            case MADRID -> List.of("Madrid");
            case BAKU -> List.of("Baku", "Bakú");
            case SINGAPUR -> List.of("Singapore", "Singapur");
            case AUSTIN -> List.of("Austin");
            case CIUDAD_DE_MEXICO -> List.of("Ciudad de Mexico", "Mexico", "CDMX", "Ciudad de México");
            case SAO_PAULO -> List.of("Sao Paulo", "São Paulo", "San Pablo");
            case LAS_VEGAS -> List.of("Las Vegas");
            case LUSAIL -> List.of("Lusail", "Doha");
            case ABU_DABI -> List.of("Abu Dhabi", "Abu Dabi");
        };
    }
}


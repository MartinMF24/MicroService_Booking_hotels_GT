package com.uade.microservices.booking.service;

import com.uade.microservices.booking.config.BookingRapidApiProperties;
import com.uade.microservices.booking.dto.rapidapi.BookingHotelRawDto;
import com.uade.microservices.booking.dto.rapidapi.RapidApiBookingResponseDto;
import com.uade.microservices.booking.model.GranPremioTarget;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Fase EXTRACT del módulo ETL.
 * Se encarga de consumir la API de Booking en RapidAPI y mapear la respuesta cruda a DTOs.
 */
@Service
public class BookingClientService {

    private static final Logger log = LoggerFactory.getLogger(BookingClientService.class);

    private final RestTemplate restTemplate;
    private final BookingRapidApiProperties properties;
    private final ObjectMapper objectMapper;

    public BookingClientService(
            @Qualifier("bookingRestTemplate") RestTemplate restTemplate,
            BookingRapidApiProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Consume la API externa de RapidAPI Booking pasando el dest_id, checkin_date y checkout_date.
     * En caso de credenciales ausentes o error de cuota/red en la API externa, provee un fallback
     * con datos representativos para garantizar continuidad operativa y testeabilidad del ETL.
     */
    public List<BookingHotelRawDto> extractHotels(GranPremioTarget target, LocalDate checkinDate, LocalDate checkoutDate) {
        if (!properties.hasValidCredentials()) {
            log.warn("RapidAPI Key no configurada o es inválida (RAPIDAPI_KEY). Usando datos de simulación representativos para {}.", target.name());
            return generateSimulatedHotels(target);
        }

        try {
            URI uri = buildRequestUri(target.getDestId(), target.getDestType(), checkinDate, checkoutDate);
            HttpHeaders headers = buildRequestHeaders();
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            log.info("Llamando a RapidAPI Booking: {} (target={}, dest_id={}, checkin={}, checkout={})",
                    uri, target.name(), target.getDestId(), checkinDate, checkoutDate);

            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseApiResponse(response.getBody(), target);
            }

            log.warn("Respuesta inesperada de RapidAPI Booking (HTTP {}). Usando fallback representativo.", response.getStatusCode());
            return generateSimulatedHotels(target);

        } catch (HttpStatusCodeException ex) {
            log.warn("Error HTTP al invocar RapidAPI Booking [{} - {}]: {}. Activando fallback representativo para continuar el proceso ETL.",
                    ex.getStatusCode(), ex.getStatusText(), ex.getResponseBodyAsString());
            return generateSimulatedHotels(target);

        } catch (ResourceAccessException ex) {
            log.warn("Timeout o falla de conectividad con RapidAPI Booking: {}. Activando fallback representativo.", ex.getMessage());
            return generateSimulatedHotels(target);

        } catch (Exception ex) {
            log.error("Excepción inesperada al invocar RapidAPI Booking: {}. Activando fallback representativo.", ex.getMessage(), ex);
            return generateSimulatedHotels(target);
        }
    }

    private URI buildRequestUri(String destId, String destType, LocalDate checkinDate, LocalDate checkoutDate) {
        String baseUrl = properties.getBaseUrl();
        String endpoint = properties.getEndpoint();
        String fullUrl = baseUrl.replaceAll("/+$", "") + "/" + endpoint.replaceAll("^/+", "");

        return UriComponentsBuilder.fromUriString(fullUrl)
                .queryParam("dest_id", destId)
                .queryParam("dest_type", destType != null ? destType : "city")
                .queryParam("checkin_date", checkinDate.toString())
                .queryParam("checkout_date", checkoutDate.toString())
                .queryParam("adults_number", "2")
                .queryParam("room_number", "1")
                .queryParam("units", "metric")
                .queryParam("order_by", "popularity")
                .queryParam("locale", "es")
                .build()
                .toUri();
    }

    private HttpHeaders buildRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-key", properties.getKey());
        headers.set("x-rapidapi-host", properties.getHost());
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private List<BookingHotelRawDto> parseApiResponse(String rawJson, GranPremioTarget target) {
        try {
            JsonNode rootNode = objectMapper.readTree(rawJson);
            List<BookingHotelRawDto> extracted = new ArrayList<>();

            if (rootNode.has("result") && rootNode.get("result").isArray()) {
                for (JsonNode node : rootNode.get("result")) {
                    extracted.add(objectMapper.treeToValue(node, BookingHotelRawDto.class));
                }
            }

            if (extracted.isEmpty() && rootNode.has("data") && rootNode.get("data").has("hotels") && rootNode.get("data").get("hotels").isArray()) {
                for (JsonNode node : rootNode.get("data").get("hotels")) {
                    extracted.add(objectMapper.treeToValue(node, BookingHotelRawDto.class));
                }
            }

            if (extracted.isEmpty() && rootNode.has("data") && rootNode.get("data").isArray()) {
                for (JsonNode node : rootNode.get("data")) {
                    extracted.add(objectMapper.treeToValue(node, BookingHotelRawDto.class));
                }
            }

            if (!extracted.isEmpty()) {
                log.info("Extracción exitosa desde RapidAPI: {} hoteles obtenidos para {}.", extracted.size(), target.name());
                return extracted;
            }

            RapidApiBookingResponseDto genericDto = objectMapper.readValue(rawJson, RapidApiBookingResponseDto.class);
            List<BookingHotelRawDto> fallback = genericDto.extractHotelsList();
            if (!fallback.isEmpty()) {
                return fallback;
            }

        } catch (Exception e) {
            log.error("Error al parsear el JSON de RapidAPI Booking: {}", e.getMessage(), e);
        }

        log.warn("No se encontraron hoteles en la respuesta JSON de RapidAPI. Empleando datos simulados para {}.", target.name());
        return generateSimulatedHotels(target);
    }

    public List<BookingHotelRawDto> generateSimulatedHotels(GranPremioTarget target) {
        List<BookingHotelRawDto> list = new ArrayList<>();
        String ciudad = target.getNombreCiudad();

        switch (target) {
            case MADRID -> {
                list.add(new BookingHotelRawDto(10101L, "Hotel Gran Meliá Palacio de los Duques", 5, 290.0, "https://images.unsplash.com/photo-1566073771259-6a8506099945"));
                list.add(new BookingHotelRawDto(10102L, "NH Collection Madrid Suecia", 4, 185.0, "https://images.unsplash.com/photo-1582719508461-905c673771fd"));
                list.add(new BookingHotelRawDto(10103L, "Barceló Torre de Madrid", 4, 160.0, "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa"));
            }
            case BAKU -> {
                list.add(new BookingHotelRawDto(20201L, "Four Seasons Hotel Baku", 5, 340.0, "https://images.unsplash.com/photo-1571896349842-33c89424de2d"));
                list.add(new BookingHotelRawDto(20202L, "Hilton Baku Seaside", 5, 210.0, "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4"));
                list.add(new BookingHotelRawDto(20203L, "JW Marriott Hotel Absheron Baku", 5, 260.0, "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb"));
            }
            case SINGAPUR -> {
                list.add(new BookingHotelRawDto(30301L, "Marina Bay Sands Hotel", 5, 520.0, "https://images.unsplash.com/photo-1566073771259-6a8506099945"));
                list.add(new BookingHotelRawDto(30302L, "The Ritz-Carlton Millenia Singapore", 5, 410.0, "https://images.unsplash.com/photo-1582719508461-905c673771fd"));
                list.add(new BookingHotelRawDto(30303L, "Pan Pacific Singapore", 4, 230.0, "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa"));
            }
            case AUSTIN -> {
                list.add(new BookingHotelRawDto(40401L, "The Driskill - The Unbound Collection", 5, 380.0, "https://images.unsplash.com/photo-1571896349842-33c89424de2d"));
                list.add(new BookingHotelRawDto(40402L, "Omni Austin Hotel Downtown", 4, 220.0, "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4"));
                list.add(new BookingHotelRawDto(40403L, "Fairmont Austin Grand Prix Suites", 4, 275.0, "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb"));
            }
            case CIUDAD_DE_MEXICO -> {
                list.add(new BookingHotelRawDto(50501L, "The St. Regis Mexico City", 5, 390.0, "https://images.unsplash.com/photo-1566073771259-6a8506099945"));
                list.add(new BookingHotelRawDto(50502L, "Gran Hotel de la Ciudad de México", 4, 195.0, "https://images.unsplash.com/photo-1582719508461-905c673771fd"));
                list.add(new BookingHotelRawDto(50503L, "Fiesta Americana Reforma Autódromo", 4, 145.0, "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa"));
            }
            case SAO_PAULO -> {
                list.add(new BookingHotelRawDto(60601L, "Palácio Tangará - Oetker Collection", 5, 430.0, "https://images.unsplash.com/photo-1571896349842-33c89424de2d"));
                list.add(new BookingHotelRawDto(60602L, "Hotel Fasano São Paulo Jardins", 5, 370.0, "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4"));
                list.add(new BookingHotelRawDto(60603L, "Grand Mercure São Paulo Interlagos", 4, 165.0, "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb"));
            }
            case LAS_VEGAS -> {
                list.add(new BookingHotelRawDto(70701L, "Bellagio Resort & F1 Strip View", 5, 490.0, "https://images.unsplash.com/photo-1566073771259-6a8506099945"));
                list.add(new BookingHotelRawDto(70702L, "The Venetian Resort Las Vegas", 5, 350.0, "https://images.unsplash.com/photo-1582719508461-905c673771fd"));
                list.add(new BookingHotelRawDto(70703L, "Caesars Palace Grand Prix Boulevard", 4, 280.0, "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa"));
            }
            case LUSAIL -> {
                list.add(new BookingHotelRawDto(80801L, "Raffles Doha & Lusail Circuit Resort", 5, 460.0, "https://images.unsplash.com/photo-1571896349842-33c89424de2d"));
                list.add(new BookingHotelRawDto(80802L, "The Ritz-Carlton Sharq Village", 5, 380.0, "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4"));
                list.add(new BookingHotelRawDto(80803L, "Waldorf Astoria Lusail Marina", 5, 320.0, "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb"));
            }
            case ABU_DABI -> {
                list.add(new BookingHotelRawDto(90901L, "W Abu Dhabi - Yas Island Trackside", 5, 580.0, "https://images.unsplash.com/photo-1566073771259-6a8506099945"));
                list.add(new BookingHotelRawDto(90902L, "The Yas Hotel Marina Views", 5, 420.0, "https://images.unsplash.com/photo-1582719508461-905c673771fd"));
                list.add(new BookingHotelRawDto(90903L, "Radisson Blu Hotel Yas Island", 4, 210.0, "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa"));
            }
            default -> {
                list.add(new BookingHotelRawDto(99901L, "Grand Prix Premium Hotel " + ciudad, 4, 180.0, "https://images.unsplash.com/photo-1566073771259-6a8506099945"));
                list.add(new BookingHotelRawDto(99902L, "Circuit View Suites " + ciudad, 5, 250.0, "https://images.unsplash.com/photo-1582719508461-905c673771fd"));
            }
        }
        return list;
    }
}


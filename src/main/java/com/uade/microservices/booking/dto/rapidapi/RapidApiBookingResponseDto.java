package com.uade.microservices.booking.dto.rapidapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * DTO genérico para capturar la respuesta compleja de la API de Booking en RapidAPI.
 * Es tolerante a las variaciones estructurales de los diferentes proveedores de RapidAPI
 * (como "result", "data.hotels", o "data" directo).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RapidApiBookingResponseDto {

    @JsonProperty("status")
    private Boolean status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("result")
    private List<BookingHotelRawDto> result;

    @JsonProperty("data")
    private JsonNode data;

    public RapidApiBookingResponseDto() {
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<BookingHotelRawDto> getResult() {
        return result;
    }

    public void setResult(List<BookingHotelRawDto> result) {
        this.result = result;
    }

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }

    /**
     * Extrae unificadamente la lista de hoteles crudos desde cualquier propiedad disponible
     * en el JSON devuelto por RapidAPI.
     */
    public List<BookingHotelRawDto> extractHotelsList() {
        if (result != null && !result.isEmpty()) {
            return result;
        }
        return new ArrayList<>();
    }
}


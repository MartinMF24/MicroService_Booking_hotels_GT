package com.uade.microservices.booking.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad JPA correspondiente a la tabla public.hoteles.
 */
@Entity
@Table(name = "hoteles")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_hotel", nullable = false, updatable = false)
    private UUID idHotel;

    @Column(name = "id_ciudad", nullable = false)
    private UUID idCiudad;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "estrellas")
    private Integer estrellas;

    @Column(name = "distancia_circuito_km", precision = 5, scale = 2)
    private BigDecimal distanciaCircuitoKm;

    @Column(name = "ofrece_traslado")
    private Boolean ofreceTraslado;

    @Column(name = "imagen_principal_url", columnDefinition = "text")
    private String imagenPrincipalUrl;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @OneToMany(
            mappedBy = "hotel",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<HabitacionHotel> habitaciones = new ArrayList<>();

    public Hotel() {
    }

    public Hotel(UUID idHotel, UUID idCiudad, String nombre, Integer estrellas,
                 BigDecimal distanciaCircuitoKm, Boolean ofreceTraslado,
                 String imagenPrincipalUrl, OffsetDateTime createdAt) {
        this.idHotel = idHotel;
        this.idCiudad = idCiudad;
        this.nombre = nombre;
        this.estrellas = estrellas;
        this.distanciaCircuitoKm = distanciaCircuitoKm;
        this.ofreceTraslado = ofreceTraslado != null ? ofreceTraslado : false;
        this.imagenPrincipalUrl = imagenPrincipalUrl;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.ofreceTraslado == null) {
            this.ofreceTraslado = false;
        }
    }

    public void addHabitacion(HabitacionHotel habitacion) {
        if (habitacion != null) {
            habitaciones.add(habitacion);
            habitacion.setHotel(this);
        }
    }

    public void removeHabitacion(HabitacionHotel habitacion) {
        if (habitacion != null) {
            habitaciones.remove(habitacion);
            habitacion.setHotel(null);
        }
    }

    public UUID getIdHotel() {
        return idHotel;
    }

    public void setIdHotel(UUID idHotel) {
        this.idHotel = idHotel;
    }

    public UUID getIdCiudad() {
        return idCiudad;
    }

    public void setIdCiudad(UUID idCiudad) {
        this.idCiudad = idCiudad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getEstrellas() {
        return estrellas;
    }

    public void setEstrellas(Integer estrellas) {
        this.estrellas = estrellas;
    }

    public BigDecimal getDistanciaCircuitoKm() {
        return distanciaCircuitoKm;
    }

    public void setDistanciaCircuitoKm(BigDecimal distanciaCircuitoKm) {
        this.distanciaCircuitoKm = distanciaCircuitoKm;
    }

    public Boolean getOfreceTraslado() {
        return ofreceTraslado;
    }

    public void setOfreceTraslado(Boolean ofreceTraslado) {
        this.ofreceTraslado = ofreceTraslado;
    }

    public String getImagenPrincipalUrl() {
        return imagenPrincipalUrl;
    }

    public void setImagenPrincipalUrl(String imagenPrincipalUrl) {
        this.imagenPrincipalUrl = imagenPrincipalUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<HabitacionHotel> getHabitaciones() {
        return habitaciones;
    }

    public void setHabitaciones(List<HabitacionHotel> habitaciones) {
        this.habitaciones = habitaciones;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hotel hotel)) return false;
        return Objects.equals(idHotel, hotel.idHotel);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idHotel);
    }
}


package com.uade.microservices.booking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad JPA correspondiente a la tabla public.habitaciones_hotel.
 */
@Entity
@Table(name = "habitaciones_hotel")
public class HabitacionHotel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_habitacion", nullable = false, updatable = false)
    private UUID idHabitacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_hotel", nullable = false)
    @JsonIgnore
    private Hotel hotel;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "precio_por_noche_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioPorNocheUsd;

    @Column(name = "stock_disponible", nullable = false)
    private Integer stockDisponible;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public HabitacionHotel() {
    }

    public HabitacionHotel(UUID idHabitacion, Hotel hotel, String tipo,
                           BigDecimal precioPorNocheUsd, Integer stockDisponible,
                           OffsetDateTime createdAt) {
        this.idHabitacion = idHabitacion;
        this.hotel = hotel;
        this.tipo = tipo;
        this.precioPorNocheUsd = precioPorNocheUsd;
        this.stockDisponible = stockDisponible != null ? stockDisponible : 0;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.stockDisponible == null) {
            this.stockDisponible = 0;
        }
    }

    public UUID getIdHabitacion() {
        return idHabitacion;
    }

    public void setIdHabitacion(UUID idHabitacion) {
        this.idHabitacion = idHabitacion;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public void setHotel(Hotel hotel) {
        this.hotel = hotel;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getPrecioPorNocheUsd() {
        return precioPorNocheUsd;
    }

    public void setPrecioPorNocheUsd(BigDecimal precioPorNocheUsd) {
        this.precioPorNocheUsd = precioPorNocheUsd;
    }

    public Integer getStockDisponible() {
        return stockDisponible;
    }

    public void setStockDisponible(Integer stockDisponible) {
        this.stockDisponible = stockDisponible;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HabitacionHotel that)) return false;
        return Objects.equals(idHabitacion, that.idHabitacion);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idHabitacion);
    }
}


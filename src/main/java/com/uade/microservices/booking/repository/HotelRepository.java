package com.uade.microservices.booking.repository;

import com.uade.microservices.booking.model.Hotel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, UUID> {

    Optional<Hotel> findByNombreIgnoreCaseAndIdCiudad(String nombre, UUID idCiudad);

    @EntityGraph(attributePaths = {"habitaciones"})
    @Query("SELECT h FROM Hotel h WHERE LOWER(h.nombre) = LOWER(:nombre) AND h.idCiudad = :idCiudad")
    Optional<Hotel> findWithHabitacionesByNombreIgnoreCaseAndIdCiudad(
            @Param("nombre") String nombre,
            @Param("idCiudad") UUID idCiudad
    );

    List<Hotel> findByIdCiudad(UUID idCiudad);

    @EntityGraph(attributePaths = {"habitaciones"})
    @Query("SELECT h FROM Hotel h WHERE h.idCiudad = :idCiudad")
    List<Hotel> findWithHabitacionesByIdCiudad(@Param("idCiudad") UUID idCiudad);
}


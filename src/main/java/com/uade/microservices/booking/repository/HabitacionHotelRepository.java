package com.uade.microservices.booking.repository;

import com.uade.microservices.booking.model.HabitacionHotel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HabitacionHotelRepository extends JpaRepository<HabitacionHotel, UUID> {

    List<HabitacionHotel> findByHotel_IdHotel(UUID idHotel);

    Optional<HabitacionHotel> findByHotel_IdHotelAndTipoIgnoreCase(UUID idHotel, String tipo);
}


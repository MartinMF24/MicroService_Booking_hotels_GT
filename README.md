# MicroService Booking Hotels GT

Microservicio REST desarrollado con **Java 25** y **Spring Boot 4.1.1** para la ingesta, transformación y persistencia (ETL) de alojamientos de **Booking.com (RapidAPI)** hacia una base de datos PostgreSQL alojada en Supabase, en el marco del proyecto Grand Prix Tracker.

---

## Características Principales

- **Arquitectura Package by Feature**: Modular, cohesiva y desacoplada bajo `com.uade.microservices.booking`.
- **Pipeline ETL Completo**:
  - **Extract**: Cliente HTTP con `RestTemplate` configurado con timeouts y headers RapidAPI (`BookingClientService`).
  - **Transform**: Normalización de esquemas, adaptación de tipos y cálculo de métricas de circuito (`BookingAdapter`).
  - **Load**: Persistencia atómica y operación **Upsert** idempotente sobre `hoteles` y `habitaciones_hotel` (`BookingSyncService`).
- **Ejecución Manual Exclusiva**: Control estricto a demanda mediante `POST /api/microservicios/sync-booking/{target}` (sin cron / `@Scheduled`).
- **Resiliencia Operativa**: Si no se provee API Key o se agota la cuota en RapidAPI, el servicio activa un generador de simulación representativo de alta fidelidad, asegurando continuidad de pruebas en cualquier ambiente.
- **Suite de Pruebas**: Cobertura con JUnit 5 y Mockito para todas las capas del microservicio.

---

## Punto de Entrada

```
src/main/java/com/uade/microservices/booking/BookingHotelsMicroserviceApplication.java
```

---

## Requisitos y Tecnologías

- **Java**: 25 (OpenJDK / Temurin)
- **Spring Boot**: 4.1.1
- **Maven**: 3.9+
- **PostgreSQL**: Supabase

---

## Ejecución del Microservicio

```bash
# Compilar y empaquetar
./mvnw clean package

# Ejecutar las pruebas
./mvnw test

# Iniciar la aplicación (corre por defecto en el puerto 8081)
./mvnw spring-boot:run
```

---

## Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/microservicios/sync-booking/all` | **Sincronización masiva**: Carga y sincroniza los 9 destinos de F1 simultáneamente en sus fechas de fin de semana |
| `POST` | `/api/microservicios/sync-booking/{target}` | Sincronización manual para un destino individual (ej: `SAO_PAULO`, `MADRID`, `BAKU`) |
| `GET` | `/api/microservicios/sync-booking/targets` | Lista los 9 destinos de Gran Premio con fechas de carrera, check-in (-1 día) y check-out (+3 días) |

> 📖 Ver guía técnica completa y documentación en [BOOKING_ETL.md](BOOKING_ETL.md).


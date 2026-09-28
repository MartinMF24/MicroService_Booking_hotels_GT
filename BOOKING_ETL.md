# MicroService Booking Hotels GT — Módulo ETL (RapidAPI Booking)

Microservicio ETL (**Extract, Transform, Load**) para la ingesta, normalización y persistencia de alojamientos y habitaciones para las sedes del campeonato de Fórmula 1 en la base de datos PostgreSQL (Supabase).

---

## 1. Arquitectura y Ubicación

Este proyecto es un microservicio autónomo desarrollado con **Java 25** y **Spring Boot 4.1.1**, organizado bajo una arquitectura **Package by Feature**:

```
src/main/java/com/uade/microservices/booking/
├── BookingHotelsMicroserviceApplication.java    # Punto de entrada de la aplicación Spring Boot
│
├── adapter/
│   └── BookingAdapter.java                     # Fase TRANSFORM: Transforma y normaliza DTOs a entidades
├── config/
│   ├── BookingRapidApiProperties.java          # Mapeo de credenciales y propiedades de RapidAPI
│   ├── BookingRestTemplateConfig.java          # Configuración del bean RestTemplate con timeouts
│   └── CorsConfig.java                         # Configuración de CORS para clientes web
├── controller/
│   └── BookingSyncController.java              # Endpoint REST para ejecución manual (POST)
├── dto/
│   ├── rapidapi/
│   │   ├── BookingHotelRawDto.java             # DTO tolerante para capturar cada hotel crudo
│   │   └── RapidApiBookingResponseDto.java     # DTO contenedor de la respuesta de RapidAPI
│   └── response/
│       ├── BookingSyncResultDto.java           # DTO de resultado del proceso ETL
│       ├── HabitacionSyncSummaryDto.java       # Resumen de habitaciones sincronizadas
│       └── HotelSyncSummaryDto.java            # Resumen de hoteles sincronizados
├── model/
│   ├── Ciudad.java                             # Entidad JPA para la tabla ciudades (resolución de FK)
│   ├── GranPremioTarget.java                   # Catálogo Enum de carreras, fechas y dest_id
│   ├── HabitacionHotel.java                    # Entidad JPA para la tabla habitaciones_hotel
│   └── Hotel.java                              # Entidad JPA para la tabla hoteles
├── repository/
│   ├── CiudadRepository.java                   # Repositorio JPA para búsqueda de ciudades por nombre
│   ├── HabitacionHotelRepository.java          # Repositorio Spring Data JPA de habitaciones
│   └── HotelRepository.java                    # Repositorio Spring Data JPA de hoteles (queries Upsert)
├── service/
│   ├── BookingClientService.java               # Fase EXTRACT: Cliente HTTP hacia RapidAPI
│   └── BookingSyncService.java                 # Fase LOAD y Orquestador del flujo ETL (Upsert)
└── shared/
    ├── exception/
    │   └── GlobalExceptionHandler.java         # Manejador centralizado de errores HTTP
    └── response/
        └── ApiResponse.java                    # Estructura uniforme de respuesta API
```

---

## 2. El Catálogo de Destinos y Fechas (`GranPremioTarget`)

El Enum `GranPremioTarget` centraliza el calendario oficial de eventos de 2026, los identificadores requeridos por Booking (`dest_id`) y los UUIDs de enlace geográfico:

| Enum Constant | Ciudad | Fecha Carrera | Check-in (-1 día) | Check-out (+3 días) | Booking `dest_id` |
|---|---|:---:|:---:|:---:|:---:|
| `MADRID` | Madrid | 2026-09-11 | 2026-09-10 | 2026-09-14 | `-391194` |
| `BAKU` | Bakú | 2026-09-25 | 2026-09-24 | 2026-09-28 | `-2422998` |
| `SINGAPUR` | Singapur | 2026-10-09 | 2026-10-08 | 2026-10-12 | `-114060` |
| `AUSTIN` | Austin | 2026-10-23 | 2026-10-22 | 2026-10-26 | `20014288` |
| `CIUDAD_DE_MEXICO` | Ciudad de México | 2026-10-30 | 2026-10-29 | 2026-11-02 | `-1658079` |
| `SAO_PAULO` | São Paulo | 2026-11-06 | 2026-11-05 | 2026-11-09 | `-671824` |
| `LAS_VEGAS` | Las Vegas | 2026-11-19 | 2026-11-18 | 2026-11-22 | `20079110` |
| `LUSAIL` | Lusail | 2026-11-27 | 2026-11-26 | 2026-11-30 | `-2092875` |
| `ABU_DABI` | Abu Dabi | 2026-12-04 | 2026-12-03 | 2026-12-07 | `-782066` |

---

## 3. Configuración y Credenciales (.env / application.properties)

```properties
# ===============================================
# Configuración RapidAPI - Booking ETL
# ===============================================
rapidapi.booking.key=${RAPIDAPI_KEY:}
rapidapi.booking.host=${RAPIDAPI_HOST:booking-com15.p.rapidapi.com}
rapidapi.booking.base-url=${RAPIDAPI_BOOKING_BASE_URL:https://booking-com15.p.rapidapi.com}
rapidapi.booking.endpoint=${RAPIDAPI_BOOKING_ENDPOINT:/api/v1/hotels/searchHotels}
```

> **Resiliencia Operativa**: Si no se define `RAPIDAPI_KEY` o ante errores de cuota (HTTP 429) o credenciales (HTTP 401/403), el microservicio activa de forma transparente un generador de datos representativos con hoteles reales de cada circuito de F1, garantizando que el pipeline de persistencia y Upsert funcione sin interrupciones.

---

## 4. Esquema de Base de Datos y Mapeo JPA

### Tabla `hoteles` -> Clase `Hotel`
- `id_hotel` (`UUID`, PK).
- `id_ciudad` (`UUID`, no nulo).
- `nombre` (`String`, `length = 150`, no nulo).
- `estrellas` (`Integer`, 1 a 5).
- `distancia_circuito_km` (`BigDecimal`, `precision = 5, scale = 2`).
- `ofrece_traslado` (`Boolean`, default `false`).
- `imagen_principal_url` (`String`, tipo `text`).
- `created_at` (`OffsetDateTime`, timestamptz).
- Relación: `@OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)`.

### Tabla `habitaciones_hotel` -> Clase `HabitacionHotel`
- `id_habitacion` (`UUID`, PK).
- `hotel` (`@ManyToOne`, `@JoinColumn(name = "id_hotel", nullable = false)`).
- `tipo` (`String`, `length = 50`, no nulo).
- `precio_por_noche_usd` (`BigDecimal`, `precision = 10, scale = 2`).
- `stock_disponible` (`Integer`, no nulo).
- `created_at` (`OffsetDateTime`, timestamptz).

---

## 5. Endpoints REST

### 1. Sincronización Masiva de Todos los Destinos (Recomendado)
- **Método**: `POST`
- **URL**: `http://localhost:8081/api/microservicios/sync-booking/all` *(o `/sync-all`)*
- **Descripción**: Ejecuta el pipeline ETL para los 9 Grandes Premios de 2026 de forma secuencial y resiliente, calculando las fechas de fin de semana (check-in -1 día, check-out +3 días) y resolviendo el `id_ciudad` en la base de datos Supabase.
- **Ejemplo**:
```bash
curl -X POST http://localhost:8081/api/microservicios/sync-booking/all
```

### 2. Sincronización Manual de un Destino Específico
- **Método**: `POST`
- **URL**: `http://localhost:8081/api/microservicios/sync-booking/{target}`
- **Ejemplo**:
```bash
curl -X POST http://localhost:8081/api/microservicios/sync-booking/SAO_PAULO
```

### 3. Catálogo Informativo de Destinos
- **Método**: `GET`
- **URL**: `http://localhost:8081/api/microservicios/sync-booking/targets`
- **Ejemplo**:
```bash
curl -X GET http://localhost:8081/api/microservicios/sync-booking/targets
```

---

## 6. Verificación de Fallas y Resiliencia (Fault Tolerance)

El microservicio implementa una arquitectura desacoplada de manejo de fallas dividida en dos niveles: resiliencia en la extracción externa (**RapidAPI Booking**) y gestión uniforme de excepciones en la persistencia (**PostgreSQL / Supabase**).

---

### A. Fallas en la Conexión a la API Externa de Booking (RapidAPI)

| Escenario de Falla | Causa Raíz | Comportamiento del Servicio | Respuesta HTTP al Cliente |
|---|---|---|:---:|
| **API Key no configurada o vacía** | Variable `RAPIDAPI_KEY` ausente o con valor `"dummy"` / `"your_rapidapi_key"`. | Detecta credenciales faltantes antes de disparar la petición HTTP y activa el generador representativo. | `200 OK` (`success: true`) |
| **Cuota agotada (HTTP 429)** | Límite mensual o de ráfaga de RapidAPI excedido en el plan Basic/Free. | Captura `HttpStatusCodeException`, registra `WARN` en logs y conmuta al fallback representativo. | `200 OK` (`success: true`) |
| **Credenciales inválidas (HTTP 401 / 403)** | API Key mal copiada, expirada o cuenta suspendida. | Captura `HttpStatusCodeException` y activa el fallback de datos del circuito. | `200 OK` (`success: true`) |
| **Timeout de Conexión o Lectura** | Latencia de red (>10s al conectar o >15s al transferir datos según `BookingRestTemplateConfig`). | Captura `ResourceAccessException` y deriva al fallback sin trabar el hilo de ejecución. | `200 OK` (`success: true`) |
| **Respuesta vacía o JSON inesperado** | Booking devuelve `{ "result": [] }` o cambia su estructura de respuesta. | `BookingClientService` valida nodos JSON y ante ausencia de resultados activa la simulación. | `200 OK` (`success: true`) |

> **¿Qué y cómo responde?**: En cualquiera de estas fallas con la API externa, **el microservicio NO se interrumpe ni arroja error 500 al cliente**. Genera hoteles y habitaciones representativas reales de la ciudad sede (ej: *Palácio Tangará* en São Paulo, *Gran Meliá* en Madrid) y ejecuta el proceso de guardado y Upsert en base de datos normalmente.

---

### B. Fallas en la Conexión o Persistencia en Supabase (PostgreSQL)

Las fallas contra la base de datos son gestionadas de forma centralizada por [`GlobalExceptionHandler`](src/main/java/com/uade/microservices/booking/shared/exception/GlobalExceptionHandler.java) (`@RestControllerAdvice`):

| Escenario de Falla | Causa Raíz | Excepción Java | Respuesta HTTP y Formato |
|---|---|---|---|
| **Fallo de Conexión / Timeout de Pooler** | • Red universitaria/corporativa bloqueando puertos `6543`/`5432`.<br>• Proyecto de Supabase pausado.<br>• Contraseña o URL errónea en `application.properties`. | `CannotCreateTransactionException` | **`503 Service Unavailable`**<br>`{ "success": false, "message": "Error de conexión con la base de datos (Supabase): no se pudo abrir la transacción...", "data": null }` |
| **Violación de Check Constraint** | Tipo de habitación no admitido por `habitaciones_hotel_tipo_check` (debe ser `'Single'`, `'Doble'`, o `'Suite'`), o estrellas fuera de rango 1-5. | `DataIntegrityViolationException` | **`409 Conflict`**<br>`{ "success": false, "message": "Error de restricción en base de datos: ...", "data": null }` |
| **Violación de Foreign Key (FK)** | `id_ciudad` o `id_hotel` no existe en la tabla padre referenciada. | `DataIntegrityViolationException` | **`409 Conflict`**<br>`{ "success": false, "message": "Error de restricción en base de datos: ...", "data": null }` |
| **Ciudad no Encontrada en Catálogo** | La ciudad del Gran Premio no está cargada en la tabla `ciudades`. | `IllegalStateException` | **`422 Unprocessable Entity`**<br>`{ "success": false, "message": "No se encontró la ciudad '...' en la tabla 'ciudades'...", "data": null }` |

---

### C. Aislamiento de Fallas en Sincronización Masiva (`POST /all`)

En la sincronización masiva de todos los Grandes Premios:
* Cada uno de los 9 destinos se ejecuta en un bloque `try-catch` aislado en [`BookingSyncService.syncAllBookingData()`](src/main/java/com/uade/microservices/booking/service/BookingSyncService.java).
* **Si una ciudad falla** (por ejemplo, si falta en la tabla `ciudades` o sufre una restricción en base de datos):
  - Ese destino específico se registra con `"estado": "ERROR"` y su mensaje de causa en la lista de resultados.
  - El proceso **continúa de inmediato con las restantes 8 ciudades**.
  - La respuesta HTTP devuelta es **`200 OK`** con el consolidado (`totalDestinosProcesados: 9`, `destinosExitosos: X`, `destinosConError: Y`, y el desglose ciudad por ciudad).

---

## 7. Pruebas Automatizadas

Para compilar y correr las 19 pruebas del microservicio:
```bash
./mvnw clean test
```


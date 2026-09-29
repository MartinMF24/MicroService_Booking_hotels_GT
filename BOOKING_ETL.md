# MicroService Booking Hotels GT — Módulo ETL (RapidAPI Booking & Supabase)

Microservicio ETL (**Extract, Transform, Load**) para la ingesta, normalización y persistencia de alojamientos y habitaciones hoteleras correspondientes a las sedes de los Grandes Premios de Fórmula 1 en la base de datos PostgreSQL (Supabase).

---

## 1. Propósito y Funcionalidad del Microservicio

### 1.1 Objetivo del Servicio
El propósito principal de este microservicio es extraer datos reales de hoteles y habitaciones desde Booking.com (vía RapidAPI) para las ciudades donde se disputan las carreras de Fórmula 1, normalizarlos bajo las reglas de negocio de la plataforma y persistirlos de forma consistente en la base de datos de Supabase. De este modo, la información queda disponible para que la aplicación principal (*Grand Prix Tracker*) la consulte directamente con tiempos de respuesta óptimos.

### 1.2 Justificación y Beneficios para la Aplicación
La aplicación cliente no consulta en tiempo real a la API de Booking por cada búsqueda de los usuarios por las siguientes razones:
* **Control de Costos y Cuotas**: Las APIs externas de búsqueda hotelera imponen límites estrictos de consumo. Si miles de usuarios consultaran directamente a Booking, la cuota se agotaría rápidamente, provocando bloqueos o sobrecostos.
* **Velocidad y Experiencia de Usuario**: Una consulta a una API externa suele tardar entre 1.5 y 4 segundos, mientras que consultar los datos ya almacenados y optimizados en la base de datos propia toma menos de 50 milisegundos.
* **Homogeneidad de Datos**: Los datos devueltos por proveedores externos son variados y no estructurados. El microservicio se encarga de estandarizar las monedas a USD, calcular distancias coherentes a los circuitos de carrera, categorizar las habitaciones en tipos concretos y acotar las calificaciones.
* **Disponibilidad Continua**: Al contar con los datos almacenados localmente en la base de datos, la aplicación sigue funcionando con normalidad incluso ante interrupciones o caídas temporales del proveedor externo de hoteles.

### 1.3 Modo de Ejecución Bajo Demanda (On-Demand)
El microservicio funciona exclusivamente bajo demanda:
* Se ejecuta mediante peticiones REST cuando se requiere actualizar un destino en particular o todos los Grandes Premios de la temporada de manera masiva.
* No cuenta con tareas automáticas programadas en segundo plano para evitar consumos de red o de cuota innecesarios.

---

## 2. Patrones de Diseño y Arquitectura Utilizados

Para garantizar robustez, mantenibilidad y resiliencia, el microservicio implementa los siguientes patrones de software:

### 2.1 Patrón ETL (Extract, Transform, Load)
Estructura el flujo de procesamiento de datos en tres etapas secuenciales e independientes:
* **Extracción (Extract)**: Consulta a la API externa de hoteles enviando los parámetros de destino geográfico, las fechas calculadas de check-in y check-out, cantidad de huéspedes y moneda, capturando la respuesta cruda.
* **Transformación (Transform)**: Toma los datos sin procesar y los convierte al modelo de negocio de la plataforma:
  - Limpia y recorta los nombres a longitudes permitidas por la base de datos.
  - Normaliza la cantidad de estrellas en una escala estándar de 1 a 5.
  - Calcula la distancia al autódromo de la ciudad y determina la disponibilidad de servicio de traslado.
  - Genera automáticamente las habitaciones canónicas con precios calculados y disponibilidad de stock.
* **Carga (Load)**: Resuelve dinámicamente la ciudad asociada en la base de datos y almacena los hoteles y sus habitaciones dentro de una transacción atómica, garantizando que no se guarden datos a medias.

### 2.2 Patrón Adaptador (Adapter Pattern)
Actúa como una capa intermedia entre la estructura cambiante del proveedor externo y el modelo de datos propio de la plataforma:
* Los nombres de los campos en las APIs externas pueden cambiar o venir con formatos diversos (por ejemplo, nombres bajo claves distintas o puntuaciones en escalas no estándar).
* El adaptador absorbe estas diferencias y produce siempre datos homogéneos acordes al modelo interno, protegiendo a la base de datos y al resto del sistema de modificaciones externas.

### 2.3 Patrón Upsert (Inserción o Actualización Idempotente)
Garantiza que el proceso pueda ejecutarse múltiples veces sobre la misma ciudad sin generar registros duplicados:
* Antes de guardar, el sistema comprueba si el hotel ya existe en la ciudad indicada.
* **Si ya existe**: Actualiza sus características (estrellas, distancia al circuito, foto y traslado) y refresca las tarifas y stock de sus habitaciones.
* **Si es nuevo**: Lo inserta como un nuevo registro junto con todas sus habitaciones asociadas.
* Esto proporciona **idempotencia total**: ejecutar la sincronización una vez o diez veces deja la base de datos en un estado consistente y sin duplicaciones.

### 2.4 Patrón Fallback de Resiliencia / Degradación Elegante (Graceful Degradation)
Protege la disponibilidad operativa del microservicio frente a eventualidades con el servicio externo:
* Si la clave de acceso a la API externa no está configurada, si se excede la cuota de peticiones permitidas, si hay fallas de autenticación o si ocurre una caída de red o timeout, el microservicio no detiene su ejecución ni devuelve un error al cliente.
* En su lugar, activa de forma transparente un catálogo de alojamientos reales y representativos para cada circuito, permitiendo que la fase de persistencia y actualización en la base de datos continúe con total normalidad.

### 2.5 Patrón Repositorio (Repository Pattern)
Separa completamente la lógica de negocio y de transformación de la interacción directa con la base de datos:
* Provee métodos específicos para consultar, verificar existencia y persistir hoteles y habitaciones.
* Optimiza las consultas para traer las entidades relacionadas de manera eficiente en una única operación de lectura.

### 2.6 Patrón Objeto de Transferencia de Datos (DTO - Data Transfer Object)
Aísla las diferentes capas del sistema mediante estructuras de datos dedicadas:
* Los datos crudos recibidos desde la red se encapsulan en estructuras transitorias.
* Las entidades persistentes quedan confinadas al dominio y a la base de datos.
* Las respuestas hacia el cliente se formatean en objetos diseñados exclusivamente para reportar resúmenes, estadísticas y estados del proceso.

### 2.7 Patrón Manejo Centralizado de Excepciones
Unifica la captura y tratamiento de cualquier error o contingencia que ocurra durante la ejecución:
* Traduce fallas técnicas de bajo nivel (problemas de red con la base de datos, violaciones de restricciones o parámetros incorrectos) en respuestas HTTP claras y amigables.
* Asegura que el cliente siempre reciba un formato de respuesta predecible y estandarizado, sin exponer trazas técnicas internas.

---

## 3. Catálogo de Destinos y Fechas de Gran Premio

El microservicio contempla las sedes oficiales del calendario de Fórmula 1 para las temporadas 2026 y 2027:

| Gran Premio / Destino | Ciudad Sede | Temporada | Fecha Carrera | Check-in (-1 día) | Check-out (+3 días) | Booking `dest_id` |
|---|---|:---:|:---:|:---:|:---:|:---:|
| `MADRID` | Madrid | 2026 | 2026-09-11 | 2026-09-10 | 2026-09-14 | `-391194` |
| `BAKU` | Bakú | 2026 | 2026-09-25 | 2026-09-24 | 2026-09-28 | `-2422998` |
| `SINGAPUR` | Singapur | 2026 | 2026-10-09 | 2026-10-08 | 2026-10-12 | `-114060` |
| `AUSTIN` | Austin | 2026 | 2026-10-23 | 2026-10-22 | 2026-10-26 | `20014288` |
| `CIUDAD_DE_MEXICO` | Ciudad de México | 2026 | 2026-10-30 | 2026-10-29 | 2026-11-02 | `-1658079` |
| `SAO_PAULO` | São Paulo | 2026 | 2026-11-06 | 2026-11-05 | 2026-11-09 | `-671824` |
| `LAS_VEGAS` | Las Vegas | 2026 | 2026-11-19 | 2026-11-18 | 2026-11-22 | `20079110` |
| `LUSAIL` | Lusail | 2026 | 2026-11-27 | 2026-11-26 | 2026-11-30 | `-2092875` |
| `ABU_DABI` | Abu Dabi | 2026 | 2026-12-04 | 2026-12-03 | 2026-12-07 | `-782066` |
| `SAKHIR` | Sakhir | 2027 | 2027-03-12 | 2027-03-11 | 2027-03-15 | `-784871` |
| `YEDA` | Yeda | 2027 | 2027-03-19 | 2027-03-18 | 2027-03-22 | `-3096108` |
| `MELBOURNE` | Melbourne | 2027 | 2027-04-02 | 2027-04-01 | 2027-04-05 | `-1586844` |
| `SUZUKA` | Suzuka | 2027 | 2027-04-09 | 2027-04-08 | 2027-04-12 | `-244616` |
| `SHANGHAI` | Shanghái | 2027 | 2027-04-16 | 2027-04-15 | 2027-04-19 | `-1924465` |
| `MIAMI` | Miami | 2027 | 2027-04-30 | 2027-04-29 | 2027-05-03 | `20023181` |
| `MONTREAL` | Montreal | 2027 | 2027-05-21 | 2027-05-20 | 2027-05-24 | `-569541` |
| `MONTECARLO` | Montecarlo | 2027 | 2027-06-04 | 2027-06-03 | 2027-06-07 | `-1451964` |
| `PORTIMAO` | Portimão | 2027 | 2027-06-18 | 2027-06-17 | 2027-06-21 | `-2173080` |
| `SILVERSTONE` | Silverstone | 2027 | 2027-07-02 | 2027-07-01 | 2027-07-05 | `-2607823` |

---

## 4. Estructura de Datos en Base de Datos (Supabase)

El microservicio interactúa con tres tablas relacionales:

1. **Tabla `ciudades`**:
   - Almacena el identificador único (`id_ciudad`) y el `nombre` de las ciudades sede. Es la tabla de referencia obligatoria para vincular cada hotel a su ubicación geográfica.
2. **Tabla `hoteles`**:
   - `id_hotel`: Identificador único (UUID) del alojamiento.
   - `id_ciudad`: Clave foránea que referencia a la tabla `ciudades`.
   - `nombre`: Nombre comercial del hotel (longitud máxima de 150 caracteres).
   - `estrellas`: Calificación del hotel (valor entero entre 1 y 5).
   - `distancia_circuito_km`: Distancia numérica aproximada al autódromo.
   - `ofrece_traslado`: Indicador booleano de disponibilidad de transporte al evento.
   - `imagen_principal_url`: Enlace público a la fotografía principal del hotel.
   - `created_at`: Marca temporal de creación.
3. **Tabla `habitaciones_hotel`**:
   - `id_habitacion`: Identificador único (UUID) de la habitación.
   - `id_hotel`: Clave foránea que referencia al hotel al que pertenece (con eliminación en cascada).
   - `tipo`: Categoría de la habitación (restringida por restricción de base de datos a `'Single'`, `'Doble'` o `'Suite'`).
   - `precio_por_noche_usd`: Tarifa diaria en dólares.
   - `stock_disponible`: Unidades disponibles para reserva.
   - `created_at`: Marca temporal de creación.

---

## 5. Contrato de la API REST

### 5.1 ¿Qué Necesitamos? (Requerimientos de Entrada)

Los endpoints de sincronización no requieren enviar un cuerpo JSON de entrada (`Request Body` vacío). Los parámetros necesarios se resuelven a través de la URL y los encabezados:

| Parámetro | Tipo | Requerido | Descripción |
|---|:---:|:---:|---|
| **Método HTTP** | Verbo HTTP | Sí | `POST` para ejecutar procesos de sincronización; `GET` para consultar el catálogo. |
| **Header `Accept`** | Header | Opcional | `application/json` (formato esperado de respuesta). |
| **Header `Content-Type`** | Header | Opcional | `application/json`. |
| **`target` (Ruta)** | Path Variable | Solo en endpoint individual | Nombre del Gran Premio a sincronizar (ej. `SAO_PAULO`, `MADRID`, `BAKU`, etc.). Es insensible a mayúsculas, minúsculas o guiones. |
| **Body** | JSON | No | No se requiere cuerpo en la petición (viaja vacío). |

---

### 5.2 ¿Cómo nos lo Brinda? (Estructura de Salida)

Todas las respuestas del microservicio utilizan un envoltorio estándar uniforme:

```json
{
  "success": true,
  "message": "Descripción clara del resultado de la operación",
  "data": { ... }
}
```

* `success` (`boolean`): Indica si la operación concluyó exitosamente (`true`) o si ocurrió algún impedimento (`false`).
* `message` (`string`): Mensaje explicativo con el resumen de la acción realizada o la causa del error.
* `data` (`object` o `array` o `null`): Contiene los datos procesados en caso de éxito, o `null` si la operación falló.

---

### 5.3 Especificación de Endpoints

#### 1. Sincronización Masiva de Todos los Grandes Premios (Recomendado)
* **Método**: `POST`
* **Ruta**: `/api/microservicios/sync-booking/all` *(o `/sync-all`)*
* **Qué hace**: Ejecuta la sincronización de las 19 ciudades del calendario en forma secuencial y aislada.
* **Qué devuelve**:
  - `totalDestinosProcesados`: Total de ciudades evaluadas (19).
  - `destinosExitosos`: Cantidad de ciudades sincronizadas con éxito.
  - `destinosConError`: Cantidad de ciudades que tuvieron algún error.
  - `totalHotelesExtraidos`: Suma total de hoteles procesados.
  - `totalHotelesCreados`: Nuevos hoteles guardados en la base de datos.
  - `totalHotelesActualizados`: Hoteles existentes cuyos datos se actualizaron.
  - `totalHabitacionesCreadas`: Nuevas habitaciones insertadas.
  - `totalHabitacionesActualizadas`: Habitaciones con tarifas y stock refrescados.
  - `resultados`: Lista completa con el detalle de cada Gran Premio procesado.

*Ejemplo de Petición*:
```bash
curl -X POST http://localhost:8081/api/microservicios/sync-booking/all
```

*Ejemplo de Respuesta Exitosa (`200 OK`)*:
```json
{
  "success": true,
  "message": "Sincronización masiva completada: 19 destinos exitosos de 19 procesados",
  "data": {
    "totalDestinosProcesados": 19,
    "destinosExitosos": 19,
    "destinosConError": 0,
    "totalHotelesExtraidos": 190,
    "totalHotelesCreados": 190,
    "totalHotelesActualizados": 0,
    "totalHabitacionesCreadas": 475,
    "totalHabitacionesActualizadas": 0,
    "timestamp": "2026-09-29T16:45:00.123456Z",
    "resultados": [
      {
        "target": "MADRID",
        "nombreCiudad": "Madrid",
        "fechaCarrera": "2026-09-11",
        "checkinDate": "2026-09-10",
        "checkoutDate": "2026-09-14",
        "destId": "-391194",
        "totalHotelesExtraidos": 10,
        "hotelesCreados": 10,
        "hotelesActualizados": 0,
        "habitacionesCreadas": 25,
        "habitacionesActualizadas": 0,
        "estado": "SUCCESS",
        "mensaje": "Sincronización ETL de Booking para Madrid completada con éxito.",
        "timestamp": "2026-09-29T16:45:02.123456Z",
        "hotelesSincronizados": [
          {
            "idHotel": "b2f6b865-c49b-4375-9c60-c44d18ec042e",
            "idCiudad": "34053323-6e73-47d9-b306-b4dc8df920cb",
            "nombre": "Gran Meliá Palacio de los Duques",
            "estrellas": 5,
            "distanciaCircuitoKm": 14.50,
            "ofreceTraslado": true,
            "imagenPrincipalUrl": "https://cf.bstatic.com/xdata/images/hotel/max1024x768/hotel_madrid.jpg",
            "habitaciones": [
              {
                "idHabitacion": "65b53e4b-a91d-4074-a698-fafe91a54109",
                "tipo": "Single",
                "precioPorNocheUsd": 380.00,
                "stockDisponible": 12
              },
              {
                "idHabitacion": "79b43e1c-529a-4123-b129-feae91a54210",
                "tipo": "Doble",
                "precioPorNocheUsd": 513.00,
                "stockDisponible": 6
              },
              {
                "idHabitacion": "89c54e2d-639b-4234-c230-aeae91a54321",
                "tipo": "Suite",
                "precioPorNocheUsd": 684.00,
                "stockDisponible": 3
              }
            ]
          }
        ]
      }
    ]
  }
}
```

---

#### 2. Sincronización Manual de un Destino Específico
* **Método**: `POST`
* **Ruta**: `/api/microservicios/sync-booking/{target}`
* **Qué hace**: Ejecuta el proceso exclusivamente para la sede indicada en `{target}`.
* **Qué devuelve**: Objeto de resultado con los totales procesados para esa ciudad y el listado de hoteles y habitaciones guardados.

*Ejemplo de Petición*:
```bash
curl -X POST http://localhost:8081/api/microservicios/sync-booking/SAO_PAULO
```

*Ejemplo de Respuesta Exitosa (`200 OK`)*:
```json
{
  "success": true,
  "message": "Sincronización ETL de Booking ejecutada correctamente para São Paulo",
  "data": {
    "target": "SAO_PAULO",
    "nombreCiudad": "São Paulo",
    "fechaCarrera": "2026-11-06",
    "checkinDate": "2026-11-05",
    "checkoutDate": "2026-11-09",
    "destId": "-671824",
    "totalHotelesExtraidos": 10,
    "hotelesCreados": 10,
    "hotelesActualizados": 0,
    "habitacionesCreadas": 26,
    "habitacionesActualizadas": 0,
    "estado": "SUCCESS",
    "mensaje": "Sincronización ETL de Booking para São Paulo completada con éxito. Hoteles creados: 10, actualizados: 0. Habitaciones creadas: 26, actualizadas: 0.",
    "timestamp": "2026-09-29T16:50:11.890123Z",
    "hotelesSincronizados": [
      {
        "idHotel": "ae44461c-8450-444b-9464-ccbe1838d39b",
        "idCiudad": "4501eeb9-010b-468a-9024-4343f698cf58",
        "nombre": "Palácio Tangará - an Oetker Collection Hotel",
        "estrellas": 5,
        "distanciaCircuitoKm": 8.70,
        "ofreceTraslado": true,
        "imagenPrincipalUrl": "https://cf.bstatic.com/xdata/images/hotel/max1024x768/hotel_sp.jpg",
        "habitaciones": [
          {
            "idHabitacion": "44e33e8b-2646-4199-9d96-adc08ef5944b",
            "tipo": "Single",
            "precioPorNocheUsd": 430.00,
            "stockDisponible": 14
          },
          {
            "idHabitacion": "55f44f9c-3757-4200-a0e7-bec09fe6055c",
            "tipo": "Doble",
            "precioPorNocheUsd": 580.50,
            "stockDisponible": 7
          },
          {
            "idHabitacion": "66a55a0d-4868-4311-b1f8-cfa10af7166d",
            "tipo": "Suite",
            "precioPorNocheUsd": 774.00,
            "stockDisponible": 3
          }
        ]
      }
    ]
  }
}
```

---

#### 3. Catálogo Informativo de Destinos
* **Método**: `GET`
* **Ruta**: `/api/microservicios/sync-booking/targets`
* **Qué hace**: Lista todos los destinos disponibles, con sus fechas oficiales, fechas de estancia calculadas e identificadores de búsqueda.

*Ejemplo de Petición*:
```bash
curl -X GET http://localhost:8081/api/microservicios/sync-booking/targets
```

---

### 5.4 Códigos de Estado HTTP

| Código HTTP | Significado | Causa Principal | Formato Devuelto |
|---|---|---|---|
| **`200 OK`** | Operación Exitosa | El proceso finalizó con normalidad (incluyendo casos resueltos por fallback). | `ApiResponse` con `success: true` |
| **`400 Bad Request`** | Parámetro Inválido | El nombre del Gran Premio enviado en la URL no existe en el catálogo. | `ApiResponse` con `success: false` y mensaje de error |
| **`409 Conflict`** | Violación de Restricción | Se intentó guardar un dato que no cumple las restricciones de la base de datos. | `ApiResponse` con `success: false` y detalle de restricción |
| **`422 Unprocessable Entity`** | Ciudad no Encontrada | La ciudad del Gran Premio no está registrada en la tabla `ciudades` de Supabase. | `ApiResponse` con `success: false` y mensaje aclaratorio |
| **`503 Service Unavailable`** | Base de Datos no Disponible | No se pudo establecer conexión o abrir la transacción contra Supabase. | `ApiResponse` con `success: false` indicando problemas de red/pooler |
| **`500 Internal Server Error`** | Error Inesperado | Excepción no contemplada. | `ApiResponse` con `success: false` |

---

### 5.5 Reglas de Negocio del Servicio

1. **Regla de Estadía Extendida de Fin de Semana**:
   - **Check-in**: Se calcula fijando la llegada **1 día antes** de la fecha de la carrera (permitiendo presenciar los entrenamientos libres y la clasificación).
   - **Check-out**: Se calcula fijando la salida **3 días después** de la carrera (cubriendo la noche de carrera del domingo y permitiendo el regreso el lunes por la mañana).
2. **Regla de Tipos Canónicos de Habitación**:
   - Las habitaciones generadas deben pertenecer de manera estricta al conjunto admitido por la base de datos: `'Single'`, `'Doble'` o `'Suite'`.
   - La habitación *Single* toma el precio base.
   - La habitación *Doble* aplica un 35% de incremento sobre el precio base.
   - La habitación *Suite* aplica un 80% de incremento y solo se habilita para hoteles de 4 o 5 estrellas.
3. **Regla de Rango de Estrellas**:
   - La cantidad de estrellas se normaliza obligatoriamente en un rango de **1 a 5**.
4. **Regla de Resolución Dinámica de Ciudad**:
   - Antes de guardar un hotel, el microservicio consulta en la tabla `ciudades` de Supabase para obtener el `id_ciudad` real.
   - Realiza la búsqueda contemplando nombres exactos, variantes sin acentos y alias frecuentes.
   - Si la ciudad no existe en la base de datos, el servicio no guarda hoteles huérfanos y rechaza la operación con código HTTP 422.
5. **Regla de Idempotencia**:
   - La re-ejecución del proceso no duplica hoteles ni habitaciones en la base de datos; actualiza los valores existentes si ya se encontraban registrados.
6. **Regla de Operación Bajo Demanda**:
   - El servicio se ejecuta únicamente cuando se invoca a través de sus endpoints REST, garantizando control sobre el consumo de peticiones externas.

---

## 6. Verificación de Fallas y Resiliencia (Fault Tolerance)

El microservicio contempla un manejo diferenciado de fallas según el componente involucrado:

### 6.1 Fallas con la API Externa de Booking
* **Escenarios**: Cuota de peticiones agotada (HTTP 429), clave de API no configurada o expirada (HTTP 401/403), caídas del proveedor o tiempos de espera agotados (timeouts de red).
* **Comportamiento**: El microservicio **no falla ni devuelve un error al cliente**. Activa inmediatamente un catálogo de hoteles reales representativos para la ciudad sede y prosigue con la carga en la base de datos.
* **Respuesta**: Devuelve **`200 OK`** con `success: true`, informando los alojamientos cargados exitosamente.

### 6.2 Fallas con la Base de Datos Supabase
* **Escenarios**: Puertos bloqueados por la red (`6543`/`5432`), instancia de base de datos pausada, credenciales incorrectas, restricciones de clave foránea o ciudad inexistente en el catálogo.
* **Comportamiento**: Las excepciones son interceptadas de manera centralizada y traducidas al código HTTP correspondiente (`503`, `409`, `422`).
* **Respuesta**: Devuelve una estructura uniforme con `success: false` y un mensaje claro que explica el motivo de la falla para facilitar su resolución.

### 6.3 Aislamiento de Fallas en Sincronización Masiva (`POST /all`)
* Durante la sincronización masiva de las 19 ciudades, el procesamiento de cada destino se realiza de forma independiente.
* Si una ciudad en particular experimenta una falla (por ejemplo, si falta en la tabla `ciudades`), esa ciudad se marca con estado de error, pero **el proceso no se cancela** y continúa sincronizando las restantes 18 sedes.
* La respuesta consolidada reporta qué ciudades concluyeron con éxito y cuáles tuvieron inconvenientes.

---

## 7. Verificación de Pruebas

Para validar el funcionamiento del microservicio y sus reglas de negocio:
```bash
./mvnw clean test
```

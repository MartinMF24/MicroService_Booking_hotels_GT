-- ====================================================================
-- MicroService_Booking_hotels_GT - Script DDL e Índices para Supabase
-- Ejecutar en: Supabase Dashboard -> SQL Editor
-- ====================================================================

-- 1. Tabla: hoteles
CREATE TABLE IF NOT EXISTS public.hoteles (
  id_hotel uuid NOT NULL DEFAULT extensions.uuid_generate_v4 (),
  id_ciudad uuid NOT NULL,
  nombre character varying(150) NOT NULL,
  estrellas integer NULL,
  distancia_circuito_km numeric(5, 2) NULL,
  ofrece_traslado boolean NULL DEFAULT false,
  imagen_principal_url text NULL,
  created_at timestamp with time zone NULL DEFAULT timezone ('utc'::text, now()),
  CONSTRAINT hoteles_pkey PRIMARY KEY (id_hotel),
  CONSTRAINT hoteles_id_ciudad_fkey FOREIGN KEY (id_ciudad) REFERENCES public.ciudades (id_ciudad) ON DELETE RESTRICT,
  CONSTRAINT hoteles_estrellas_check CHECK (estrellas >= 1 AND estrellas <= 5)
);

-- Índice para búsquedas rápidas por ciudad
CREATE INDEX IF NOT EXISTS idx_hoteles_id_ciudad 
    ON public.hoteles(id_ciudad);

-- Índice compuesto para agilizar el Upsert (búsqueda por nombre y ciudad)
CREATE INDEX IF NOT EXISTS idx_hoteles_nombre_ciudad 
    ON public.hoteles(LOWER(nombre), id_ciudad);


-- 2. Tabla: habitaciones_hotel
CREATE TABLE IF NOT EXISTS public.habitaciones_hotel (
  id_habitacion uuid NOT NULL DEFAULT extensions.uuid_generate_v4 (),
  id_hotel uuid NOT NULL,
  tipo character varying(50) NOT NULL,
  precio_por_noche_usd numeric(10, 2) NOT NULL,
  stock_disponible integer NOT NULL DEFAULT 0,
  created_at timestamp with time zone NULL DEFAULT timezone ('utc'::text, now()),
  CONSTRAINT habitaciones_hotel_pkey PRIMARY KEY (id_habitacion),
  CONSTRAINT habitaciones_hotel_id_hotel_fkey FOREIGN KEY (id_hotel) REFERENCES public.hoteles (id_hotel) ON DELETE CASCADE,
  CONSTRAINT habitaciones_hotel_tipo_check CHECK (tipo IN ('Single', 'Doble', 'Suite'))
);

-- Índice de Foreign Key para agilizar el JOIN FETCH y borrado en cascada
CREATE INDEX IF NOT EXISTS idx_habitaciones_id_hotel 
    ON public.habitaciones_hotel(id_hotel);

-- Índice para búsquedas por tipo de habitación y hotel (Upsert)
CREATE INDEX IF NOT EXISTS idx_habitaciones_hotel_tipo 
    ON public.habitaciones_hotel(id_hotel, LOWER(tipo));


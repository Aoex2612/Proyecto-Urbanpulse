# Experimento 4: Influencia del volumen de datos

Este documento define un procedimiento reproducible para estudiar cómo afecta el
tamaño de la tabla `incidents` al rendimiento de consultas filtradas, paginación,
ordenación cronológica y analítica agregada.

## Objetivo e hipótesis

Comparar las latencias de las consultas con 10.000 y 100.000 incidencias,
ampliando a 1.000.000 solo si los recursos locales lo permiten. Se
comparará la configuración de índices existente con dos índices compuestos
adicionales. La pregunta arquitectónica es si el diseño de índices mejora la
capacidad sin aumentar las instancias de Spring Boot.

## Preparación

1. Levantar PostgreSQL y el backend con la configuración habitual del proyecto.
   Usar exclusivamente una base de datos local desechable o de desarrollo.
2. Elegir un tamaño total superior al número de filas preexistentes y generar
   únicamente las filas sintéticas necesarias para alcanzarlo. El esquema
   inicial precarga aproximadamente 5.000 incidencias, por lo que 1.000 no es
   un tamaño alcanzable sin borrar datos preexistentes.

```bash
docker compose exec -T postgres psql -U admin -d urban_pulse_db -v cantidad=10000 \
  -f /dev/stdin < k6/experimento-4-datos.sql
```

Para cambiar de tamaño entre mediciones, retirar primero solo las filas sintéticas:

```bash
docker compose exec -T postgres psql -U admin -d urban_pulse_db \
  -f /dev/stdin < k6/experimento-4-limpiar.sql
```

`cantidad` es el número total deseado de filas en `incidents`, no el número de
filas nuevas. El generador requiere el usuario de desarrollo
`citizen@urbanpulse.es`, utiliza el prefijo reservado `EXP4-` para identificar
las filas añadidas y distribuye sus fechas dentro de los últimos 29 días. Si el
tamaño pedido es menor que el ya existente, aborta sin borrar datos.

## Consultas y configuración de k6

El script `k6/experimento-4-volumen-datos.js` admite estos modos:

| `MODE` | Consulta | Usuario |
|---|---|---|
| `filtered_search` | Búsqueda por categoría, distrito y rango de fechas | Administrador |
| `paginated` | Incidencias propias, página de 50 y orden descendente por creación | Ciudadano |
| `analytics` | Resumen agregado por distrito y rango de fechas | Administrador |

Ejemplo de ejecución:

```bash
MODE=filtered_search VUS=5 DURATION=2m k6 run k6/experimento-4-volumen-datos.js
```

Repetir cada modo y tamaño de datos al menos tres veces, con el entorno
estabilizado y sin otras cargas. Mantener iguales `VUS`, `DURATION`, versión de
la aplicación y configuración de infraestructura en todas las comparaciones.
Usar `MODE=paginated` para observar conjuntamente la paginación y su ordenación
por fecha: el endpoint disponible aplica ambas en una única consulta.
El resumen de k6 incluye p50, p95 y p99; `http_reqs` aporta el throughput y las
métricas de fallo permiten registrar errores.

## Comparación de índices

La configuración base conserva los índices ya definidos por el esquema. Tras
completar y guardar sus resultados, crear los índices compuestos adicionales:

```bash
docker compose exec -T postgres psql -U admin -d urban_pulse_db \
  -f /dev/stdin < k6/experimento-4-indices.sql
```

Repetir las mediciones con los mismos tamaños y parámetros. Los índices
compuestos aceleran, respectivamente, los filtros por categoría/distrito/fecha
(respetando la normalización `lower`/`trim` aplicada por la búsqueda) y la
paginación de incidencias propias por ciudadano/fecha. Para volver a la
configuración base sin retirar los índices preexistentes:

```bash
docker compose exec -T postgres psql -U admin -d urban_pulse_db \
  -f /dev/stdin < k6/experimento-4-restaurar-indices.sql
```

Si la latencia o el consumo de memoria del modo `analytics` crecen de forma
insegura, detener esa ejecución y limitar su tamaño; el servicio carga las
incidencias coincidentes para calcular el resumen. No ejecutar el millón de
filas en ese modo sin comprobar antes la memoria disponible.

## Resultados

Registrar por cada combinación de tamaño, modo e índices las tres ejecuciones:

| Filas totales | Modo | Índices | Repetición | p50 (ms) | p95 (ms) | p99 (ms) | req/s | Errores |
|---:|---|---|---:|---:|---:|---:|---:|---:|

Los resultados están pendientes de ejecución. No se incluyen cifras estimadas.
Analizar también la variación entre repeticiones, el volumen de respuesta y
los fallos. Este script usa usuarios virtuales concurrentes, por lo que no
genera `dropped_iterations`; esa métrica aplica al cambiar a un ejecutor de tasa
de llegada.

## Limpieza

Al finalizar, ejecutar `experimento-4-limpiar.sql` para retirar las filas
sintéticas y `experimento-4-restaurar-indices.sql` para retirar los índices
temporales. Ambos procedimientos conservan el resto del esquema y sus datos.

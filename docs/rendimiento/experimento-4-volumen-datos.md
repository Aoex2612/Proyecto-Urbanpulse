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
| 10.000 | `filtered_search` | Base | 1 | 1483.89 | 1525.82 | 1872.37 | 2.00 | 0.0% (0) |
| 10.000 | `filtered_search` | Base | 2 | 1493.98 | 1523.45 | 1535.53 | 2.01 | 0.0% (0) |
| 10.000 | `filtered_search` | Base | 3 | 1407.77 | 1439.62 | 1457.62 | 2.07 | 0.0% (0) |
| 10.000 | `paginated` | Base | 1 | 72.74 | 80.38 | 88.35 | 4.66 | 0.0% (0) |
| 10.000 | `paginated` | Base | 2 | 72.38 | 78.15 | 83.06 | 4.66 | 0.0% (0) |
| 10.000 | `paginated` | Base | 3 | 68.83 | 73.65 | 75.48 | 4.68 | 0.0% (0) |
| 10.000 | `analytics` | Base | 1 | 7605.87 | 8114.62 | 8115.24 | 0.59 | 0.0% (0) |
| 10.000 | `analytics` | Base | 2 | 7445.77 | 7656.54 | 7682.65 | 0.60 | 0.0% (0) |
| 10.000 | `analytics` | Base | 3 | 7260.73 | 7319.85 | 7326.27 | 0.61 | 0.0% (0) |
| 10.000 | `filtered_search` | Adicionales | 1 | 539.76 | 558.21 | 588.28 | 3.24 | 0.0% (0) |
| 10.000 | `filtered_search` | Adicionales | 2 | 502.98 | 574.94 | 601.06 | 3.30 | 0.0% (0) |
| 10.000 | `filtered_search` | Adicionales | 3 | 542.78 | 577.43 | 660.58 | 3.24 | 0.0% (0) |
| 10.000 | `paginated` | Adicionales | 1 | 26.92 | 30.29 | 31.72 | 4.87 | 0.0% (0) |
| 10.000 | `paginated` | Adicionales | 2 | 26.60 | 28.80 | 29.45 | 4.87 | 0.0% (0) |
| 10.000 | `paginated` | Adicionales | 3 | 26.65 | 29.04 | 30.19 | 4.87 | 0.0% (0) |
| 10.000 | `analytics` | Adicionales | 1 | 3151.18 | 3508.21 | 3520.91 | 1.18 | 0.0% (0) |
| 10.000 | `analytics` | Adicionales | 2 | 3236.11 | 3502.83 | 3516.32 | 1.17 | 0.0% (0) |
| 10.000 | `analytics` | Adicionales | 3 | 3228.91 | 3512.70 | 3536.48 | 1.17 | 0.0% (0) |
| 100.000 | `filtered_search` | Base | 1 | 6607.23 | 6699.28 | 6713.47 | 0.66 | 0.0% (0) |
| 100.000 | `filtered_search` | Base | 2 | 6570.33 | 6655.50 | 6685.69 | 0.67 | 0.0% (0) |
| 100.000 | `filtered_search` | Base | 3 | 6585.81 | 6697.84 | 6741.00 | 0.66 | 0.0% (0) |
| 100.000 | `paginated` | Base | 1 | 174.45 | 195.00 | 206.16 | 4.25 | 0.0% (0) |
| 100.000 | `paginated` | Base | 2 | 174.91 | 184.86 | 227.05 | 4.22 | 0.0% (0) |
| 100.000 | `paginated` | Base | 3 | 174.15 | 182.32 | 186.39 | 4.26 | 0.0% (0) |
| 100.000 | `analytics` | Base | 1 | 40668.16 | 43968.87 | 44105.20 | 0.13 | 0.0% (0) |
| 100.000 | `analytics` | Base | 2 | 40268.64 | 46800.38 | 46853.29 | 0.12 | 0.0% (0) |
| 100.000 | `analytics` | Base | 3 | 41858.83 | 44153.80 | 44216.36 | 0.13 | 0.0% (0) |
| 100.000 | `filtered_search` | Adicionales | 1 | 6552.03 | 6626.99 | 6639.01 | 0.67 | 0.0% (0) |
| 100.000 | `filtered_search` | Adicionales | 2 | 6523.16 | 6589.59 | 6608.90 | 0.67 | 0.0% (0) |
| 100.000 | `filtered_search` | Adicionales | 3 | 6531.70 | 6618.89 | 6633.05 | 0.67 | 0.0% (0) |
| 100.000 | `paginated` | Adicionales | 1 | 90.18 | 95.13 | 97.83 | 4.59 | 0.0% (0) |
| 100.000 | `paginated` | Adicionales | 2 | 89.77 | 94.77 | 96.16 | 4.59 | 0.0% (0) |
| 100.000 | `paginated` | Adicionales | 3 | 90.39 | 95.43 | 97.10 | 4.59 | 0.0% (0) |
| 100.000 | `analytics` | Adicionales | 1 | 40227.70 | 41739.30 | 41748.22 | 0.13 | 0.0% (0) |
| 100.000 | `analytics` | Adicionales | 2 | 40931.66 | 44360.93 | 44429.24 | 0.13 | 0.0% (0) |
| 100.000 | `analytics` | Adicionales | 3 | 58870.42 | 77112.03 | 77155.88 | 0.07 | 0.0% (0) |

### Análisis de resultados

- **Variación y errores:** Todas las ejecuciones completaron con **0 % de errores** (`0` fallos HTTP y checks al 100 %). La variabilidad entre repeticiones es muy reducida (< 5 % en la mayoría de los casos), excepto en `analytics` con 100.000 filas e índices adicionales (donde la tercera repetición alcanzó 58,87 s en p50 y 77,11 s en p95, consistente con presión de memoria y pausas de recolección de basura en la JVM). Debido a la degradación severa observada en `analytics` a 100.000 filas (> 40 s de latencia), no se escaló a 1.000.000 de filas.
- **Impacto de los índices en 10.000 filas:** Los índices compuestos reducen la latencia mediana de forma uniforme en todos los modos:
  - `filtered_search`: pasa de ~1.462 ms a ~529 ms (**-63,8 %**).
  - `paginated`: pasa de ~71 ms a ~27 ms (**-62,5 %**).
  - `analytics`: pasa de ~7.438 ms a ~3.205 ms (**-56,9 %**).
- **Cuellos de botella en 100.000 filas y volumen de respuesta:**
  - `paginated` mantiene un tamaño de respuesta constante (~26,6 KB por petición) y sigue beneficiándose claramente del índice compuesto `(citizen_id, created_at DESC)`, bajando su p50 media de ~174,5 ms a ~90,1 ms (**-48,4 %**) y manteniendo un throughput estable de 4,59 req/s.
  - `filtered_search` apenas mejora con el índice a 100.000 filas (~6.588 ms base frente a ~6.536 ms con índices). Al no estar paginado, el volumen medio de respuesta crece de ~651 KB (10.000 filas) a **~2,98 MB por petición** (100.000 filas), desplazando el cuello de botella desde PostgreSQL hacia la hidratación de entidades JPA, el mapeo a DTO y la serialización JSON en Spring Boot.
  - `analytics` devuelve **~12,99 MB por petición** con 100.000 filas (frente a ~1,35 MB con 10.000), ya que el backend recupera todas las incidencias coincidentes para calcular el resumen en memoria. En este escenario el coste de transferencia, memoria y serialización domina completamente el tiempo de respuesta (> 40 s), haciendo irrelevante la ganancia del índice en base de datos.
- **Conclusión arquitectónica:** El diseño de índices mejora de forma efectiva la capacidad en consultas acotadas y paginadas sin necesidad de escalar instancias de Spring Boot. Sin embargo, para endpoints sin paginación (`filtered_search`) o agregaciones procesadas en aplicación (`analytics`), los índices no evitan la degradación ante volúmenes altos: se requiere introducir paginación en búsqueda y delegar el cálculo agregado a consultas `GROUP BY` en PostgreSQL.

*(Nota sobre métricas de k6: este script usa usuarios virtuales concurrentes, por lo que no genera `dropped_iterations`; esa métrica aplica al cambiar a un ejecutor de tasa de llegada).*

## Limpieza

Al finalizar, ejecutar `experimento-4-limpiar.sql` para retirar las filas
sintéticas y `experimento-4-restaurar-indices.sql` para retirar los índices
temporales. Ambos procedimientos conservan el resto del esquema y sus datos.

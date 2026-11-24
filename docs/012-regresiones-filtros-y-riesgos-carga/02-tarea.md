# Tarea 2: Consistencia documental del Experimento 3

## Descripción
Se ha realizado una revisión y armonización de las cifras y métricas documentadas en el informe del **Experimento 3: Simulación de un Pico de Incidencias Urbanas (Emergencia Urbana / Spike Test)** (`docs/rendimiento/experimento-3-emergencia.md`).

Se clarificó explícitamente la distinción entre:
1. **Peticiones HTTP Totales (1.022 peticiones):** Incluye la petición técnica inicial de autenticación en `setup()` (1 request a `/api/v1/auth/login`) más las peticiones concurrentes bajo carga.
2. **Peticiones y Checks bajo Carga (1.021 checks / reqs):** Las peticiones de los dos escenarios concurrentes evaluadas con aserciones o checks en k6:
   - **446 lecturas del mapa** (`GET /api/v1/incidents` con check de HTTP 200).
   - **575 reportes de emergencia** (`POST /api/v1/incidents` con check de HTTP 201).
   - Suma exacta: $446 + 575 = 1.021$.

## Archivos modificados
- `docs/rendimiento/experimento-3-emergencia.md`:
  - Se actualizó la tabla de *Resumen Global de Ejecución* desagregando las peticiones totales (1.022) y los checks bajo carga (1.021).
  - Se añadió una nota explicativa aclarando la procedencia de cada cifra (1 login en `setup()` + 1.021 peticiones bajo carga).
  - En la sección *Hallazgo 1*, se clarificó la relación entre las 1.022 peticiones HTTP exitosas y los 1.021 checks bajo carga (446 lecturas y 575 escrituras).
- `docs/012-regresiones-filtros-y-riesgos-carga/tasks.md`:
  - Se marcó como completada la Tarea 2 (`[x]`).

## Verificación
- Revisión de consistencia matemática: $1 \text{ (login setup)} + 446 \text{ (lecturas)} + 575 \text{ (escrituras)} = 1.022 \text{ peticiones totales HTTP}$.
- Revisión de coherencia con el script `k6/experimento-3-emergencia.js` y el reporte `k6/reports/exp3_emergencia_spike.html`.

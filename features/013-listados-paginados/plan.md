# 013 — Lectura paginada y respuesta ligera de incidencias — Plan

_¿Cómo se implementa lo descrito en `spec.md`? Respeta estrictamente la `constitution/`, la capa de mappers/DTOs y las decisiones registradas en `docs/ADR/`._

## Enfoque

La estrategia es **aditiva y retrocompatible**, tal como fija el **ADR-12**:

1. Un endpoint ligero específico para el mapa (`GET /api/v2/incidents/map`) que devuelve solo los campos necesarios y elimina el correo del ciudadano.
2. Paginación opcional (`page`/`size`) en `/api/v2/incidents` y `/api/v2/incidents/search`: sin parámetros se conserva la respuesta actual; con parámetros se devuelve el `PageResponse` ya existente.
3. Migración del panel de operador a paginación de servidor siguiendo el patrón ya validado en `MyIncidentsPage`.
4. Eliminación del N+1 de `getActiveIncidentsV2` mediante carga anticipada por lote o proyección, sin cambiar la forma pública del DTO completo.

No se cambia la versión de la API ni se rompe ningún consumidor en esta entrega. La retirada de la respuesta completa se pospone a una versión mayor.

## Implementación

### 1. DTO ligero de mapa

- Crear `Backend/src/main/java/org/bulbaseros/dto/v2/IncidentMapResponseV2.java`:
  - Campos: `id` (UUID público como `String`), `latitude`, `longitude`, `status`, `priority`, `category`, `createdAt` y `title`.
  - Sin `description`, `citizenEmail`, `citizenName`, `citizenId`, `urbanAssets`, `urbanContext`, `activeAssignments`, `address`, `district` ni `neighborhood`.
  - Anotaciones Lombok (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`) y Javadoc de clase y campos clave, conforme a `AGENTS.md`.
- Añadir prueba de contrato que serialice el DTO y falle si aparece `citizenEmail` o cualquier campo excluido.

### 2. Mapper dedicado

- Crear `Backend/src/main/java/org/bulbaseros/mappers/IncidentMapMapperV2.java`:
  - `toDto(Incidencia)` y `toDtoList(List<Incidencia>)` sin dependencias de repositorios.
  - Omitir incidencias con `latitude` o `longitude` nulas o fuera de rango.
  - Truncar el título al tamaño corto acordado en la spec (valor pendiente de aprobación; se propone 80 caracteres con elipsis).
- No reutilizar `IncidentMapperV2.toDto` para el mapa: ese mapper resuelve asignaciones por incidencia y es el origen del N+1.

### 3. Endpoint ligero

- `Backend/src/main/java/org/bulbaseros/controller/v2/IncidentControllerV2.java`:
  - Añadir `@GetMapping("/map")` antes de `/{id}` (Spring resuelve primero las rutas literales).
  - Devolver `ResponseEntity<List<IncidentMapResponseV2>>`.
  - Mantener la protección y el filtrado por rol ya existentes; el endpoint no expone datos personales.
- `Backend/src/main/java/org/bulbaseros/service/IncidentService.java`:
  - Añadir `getActiveIncidentsMapV2()` con `@Transactional(readOnly = true)`.
  - Reutilizar `findByStatusNotIn(CLOSED, REJECTED)` y mapear con `IncidentMapMapperV2`.
  - No resolver activos, contexto urbano, asignaciones ni correo.

### 4. Paginación opcional en listados

- `Backend/src/main/java/org/bulbaseros/repository/IncidenciaRepository.java`:
  - Añadir `Page<Incidencia> findByStatusNotIn(Collection<EstadoIncidencia> statuses, Pageable pageable)`.
- `Backend/src/main/java/org/bulbaseros/controller/v2/IncidentControllerV2.java`:
  - En `getActiveIncidents`, aceptar `@RequestParam(required = false) Integer page` y `size`.
  - Si ambos son `null`, mantener `ResponseEntity<List<IncidentResponseV2>>`.
  - Si alguno se envía, validar `size` entre 1 y 100 y devolver `PageResponse<IncidentResponseV2>`.
  - Usar `ResponseEntity<?>` o un tipo común documentado para soportar ambas formas sin romper la firma existente.
  - Aplicar el mismo patrón a `search`, añadiendo `page`/`size` a los parámetros ya existentes.
- `Backend/src/main/java/org/bulbaseros/service/IncidentService.java`:
  - Sobrecargas `getActiveIncidentsV2(Integer page, Integer size)` y `searchIncidentsV2(..., Integer page, Integer size)`.
  - Página base 0, orden descendente por `createdAt` cuando no se especifique otro.
  - Página fuera de rango: `PageResponse` vacío con metadatos coherentes.
  - `size` inválido: `400` con `ErrorResponse`, no un valor silencioso.
- No se aplica límite por defecto cuando no hay parámetros: la respuesta actual se conserva (ADR-12).

### 5. Eliminación del N+1

- `Backend/src/main/java/org/bulbaseros/repository/TareasRepository.java`:
  - Añadir una consulta por lote para obtener las tareas activas de un conjunto de `publicId` de incidencia.
- `Backend/src/main/java/org/bulbaseros/mappers/IncidentMapperV2.java`:
  - Añadir una sobrecarga `toDto(Incidencia, List<Tareas>)` o un método `toDtoList(List<Incidencia>, Map<UUID, List<Tareas>>)` que no consulte el repositorio por cada elemento.
  - Mantener el método actual para el detalle puntual.
- `Backend/src/main/java/org/bulbaseros/service/IncidentService.java`:
  - Precargar las tareas activas en una sola consulta para el listado o la página y pasarlas al mapper.
  - Alternativa preferente si la consulta se complica: proyección directa a DTO en la consulta del repositorio.
- Verificación: con `spring.jpa.properties.hibernate.generate_statistics=true` y el logger de estadísticas en `INFO`, el número de sentencias JDBC debe ser constante (no proporcional a 4.500 incidencias).

### 6. Frontend: servicio y adaptador

- `frontend/src/services/incidentService.js`:
  - Añadir `getMapIncidents()` contra `/api/v2/incidents/map`.
  - Añadir `getIncidentsPage({ page, size, ...filtros })` que devuelva siempre `{ content, totalElements, totalPages, page, size }`.
  - Mantener `getAllIncidents()` y `searchIncidents()` sin parámetros para los consumidores legacy, o normalizar ambas formas en un adaptador interno.
  - Documentar con JSDoc la diferencia entre lista completa y página.
- `frontend/src/services/incidentService.test.js`:
  - Actualizar las aserciones de query params y añadir pruebas de normalización de ambas formas.

### 7. Frontend: mapa y panel de operador

- `frontend/src/pages/MapPage.jsx`:
  - Consumir `getMapIncidents()` en lugar de `getAllIncidents()`.
  - Mantener la ficha completa mediante la consulta de detalle existente al abrir `IncidentDetailModal`.
  - Adaptar los campos usados por el mapa (título corto, estado, prioridad, categoría, fecha).
- `frontend/src/pages/OperatorPanelPage.jsx`:
  - Replicar el patrón de `MyIncidentsPage`: `PAGE_SIZE = 10`, estado `currentPage`, `totalPages`, `totalElements`.
  - Cargar solo la página actual; al cambiar de página no recargar el catálogo completo.
  - Reiniciar en la página 1 al aplicar o modificar filtros.
  - Enviar los filtros multicriterio como query params.
  - Añadir indicador `aria-live` de carga y de página actual sobre total.
  - Si se permite más de 50 filas por página, evaluar `@tanstack/react-virtual` (peer React 19) para mantener el desplazamiento fluido.

## Decisiones

- **Contrato aditivo, no ruptura** — Decisión del **ADR-12**: preserva consumidores y pruebas, y permite migrar por partes. Se descarta convertir los endpoints actuales a paginación obligatoria en esta entrega.
- **Endpoint `/map` separado en lugar de `fields=` en el listado** — Un DTO explícito es más fácil de validar, documentar y proteger contra fugas de PII que una selección dinámica de campos.
- **Mapper dedicado para el mapa** — Evita arrastrar la resolución de asignaciones del mapper completo y hace imposible el N+1 en esa ruta por construcción.
- **`PageResponse` existente** — Ya se usa en `/my-incidents` y en el listado legacy; no se introduce un formato nuevo (ADR-12, ADR-03).
- **Sin límite por defecto en búsqueda** — Se prioriza la retrocompatibilidad estricta; la acotación se pospone a una versión mayor y se registra como deuda.
- **Paginación en servidor para el panel** — Se descarta paginar solo en cliente porque no reduce payload ni exposición y mantiene el coste de parseo.

## Línea base (Antes / Objetivo)

Medición local realizada el 2026-11-26 contra el backend en `http://localhost:8080` y una base PostgreSQL aislada `urban_pulse_baseline` sembrada con el script `database/init/diagramUrbanPulse.sql` (5.000 incidencias totales, 4.500 activas, 0 sin coordenadas). El frontend se midió con `npm run build`, `npm test` y `npm run test:coverage`.

| Métrica | Antes (medido) | Objetivo | Fuente |
| :--- | ---: | ---: | :--- |
| `GET /api/v2/incidents` — payload | 4.771.646 bytes (~4,55 MiB) | ≤ 1 MB con el endpoint `/map` | `curl.exe -w` |
| `GET /api/v2/incidents` — tiempo | 9.851 ms (mediana de 3; 11.540 / 9.851 / 9.611) | p95 ≤ 1.000 ms en `/map` | `curl.exe -w` |
| `GET /api/v2/incidents` — elementos | 4.500 incidencias activas | Página de 10 en el panel | Inspección JSON |
| `GET /api/v2/incidents/search?category=ALUMBRADO` — payload | 659.660 bytes (~644 KiB) | Reducción proporcional al tamaño de página | `curl.exe -w` |
| `GET /api/v2/incidents/search?category=ALUMBRADO` — tiempo | 1.368 ms (mediana de 3; 1.521 / 1.368 / 1.276) | p95 ≤ 1.000 ms con paginación | `curl.exe -w` |
| `GET /api/v2/incidents/search?category=ALUMBRADO` — elementos | 626 incidencias | Página de 10 en el panel | Inspección JSON |
| `GET /api/v2/incidents/{id}` — payload | 757 bytes | Sin cambios | `curl.exe -w` |
| `GET /api/v2/incidents/{id}` — tiempo | 33 ms (mediana de 3; 72 / 33 / 27) | Sin cambios | `curl.exe -w` |
| `GET /api/v2/incidents/map` | 400: la ruta no existe y cae en `/{id}` con un UUID inválido | 200 con DTO ligero | `curl.exe` |
| `citizenEmail` en la respuesta del listado | 4.500 apariciones (una por incidencia) | 0 en `/map` | Inspección JSON |
| Consultas a `assignments` en un listado | 4.502 `seq_scan` para 4.500 activas | Constante (1–2) | `pg_stat_user_tables` |
| JS de entrada principal | 1.063,46 kB (329,77 kB gzip) | Sin aumento por esta spec | `npm run build` |
| Tests frontend | 37 archivos / 303 tests en verde | 100 % en verde | `npm test` |
| Cobertura de sentencias | 81,68 % | Sin regresión > 1 p.p. | `npm run test:coverage` |
| Cobertura de ramas | 71,91 % | Sin regresión > 1 p.p. | `npm run test:coverage` |
| Filas del panel de operador con 4.500 activas | 4.500 filas en un solo render | 10 filas por página | Inspección de código / DevTools |
| Nodos del DOM del panel | Pendiente de medición en navegador | ≤ 500 nodos de tabla por página | DevTools |

**Nota:** las cifras de payload y latencia se obtuvieron en local, con la JVM en caliente y sin concurrencia. No sustituyen a la medición k6 de `docs/rendimiento/experimento-1-baseline.md`; sirven para comparar antes/después con el mismo equipo y método. El detalle del protocolo está en `docs/rendimiento/experimento-4-baseline-frontend.md`.

## Riesgos

- **Riesgo: romper consumidores al cambiar la forma de respuesta.** Mitigación: sin `page`/`size` se conserva la lista actual; pruebas de contrato para ambas formas.
- **Riesgo: `ResponseEntity<?>` degrade la documentación OpenAPI.** Mitigación: documentar ambas variantes y añadir ejemplos en las pruebas de controlador.
- **Riesgo: la proyección o el `Pageable` dupliquen filas por joins.** Mitigación: usar subconsulta `EXISTS` o carga por lote, no `JOIN` con colecciones; verificar conteos.
- **Riesgo: N+1 persista en el listado completo.** Mitigación: medir con estadísticas Hibernate y considerar la consulta una condición de aceptación, no una mejora opcional.
- **Riesgo: el panel pierde filtros al paginar.** Mitigación: centralizar filtros en query params y reiniciar página al cambiarlos; pruebas de integración de la página.
- **Riesgo: el endpoint `/map` omita incidencias válidas por coordenadas nulas.** Mitigación: probar con incidencias sin coordenadas y documentar la omisión en la spec (RF-3).
- **Riesgo: el título corto pierda información necesaria.** Mitigación: el detalle completo sigue disponible por identificador; el truncado se define en la spec.

## Estrategia de tests

- **Backend unitario:** mapper ligero (campos y omisión de coordenadas), validación de `page`/`size`, ramas con y sin paginación.
- **Backend de contrato:** respuesta sin parámetros idéntica a la anterior; respuesta paginada con `content`, `totalElements`, `totalPages`, `page` y `size`; ausencia de `citizenEmail` en `/map`.
- **Backend de integración con PostgreSQL:** conteo de consultas con estadísticas Hibernate; página fuera de rango; tamaño inválido; orden estable entre páginas.
- **Frontend:** adaptador de `incidentService`, `MapPage` con el endpoint ligero, `OperatorPanelPage` con paginación, cambio de página, reinicio al filtrar e indicador `aria-live`.
- **Regresión:** mantener verdes los 303 tests actuales y actualizar los mocks que devuelven arrays.

## Referencias

- `features/013-listados-paginados/spec.md`
- `docs/ADR/ADR-03-desacoplamiento-dtos-mappers.md`
- `docs/ADR/ADR-10-identificadores-publicos-uuid-y-versionado-api-v2.md`
- `docs/ADR/ADR-12-contrato-de-lectura-de-listados-de-incidencias.md`
- `docs/rendimiento/experimento-1-baseline.md`
- `docs/rendimiento/experimento-4-baseline-frontend.md`
- `docs/issue-refactor-frontend.md` (hallazgos C3, C4 y riesgos R1–R4)

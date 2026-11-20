# 007 — Activos Urbanos y Contexto Externo — Plan

_Cómo se implementa lo descrito en `spec.md`. Respeta la `constitution/` y se sustenta en [ADR-09](../../docs/ADR/ADR-09-activos-urbanos-contexto-externo-y-resiliencia.md)._

## Enfoque

La solución amplía la plataforma UrbanPulse integrando el enriquecimiento espacial y ambiental de incidencias sin comprometer la disponibilidad ni el rendimiento del flujo de reporte ciudadano. 
Se estructuran tres capas de solución:
1. **HU16 (Asociación con Activos):** Servicio de cálculo geodésico de proximidad (Haversine optimizado con pre-filtrado por bounding box) que busca activos (`urban_assets`) en un radio de 50m, calcula el nivel de confianza ($1.0 - d/50$) y genera asociaciones no confirmadas en `incident_asset_associations`, exponiendo endpoints REST protegidos por RBAC para confirmar o desvincular activos por parte de operadores y administradores.
2. **HU17 (Ingesta de Contexto Externo):** Conector HTTP cliente para ingesta de datos meteorológicos (Open-Meteo) y de tráfico local en el momento de la creación de la incidencia, normalizando los datos en `external_observations` y relacionándolos mediante `incident_contexts`.
3. **HU18 (Degradación Controlada & Resiliencia):** Incorporación de timeouts de 3.000 ms y patrón Circuit Breaker con ventana deslizante y periodo de enfriamiento. Ante fallos de conexión, timeout o apertura del circuito, se activa de manera transparente un fallback silencioso que permite crear la incidencia (`HTTP 201 Created`) con colecciones vacías de activos o contexto sin bloquear ni demorar al ciudadano.
4. **Presentación Frontend:** Ficha de detalle de incidencia extendida en React para renderizar la tarjeta de activos urbanos próximos (con badges de distancia, confianza y botón de confirmación/desvinculación para operadores) y la tarjeta de contexto ambiental y meteorológico en el instante del aviso.

---

## Implementación

### 1. Modelo de Datos y Persistencia Backend
- Extender/verificar repositorios Spring Data JPA para las entidades existentes:
  - `ActivoUrbanoRepository`: Métodos de búsqueda por bounding box espacial `findByLatitudeBetweenAndLongitudeBetween(...)` y por `assetCode`.
  - `AsociacionIncidenciaActivoRepository`: Métodos para consultar asociaciones por `incidentId`, buscar por clave compuesta y eliminar por id.
  - `ObservacionesExternasRepository` y `ContextoIncidenciaRepository`: Métodos para guardar y asociar observaciones a una incidencia.
- Archivos:
  - `Backend/src/main/java/org/bulbaseros/repository/ActivoUrbanoRepository.java`
  - `Backend/src/main/java/org/bulbaseros/repository/AsociacionIncidenciaActivoRepository.java`
  - `Backend/src/main/java/org/bulbaseros/repository/ObservacionesExternasRepository.java`
  - `Backend/src/main/java/org/bulbaseros/repository/ContextoIncidenciaRepository.java`

### 2. DTOs y Mappers Backend
- Crear DTOs para transporte desacoplado:
  - `IncidentAssetResponse`: `assetId`, `assetCode`, `type`, `name`, `latitude`, `longitude`, `distanceMeters`, `confidenceScore`, `isConfirmed`.
  - `ExternalObservationResponse`: `id`, `source`, `metricType`, `valueNum`, `valueText`, `observedAt`.
- Mappers dedicados:
  - `UrbanAssetMapper`: Mapeo bidireccional entre `AsociacionIncidenciaActivo` / `ActivoUrbano` y `IncidentAssetResponse`.
  - `UrbanContextMapper`: Mapeo de `ObservacionesExternas` a `ExternalObservationResponse`.
- Actualizar `IncidentResponse` para incorporar:
  - `private List<IncidentAssetResponse> urbanAssets;`
  - `private List<ExternalObservationResponse> urbanContext;`
- Archivos:
  - `Backend/src/main/java/org/bulbaseros/dto/IncidentAssetResponse.java`
  - `Backend/src/main/java/org/bulbaseros/dto/ExternalObservationResponse.java`
  - `Backend/src/main/java/org/bulbaseros/mappers/UrbanAssetMapper.java`
  - `Backend/src/main/java/org/bulbaseros/mappers/UrbanContextMapper.java`
  - `Backend/src/main/java/org/bulbaseros/dto/IncidentResponse.java`

### 3. Servicios de Negocio y Lógica de Proximidad (HU16)
- Crear `UrbanAssetService`:
  - Algoritmo de cálculo de distancia mediante fórmula de Haversine (`distanceInMeters(lat1, lon1, lat2, lon2)`).
  - Búsqueda de candidatos en radio de 50 metros y cálculo de `confidenceScore = Math.max(0.0, 1.0 - (distance / 50.0))`.
  - Método `associateNearbyAssets(Incidencia incident)`: detecta candidatos y crea registros en `incident_asset_associations`.
  - Método `confirmAssetAssociation(Long incidentId, Long assetId)`: valida rol, actualiza `is_confirmed = true`.
  - Método `removeAssetAssociation(Long incidentId, Long assetId)`: elimina el vínculo.
- Archivo: `Backend/src/main/java/org/bulbaseros/service/UrbanAssetService.java`

### 4. Integración Externa y Patrón Circuit Breaker / Fallback (HU17, HU18)
- Crear componente de resiliencia `CircuitBreakerManager` o gestor de estado para fuentes externas:
  - Control de fallos sucesivos (umbral 50% en ventana de 10 peticiones).
  - Estados: CLOSED, OPEN, HALF_OPEN y periodo de enfriamiento de 30s.
- Crear `ExternalContextService`:
  - Cliente REST con timeout estricto de conexión y lectura (3.000 ms).
  - Consulta meteorológica a Open-Meteo (temperatura, precipitación, viento en las coordenadas del reporte).
  - Consulta de tráfico municipal (o stub de respaldo cuando no esté disponible el WFS de tráfico de Málaga).
  - Manejo de Fallback transparente: si el circuito está abierto o la petición externa lanza excepción/timeout, registra un log de aviso y devuelve lista vacía sin propagar excepciones al hilo de creación.
- Integrar `associateNearbyAssets` y `captureExternalContext` en `IncidentService.createIncident(...)`.
- Archivos:
  - `Backend/src/main/java/org/bulbaseros/service/resilience/CircuitBreaker.java`
  - `Backend/src/main/java/org/bulbaseros/service/ExternalContextService.java`
  - `Backend/src/main/java/org/bulbaseros/service/IncidentService.java`

### 5. Controladores REST y Seguridad RBAC
- Exponer endpoints en `IncidentController` o crear `UrbanAssetController`:
  - `POST /api/incidents/{incidentId}/assets/{assetId}/confirm`: Restringido a `hasAnyRole('OPERATOR', 'ADMIN')`.
  - `DELETE /api/incidents/{incidentId}/assets/{assetId}`: Restringido a `hasAnyRole('OPERATOR', 'ADMIN')`.
  - Actualizar `GET /api/incidents/{id}` para enriquecer la respuesta con `urbanAssets` y `urbanContext`.
- Manejo de excepciones en `GlobalExceptionHandler` (`EntityNotFoundException`, etc.).
- Archivo:
  - `Backend/src/main/java/org/bulbaseros/controller/IncidentController.java`
  - `Backend/src/main/java/org/bulbaseros/exception/GlobalExceptionHandler.java`

### 6. Frontend: Servicios API y Vistas de Detalle
- Actualizar `frontend/src/services/incidentService.js`:
  - Métodos `confirmIncidentAsset(incidentId, assetId)` y `removeIncidentAsset(incidentId, assetId)`.
- Crear/actualizar componentes en `frontend/src/components/`:
  - `IncidentAssetCard.jsx`: Lista de activos urbanos vinculados con badge de tipo (semáforo, contenedor, etc.), distancia en metros, indicador de confianza y botón de acción de confirmación para operadores.
  - `IncidentContextCard.jsx`: Panel meteorológico y de tráfico que muestra temperatura, viento, lluvia y estado de la vía al momento del reporte.
  - Integrar ambas tarjetas en el modal/vista de detalle de la incidencia (`IncidentDetailModal.jsx`).

---

## Decisiones

- **Cálculo de Proximidad con Haversine vs. PostGIS:** Se opta por Haversine con prefiltrado por caja delimitadora (`bounding box`) en Java/Spring Data JPA para mantener el proyecto sin dependencias de extensiones nativas no portables en el contenedor PostgreSQL básico, garantizando compatibilidad total con los tests de integración en memoria (H2) y locales.
- **Circuit Breaker implementado con componentes ligeros Spring Boot vs. Netflix Hystrix/Resilience4j externo:** Se diseña una solución desacoplada y concisa orientada a Spring 3.4.3 con timeouts directos en `RestTemplate` y control de fallos en estado atómico, minimizando la complejidad de dependencias externas no declaradas en la constitución.
- **Confirmación manual por operador:** Para evitar falsos positivos cuando existen múltiples elementos de infraestructura cercanos (ej. tres contenedores de reciclaje contiguos), el sistema preselecciona el candidato más cercano con menor distancia, pero deja la confirmación definitiva (`is_confirmed = true`) como una acción verificable de triaje.
- **Consulta Externa en Creación:** El enrichment se ejecuta antes de cerrar la transacción de la incidencia o de forma desacoplada protegida por fallback; el timeout de 3.000 ms garantiza que la UX del usuario no se congele.

---

## Riesgos

- **Riesgo:** Latencias elevadas en Open-Meteo o portal de datos abiertos provocan retrasos perceptibles en el guardado de la incidencia.
  - *Mitigación:* Timeouts de conexión y lectura estrictos de 3 segundos; si se rebasa, el Fallback se ejecuta inmediatamente guardando la incidencia con contexto vacío.
- **Riesgo:** El catálogo de activos (`urban_assets`) en BD está vacío inicialmente en entornos de desarrollo o tests.
  - *Mitigación:* Se cargan datos de inicialización (seeds) representativos de mobiliario de Málaga (semáforos en Alameda Principal, fuentes en Parque de Málaga, paradas de autobús) en `database/init/` y fixtures de pruebas.
- **Riesgo:** Conflicto de permisos si un usuario `CITIZEN` intenta manipular asociaciones de activos.
  - *Mitigación:* Restricción a nivel de método con `@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")` y comprobación en Spring Security.

---

## Estrategia de tests

- **Backend Unit Tests:**
  - `UrbanAssetServiceTest`: Pruebas del cálculo de distancia Haversine, ordenación por cercanía, cálculo de nivel de confianza y filtrado por umbral de 50 metros.
  - `CircuitBreakerTest`: Verificación de transición de estados (CLOSED -> OPEN -> HALF_OPEN -> CLOSED) ante fallos simulados y ratio de error.
  - `ExternalContextServiceTest`: Simulación con Mockito / `MockRestServiceServer` para respuestas 200 OK válidas de clima, respuestas erróneas 500, timeouts y activación del fallback retornando lista vacía.
- **Backend Integration Tests:**
  - `IncidentControllerAssetIntegrationTest`: Verificación de los endpoints `POST /confirm` y `DELETE /assets/{assetId}`, control de accesos RBAC (403 para ciudadanos, 200 para operadores) y persistencia en base de datos.
- **Frontend Tests (Vitest):**
  - Pruebas de renderizado de `IncidentAssetCard` y `IncidentContextCard` con datos completos y con estado vacío / no disponible.
  - Pruebas del flujo de confirmación de activo con simulación de llamada al servicio y actualización reactiva del estado.

# Tarea 5: Integración en Creación y Consulta de Incidencias

## Resumen de la Tarea

En esta tarea se ha completado la orquestación e integración de los subsistemas de **Activos Urbanos** (`UrbanAssetService`) y **Contexto Externo** (`ExternalContextService`) dentro del flujo principal del ciclo de vida de incidencias en `IncidentService`.

Esta integración da cumplimiento a los requisitos funcionales **RF-1, RF-5, RF-8, RF-10 y RF-15** definidos en la especificación (`spec.md`) y en los registros de decisión de arquitectura **ADR-06** y **ADR-09**, asegurando **degradación controlada absoluta**: la creación de la incidencia (`HTTP 201 Created`) y la consulta de su detalle siempre se completan con éxito independientemente de la disponibilidad, latencia o caídas transitorias de los servicios de activos o APIs remotas de terceros (Open-Meteo, tráfico municipal).

---

## Modificaciones Implementadas

### 1. Inyección de Dependencias en `IncidentService.java`
- **Ubicación:** `Backend/src/main/java/org/bulbaseros/service/IncidentService.java`
- Se inyectaron como dependencias opcionales (`@Autowired(required = false)`):
  - `UrbanAssetService urbanAssetService`
  - `ExternalContextService externalContextService`
- Se actualizaron los constructores de `IncidentService` para permitir la inyección tanto en el contenedor de Spring Boot como en entornos desacoplados de pruebas unitarias.

### 2. Integración en `createIncident(CreateIncidentRequest, String)`
- Tras persistir con éxito la entidad `Incidencia` en la base de datos relacional y transformarla a su DTO inicial `IncidentResponse`:
  - **Asociación de Activos Cercanos (RF-1, RF-5):** Invocación de `urbanAssetService.associateNearbyAssets(savedIncident)` dentro de un bloque `try-catch` defensivo. Si se produce cualquier excepción inesperada, se captura, se emite una advertencia de registro (`log.warn`) y se asigna una lista vacía (`Collections.emptyList()`).
  - **Captura de Contexto Ambiental y Vial (RF-8, RF-10):** Invocación de `externalContextService.captureAndSaveContext(savedIncident)` protegida de igual forma con `try-catch` defensivo. Ante fallos de conexión o circuito abierto, se asigna una lista vacía (`Collections.emptyList()`).
  - **Población en `IncidentResponse`:** Los campos `urbanAssets` y `urbanContext` quedan debidamente completados y expuestos al cliente HTTP.

### 3. Integración en `getIncidentById(Long)`
- Al consultar una incidencia existente por su identificador único:
  - Se obtienen las asociaciones de activos registradas mediante `urbanAssetService.getAssetsByIncidentId(id)` y se asignan a `response.setUrbanAssets(...)`.
  - Se recuperan las observaciones de contexto histórico mediante `externalContextService.getContextByIncidentId(id)` y se asignan a `response.setUrbanContext(...)`.
  - Ambas operaciones se encuentran envueltas en captura defensiva para garantizar que la consulta de la incidencia jamás falle con error 500 si se presentasen anomalías en los repositorios de contexto o activos.

---

## Pruebas Unitarias Implementadas

Se actualizaron y ampliaron las pruebas unitarias en `Backend/src/test/java/org/bulbaseros/service/IncidentServiceTest.java`:

1. **`shouldCreateIncidentWithAssetsAndContextSuccessfully`:**
   - Verifica la orquestación exitosa de alta de incidencia donde `associateNearbyAssets` y `captureAndSaveContext` retornan datos válidos.
   - Comprueba que el objeto `IncidentResponse` retornado contiene los activos y las observaciones contextuales correctamente mapeados.
2. **`shouldCreateIncidentWithFallbackWhenAssetOrContextFails` (RF-15):**
   - Simula fallos y excepciones no controladas tanto en la búsqueda de activos como en el conector externo de contexto.
   - Valida que la incidencia se persiste sin interrupción, devolviendo listas vacías en `urbanAssets` y `urbanContext` y garantizando degradación controlada.
3. **`shouldGetIncidentByIdWithAssetsAndContext` (RF-5, RF-10):**
   - Comprueba que la consulta por ID enriquece el DTO de respuesta con los activos vinculados (incluyendo estado de confirmación `isConfirmed`) y las métricas de tráfico y meteorología.
4. **`shouldGetIncidentByIdWithGracefulDegradationWhenSubservicesFail`:**
   - Comprueba que ante excepciones en los servicios auxiliares durante la consulta por ID, la información básica de la incidencia se entrega íntegra con listas vacías en los campos agregados.

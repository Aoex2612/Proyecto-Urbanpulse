# Tarea 3: Lógica de Negocio de Creación Enriquecida y Consulta Paginada en `IncidentService`

**Feature:** `004-reporte-y-seguimiento-de-incidencias`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-1, RF-4, RF-5, RF-6, RF-7, RF-8, RF-10, RF-12, RF-13, RF-14, RF-15, RF-18  
**Requisitos no funcionales:** Tolerancia a fallos, degradación controlada, aislamiento por ciudadano emisor  

---

## 1. Resumen de la Tarea

En esta tarea se ha implementado y testeado la lógica de negocio central en `IncidentService`, integrando la validación espacial de coordenadas municipales, el enriquecimiento territorial automático asistido por `MalagaOpenDataService` y la consulta paginada y filtrada para el seguimiento de reportes ciudadanos propios:

1. **Inyección de `MalagaOpenDataService` en `IncidentService`:**
   - Incorporación del componente de datos abiertos mediante inyección de dependencias en el constructor principal con documentación Javadoc completa.

2. **Validación de Límites Municipales y Alta Enriquecida en `createIncident`:**
   - Validación estricta de las coordenadas geográficas de la solicitud: si la latitud o longitud son nulas o se encuentran fuera del término municipal de Málaga (latitud `[36.5000, 36.9000]`, longitud `[-4.7000, -4.2000]`), se arroja de forma inmediata una excepción `ResponseStatusException(HttpStatus.BAD_REQUEST, "Las coordenadas se encuentran fuera del término municipal de Málaga")` (RF-5).
   - Enriquecimiento territorial asistido: si los campos `address`, `district` o `neighborhood` no vienen especificados (o vienen vacíos), se consulta `malagaOpenDataService.resolveLocation(latitude, longitude)` autocompletando dichos atributos en la entidad con los valores obtenidos o con `null` si opera la degradación controlada (RF-1, RF-2, RF-6, RF-7, RF-8, RF-10).
   - Mantenimiento inalterado del estado inicial por defecto `REPORTED` y prioridad `MEDIUM` (RF-4).

3. **Consulta Paginada de Avisos Propios en `getMyIncidents`:**
   - Implementación del método `PageResponse<IncidentResponse> getMyIncidents(String userEmail, EstadoIncidencia status, Pageable pageable)`:
     - Validación y normalización del correo electrónico del ciudadano; si no existe en base de datos, arroja `ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado")`.
     - Aplicación de ordenación cronológica descendente por fecha de creación por defecto (`Sort.by(Sort.Direction.DESC, "createdAt")`) cuando el `Pageable` no define una ordenación explícita (RF-13).
     - Consulta condicional: delegación en `incidenciaRepository.findByCitizenId(...)` si el estado es `null` ("TODOS"), o en `incidenciaRepository.findByCitizenIdAndStatus(...)` si se proporciona un estado específico (RF-12, RF-14).
     - Transformación del resultado `Page<Incidencia>` a `PageResponse<IncidentResponse>` mediante `incidentMapper.toPageResponse(page)`.

4. **Geocodificación Inversa Directa en `reverseGeocode`:**
   - Implementación del método `ReverseGeocodeResponse reverseGeocode(Double latitude, Double longitude)`:
     - Validación preliminar del término municipal de Málaga arrojando `ResponseStatusException(HttpStatus.BAD_REQUEST, "Las coordenadas están fuera de Málaga")` ante coordenadas nulas o fuera de límites.
     - Delegación transparente en `malagaOpenDataService.resolveLocation(latitude, longitude)` retornando el DTO territorial.

5. **Ampliación de la Suite de Pruebas Unitarias (`IncidentServiceTest`):**
   - Incorporación de 12 nuevos casos de prueba unitaria con Mockito y JUnit 5 verificando exhaustivamente:
     - Alta exitosa con autocompletado territorial de dirección, barrio y distrito desde `MalagaOpenDataService`.
     - Alta exitosa con degradación controlada (campos territoriales nulos) ante indisponibilidad externa.
     - Rechazo con 400 Bad Request ante coordenadas fuera de Málaga o coordenadas nulas en creación.
     - Consulta de incidencias propias sin filtro de estado (`createdAt DESC`).
     - Consulta de incidencias propias filtradas por estado del ciclo de vida.
     - Gestión de error 404 Not Found cuando el usuario emisor no existe.
     - Validación de argumentos nulos o en blanco en `getMyIncidents`.
     - Resolución exitosa de geocodificación inversa con coordenadas válidas.
     - Rechazo de geocodificación inversa con 400 Bad Request ante coordenadas nulas o fuera de límites.
     - Paginación por defecto ante parámetro `Pageable` nulo.
   - Total de pruebas en `IncidentServiceTest`: 27 tests pasando satisfactoriamente. Total en la suite global de Backend: 253 tests pasando (100% verde).

---

## 2. Archivos Modificados / Creados

- `Backend/src/main/java/org/bulbaseros/service/IncidentService.java`: Actualización con inyección de dependencias, validación espacial, enriquecimiento en creación, implementación de `getMyIncidents` y `reverseGeocode`.
- `Backend/src/test/java/org/bulbaseros/service/IncidentServiceTest.java`: Inyección de mock de `MalagaOpenDataService` y adición de 12 nuevos métodos de prueba unitaria.
- `Backend/src/test/java/org/bulbaseros/integration/IncidentPostgresIntegrationTest.java`: Ajuste de coordenadas del caso de prueba a coordenadas válidas dentro del término municipal de Málaga.
- `features/004-reporte-y-seguimiento-de-incidencias/tasks.md`: Actualización de estado de la Tarea 3 a completada `[x]`.
- `docs/004-reporte-y-seguimiento-de-incidencias/03-tarea.md`: Documentación técnica de la tarea.

# Tarea 4: Endpoints REST y Seguridad Perimetral en Backend

**Feature:** `004-reporte-y-seguimiento-de-incidencias`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-1, RF-2, RF-5, RF-12, RF-14, RF-15, RF-16, RF-18  
**Requisitos no funcionales:** Seguridad perimetral, RBAC, paginación controlada, validación de entradas  

---

## 1. Resumen de la Tarea

En esta tarea se han expuesto y asegurado los nuevos endpoints REST en la capa de controladores y configuración de seguridad web de UrbanPulse, permitiendo el seguimiento paginado y filtrado de incidencias del ciudadano autenticado, así como la geocodificación inversa directa dentro del término municipal de Málaga:

1. **Endpoint de Seguimiento de Avisos Propios (`GET /api/incidents/my-incidents`):**
   - Implementado en `IncidentController.java` conforme al contrato de `docs/api.md`.
   - Soporte para parámetros de consulta:
     - `status` (opcional): si no se informa, viene vacío o equivale a `"TODOS"` (sin distinguir mayúsculas/minúsculas), se pasa `null` a la capa de servicio para no aplicar filtro de estado. Si viene un valor válido de `EstadoIncidencia`, se parsea adecuadamente; si no coincide con ningún estado válido, arroja `ResponseStatusException(HttpStatus.BAD_REQUEST)` (RF-14).
     - `page` (opcional, por defecto `0`, mínimo `0`): validado para rechazar índices negativos con HTTP 400 Bad Request.
     - `size` (opcional, por defecto `10`, rango `[1, 50]`): validado para rechazar tamaños fuera de rango con HTTP 400 Bad Request.
   - Extracción segura del correo electrónico del ciudadano emisor a partir del token JWT inyectado en `Authentication authentication`, garantizando que el usuario solo pueda consultar sus propias incidencias y nunca manipular el identificador en los parámetros de la petición (RF-12, RF-18).
   - Invocación a `incidentService.getMyIncidents(userEmail, estadoFilter, pageable)` retornando `ResponseEntity.ok(pageResponse)`.

2. **Endpoint de Geocodificación Inversa Territorial (`GET /api/incidents/geocode/reverse`):**
   - Implementado en `IncidentController.java`.
   - Acepta parámetros de consulta `@RequestParam Double latitude` y `@RequestParam Double longitude`.
   - Delega en `incidentService.reverseGeocode(latitude, longitude)`, el cual valida que las coordenadas pertenezcan estrictamente al término municipal de Málaga (`[36.5000, 36.9000]`, `[-4.7000, -4.2000]`), respondiendo HTTP 400 Bad Request si son nulas o exceden dichos límites.
   - Retorna `ResponseEntity.ok(reverseGeocodeResponse)` con los campos `address`, `district` y `neighborhood` (o nulos ante degradación controlada) (RF-1, RF-2, RF-5).

3. **Seguridad Perimetral en `SecurityConfig.java`:**
   - Incorporación explícita de reglas de autorización perimetral para `GET /api/incidents/my-incidents` y `GET /api/incidents/geocode/reverse` requiriendo autenticación previa (`.authenticated()`).
   - Peticiones anónimas sin cabecera `Authorization: Bearer <token>` son rechazadas de forma uniforme por `JwtAuthenticationEntryPoint` respondiendo HTTP 401 Unauthorized estructurado con `ErrorResponse`.

4. **Batería de Pruebas Unitarias y de Integración:**
   - Creación de `Backend/src/test/java/org/bulbaseros/controller/IncidentControllerTest.java` con 17 casos de prueba:
     - `GET /api/incidents/my-incidents` con usuario autenticado (`@WithMockUser` y JWT real): responde 200 OK con estructura canónica `PageResponse`.
     - `GET /api/incidents/my-incidents` filtrando por `status`: delega en el servicio con el estado filtrado correspondiente.
     - `GET /api/incidents/my-incidents` con `status=TODOS`: pasa `null` a la capa de servicio.
     - `GET /api/incidents/my-incidents` sin token JWT: responde 401 Unauthorized.
     - `GET /api/incidents/my-incidents` con `status` inválido o paginación fuera de rango (`page < 0`, `size < 1`, `size > 50`): responde 400 Bad Request.
     - `GET /api/incidents/geocode/reverse` con coordenadas válidas dentro de Málaga: responde 200 OK con `ReverseGeocodeResponse`.
     - `GET /api/incidents/geocode/reverse` con coordenadas fuera de límites o nulas: responde 400 Bad Request.
     - `GET /api/incidents/geocode/reverse` sin token JWT: responde 401 Unauthorized.
     - `GET /api/incidents/{id}` con identificador existente (200 OK) e inexistente (404 Not Found).
     - `POST /api/incidents` con coordenadas fuera de Málaga: responde 400 Bad Request.
     - Invocaciones unitarias directas sobre los métodos del controlador validando lógica de filtrado, paginación y manejo de excepciones.
   - Ampliación de `Backend/src/test/java/org/bulbaseros/controller/IncidentControllerSecurityTest.java` con verificaciones perimetrales de acceso anónimo frente a `my-incidents` y `geocode/reverse`.
   - Resultados de ejecución de Maven Surefire: **272 tests ejecutados, 0 fallos, 0 errores, 1 skipped (100% verde)**.

---

## 2. Archivos Modificados / Creados

- `Backend/src/main/java/org/bulbaseros/controller/IncidentController.java`: Adición de endpoints `GET /api/incidents/my-incidents` y `GET /api/incidents/geocode/reverse` con Javadoc completo.
- `Backend/src/main/java/org/bulbaseros/config/SecurityConfig.java`: Configuración de seguridad perimetral para proteger los nuevos endpoints exigiendo autenticación.
- `Backend/src/test/java/org/bulbaseros/controller/IncidentControllerTest.java`: Nueva suite de pruebas unitarias y de integración sobre los endpoints y contratos REST.
- `Backend/src/test/java/org/bulbaseros/controller/IncidentControllerSecurityTest.java`: Adición de pruebas de rechazo 401 para peticiones no autenticadas a los nuevos endpoints.
- `features/004-reporte-y-seguimiento-de-incidencias/tasks.md`: Marcado de la Tarea 4 como completada `[x]`.
- `docs/004-reporte-y-seguimiento-de-incidencias/04-tarea.md`: Documentación técnica de la tarea.

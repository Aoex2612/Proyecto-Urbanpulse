# Tarea 6: Endpoints REST y Seguridad RBAC para Gestión de Activos

## Resumen de la Tarea

En esta tarea se han implementado y asegurado los endpoints REST necesarios para la gestión formal de activos urbanos asociados a incidencias (`RF-3`, `RF-4`, `RF-7`).

Se garantiza el cumplimiento de los controles de acceso basados en roles (**RBAC**), permitiendo únicamente a operadores municipales (`OPERATOR`) y administradores del sistema (`ADMIN`) realizar acciones de confirmación manual de activos contiguos sugeridos o de desvinculación/eliminación de activos asociados erróneamente. Usuarios con rol ciudadano (`CITIZEN`) o peticiones no autenticadas son bloqueadas por las capas de seguridad perimetral y metodológica respondiendo con códigos estándar `403 Forbidden` y `401 Unauthorized` respectivamente.

---

## Modificaciones Implementadas

### 1. Endpoints REST en `IncidentController.java`
- **Ubicación:** `Backend/src/main/java/org/bulbaseros/controller/IncidentController.java`
- **Inyección de dependencia:** Se integró `UrbanAssetService urbanAssetService` como dependencia inyectable opcional, manteniendo compatibilidad con constructores anteriores.
- **`POST /api/incidents/{incidentId}/assets/{assetId}/confirm` (RF-3, RF-7):**
  - Anotado con `@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")`.
  - Delega en `urbanAssetService.confirmAssetAssociation(incidentId, assetId)`.
  - Retorna `200 OK` con el DTO `IncidentAssetResponse` con `isConfirmed = true`.
  - Si la asociación no existe, arroja `ResponseStatusException(HttpStatus.NOT_FOUND)` gestionada globalmente devolviendo `404 Not Found`.
- **`DELETE /api/incidents/{incidentId}/assets/{assetId}` (RF-4):**
  - Anotado con `@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")`.
  - Delega en `urbanAssetService.removeAssetAssociation(incidentId, assetId)`.
  - Retorna `200 OK` tras completar la desvinculación.
  - Si la asociación no existe, arroja `ResponseStatusException(HttpStatus.NOT_FOUND)` devolviendo `404 Not Found`.

### 2. Configuración Perimetral de Seguridad en `SecurityConfig.java`
- **Ubicación:** `Backend/src/main/java/org/bulbaseros/config/SecurityConfig.java`
- Se actualizaron las reglas de autorización perimetral `authorizeHttpRequests`:
  - `POST /api/incidents/*/assets/*/confirm` restringido a roles `OPERATOR` y `ADMIN`.
  - `DELETE /api/incidents/*/assets/*` restringido a roles `OPERATOR` y `ADMIN`.
- Se mantiene la doble capa de protección (filtro perimetral de URL y seguridad a nivel de método con `@PreAuthorize`).

### 3. Manejo de Excepciones Estructurado en `GlobalExceptionHandler.java`
- Las excepciones `ResponseStatusException` con código `404 Not Found` generadas por `UrbanAssetService` (cuando una incidencia o activo no están asociados) se serializan mediante `GlobalExceptionHandler.handleResponseStatusException` retornando una estructura estándar `ErrorResponse` (status, error, message, path, timestamp).
- Las violaciones de autorización RBAC (`AccessDeniedException`) se manejan retornando `403 Forbidden` estructurado mediante `GlobalExceptionHandler.handleAccessDeniedException`.

---

## Pruebas de Integración y MockMvc

### 1. `IncidentControllerSecurityTest.java`
Se añadieron tests de integración con `MockMvc` cubriendo todos los escenarios exigidos:
- **Confirmación de activo (`POST /api/incidents/{id}/assets/{assetId}/confirm`):**
  - `401 Unauthorized` para peticiones anónimas sin token.
  - `403 Forbidden` para peticiones autenticadas con rol `CITIZEN`.
  - `200 OK` para rol `OPERATOR` retornando el activo con `isConfirmed = true`.
  - `200 OK` para rol `ADMIN`.
  - `404 Not Found` cuando la asociación de activo indicada no existe (cumpliendo `RF-7`).
- **Desvinculación de activo (`DELETE /api/incidents/{id}/assets/{assetId}`):**
  - `401 Unauthorized` para peticiones anónimas sin token.
  - `403 Forbidden` para peticiones con rol `CITIZEN`.
  - `200 OK` para rol `OPERATOR`.
  - `200 OK` para rol `ADMIN`.
  - `404 Not Found` cuando la asociación no existe en el repositorio.

### 2. `IncidentControllerTest.java`
Se agregaron pruebas unitarias directas:
- Delegación correcta en `urbanAssetService.confirmAssetAssociation` y retorno `200 OK`.
- Delegación correcta en `urbanAssetService.removeAssetAssociation` y retorno `200 OK`.
- Manejo defensivo con `501 Not Implemented` si el controlador se instancia sin `UrbanAssetService`.

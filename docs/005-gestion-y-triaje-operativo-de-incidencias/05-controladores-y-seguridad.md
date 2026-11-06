# Tarea 5: Controladores REST, Seguridad RBAC y Documentación OpenAPI en Backend

**Feature:** `005-gestion-y-triaje-operativo-de-incidencias`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-1 al RF-24  

---

## 1. Resumen de la Tarea

En esta tarea se ha implementado la capa de controladores REST y seguridad RBAC en el Backend para la orquestación del triaje operativo, asignaciones multidepartamentales, catálogo de técnicos y muro de comentarios, protegiendo todos los accesos mediante control granular de roles y validación de contratos JSON con Bean Validation:

1. **`IncidentTriageController` (`Backend/src/main/java/org/bulbaseros/controller/IncidentTriageController.java`):**
   - Inyección de servicios de negocio: `IncidentTriageService`, `AssignmentService` y `CommentService`.
   - Extracción segura del usuario autenticado a partir de `Authentication.getName()` (email emisor).
   - Documentación exhaustiva en español con Javadoc en clase, constructores y métodos (`@param`, `@return`, `@throws`).
   - **Endpoints de cálculo y actualización de prioridad:**
     * `POST /api/incidents/{id}/priority/calculate`: Protegido con `@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")`. Recibe `@Valid PriorityCalculationRequest` y retorna HTTP 200 OK con `PriorityCalculationResponse` (RF-1, RF-3).
     * `PUT /api/incidents/{id}/priority`: Protegido con `@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")`. Recibe `@Valid UpdatePriorityRequest` y retorna HTTP 200 OK con `IncidentResponse` (RF-2, RF-3).
   - **Endpoints de transiciones de ciclo de vida:**
     * `POST /api/incidents/{id}/validate`: Protegido con `@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")`. Recibe `@Valid @RequestBody(required = false) ValidateIncidentRequest` y retorna HTTP 200 OK con `IncidentResponse` (RF-4).
     * `POST /api/incidents/{id}/reject`: Protegido con `@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")`. Recibe `@Valid RejectIncidentRequest` (motivo &ge; 5 caracteres) y retorna HTTP 200 OK con `IncidentResponse` (RF-5, RF-6).
     * `POST /api/incidents/{id}/reevaluate`: Protegido con `@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")`. Recibe `@Valid ReevaluateIncidentRequest` y retorna HTTP 200 OK con `IncidentResponse` (RF-7).
     * `POST /api/incidents/{id}/start-work`: Protegido con `@PreAuthorize("hasRole('TECHNICIAN')")`. Retorna HTTP 200 OK con `IncidentResponse` en estado `IN_PROGRESS` (RF-13, RF-14).
     * `POST /api/incidents/{id}/resolve`: Protegido con `@PreAuthorize("hasRole('TECHNICIAN')")`. Recibe `@Valid @RequestBody(required = false) ResolveIncidentRequest` y retorna HTTP 200 OK con `IncidentResponse` en estado `RESOLVED` (RF-15, RF-16).
     * `POST /api/incidents/{id}/close`: Protegido con `@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")`. Retorna HTTP 200 OK con `IncidentResponse` en estado `CLOSED` (RF-17, RF-19).
     * `POST /api/incidents/{id}/reopen`: Protegido con `@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")`. Recibe `@Valid ReopenIncidentRequest` (motivo &ge; 5 caracteres) y retorna HTTP 200 OK con `IncidentResponse` en estado `REOPENED` (RF-18, RF-19).
   - **Endpoints de asignaciones operativas:**
     * `POST /api/incidents/{id}/assignments`: Protegido con `@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")`. Recibe `@Valid AssignIncidentRequest` y retorna HTTP 200 OK con `List<AssignmentResponse>` (RF-8, RF-9, RF-10).
     * `POST /api/incidents/{id}/assignments/deactivate`: Protegido con `@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")`. Recibe `@Valid DeactivateAssignmentRequest` y retorna HTTP 200 OK con `List<AssignmentResponse>` (RF-11, RF-12).
   - **Endpoints de muro de comentarios y notas:**
     * `GET /api/incidents/{id}/comments`: Protegido con `@PreAuthorize("isAuthenticated()")`. Retorna HTTP 200 OK con `List<CommentResponse>` filtrada por el servicio según el rol (RF-20, RF-21).
     * `POST /api/incidents/{id}/comments`: Protegido con `@PreAuthorize("hasAnyRole('OPERATOR', 'TECHNICIAN', 'ADMIN')")`. Recibe `@Valid CreateCommentRequest` y retorna HTTP 201 Created con `CommentResponse` (RF-20).
     * `PUT /api/incidents/{id}/comments/{commentId}`: Protegido con `@PreAuthorize("isAuthenticated()")`. Recibe `@Valid UpdateCommentRequest` y retorna HTTP 200 OK con `CommentResponse` tras verificar autoría o rol admin en servicio (RF-22, RF-23).

2. **`DepartmentController` (`Backend/src/main/java/org/bulbaseros/controller/DepartmentController.java`):**
   - Inyección de dependencias: `DepartamentoRepository`, `UserRepository`, `DepartmentMapper`.
   - `GET /api/departments`: Protegido con `@PreAuthorize("isAuthenticated()")`. Retorna el catálogo completo ordenado alfabéticamente en HTTP 200 OK con `List<DepartmentResponse>`.
   - `GET /api/departments/{id}/technicians`: Protegido con `@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")`. Valida la existencia del departamento (HTTP 404 si no existe) y recupera los técnicos adscritos con rol `TECHNICIAN`, retornando HTTP 200 OK con `List<UserSummaryResponse>`.

3. **Seguridad RBAC y Configuración Central (`SecurityConfig` y `GlobalExceptionHandler`):**
   - Habilitación de `@EnableMethodSecurity` en `SecurityConfig` para soportar anotaciones `@PreAuthorize` a nivel de método.
   - Configuración de reglas perimetrales en `SecurityFilterChain`:
     * Rutas `/api/departments/**`: requieren autenticación previa (`.authenticated()`).
     * Rutas `/api/incidents/**`: autenticadas, delegando permisos específicos en `@PreAuthorize`.
   - Adaptación en `GlobalExceptionHandler`:
     * Adición de manejador para `AccessDeniedException` retornando HTTP 403 Forbidden estructurado (`ErrorResponse`) ante fallos de autorización en métodos con `@PreAuthorize`.
     * Soporte de `IllegalStateException` en el manejador de argumentos de estado retornando HTTP 400 Bad Request estructurado.

---

## 2. Cobertura de Pruebas de Integración con MockMvc

Se han creado suites de prueba exhaustivas con `@SpringBootTest`, `@AutoConfigureMockMvc` y `@WithMockUser`:

### `DepartmentControllerTest` (7 tests)
- `GET /api/departments` sin token &rarr; 401 Unauthorized.
- `GET /api/departments` con rol `CITIZEN` &rarr; 200 OK y array serializado.
- `GET /api/departments/{id}/technicians` sin token &rarr; 401 Unauthorized.
- `GET /api/departments/{id}/technicians` con rol `CITIZEN` &rarr; 403 Forbidden.
- `GET /api/departments/{id}/technicians` con rol `TECHNICIAN` &rarr; 403 Forbidden.
- `GET /api/departments/{id}/technicians` con rol `OPERATOR` y departamento inexistente &rarr; 404 Not Found.
- `GET /api/departments/{id}/technicians` con roles `OPERATOR` y `ADMIN` &rarr; 200 OK con lista de `UserSummaryResponse`.

### `IncidentTriageControllerTest` (23 tests)
- **Cálculo asistido de prioridad:** 401 sin token, 403 con `CITIZEN`, 400 con body incompleto (`@Valid`), 200 OK con `OPERATOR`.
- **Fijación de prioridad:** 401 sin token, 403 con `CITIZEN`, 400 con prioridad nula, 200 OK con `ADMIN`.
- **Validación inicial:** 401 sin token, 403 con `CITIZEN`, 200 OK con `OPERATOR`.
- **Rechazo de incidencia:** 401 sin token, 403 con `CITIZEN`, 400 con motivo &lt; 5 caracteres, 200 OK con `OPERATOR`.
- **Reevaluación de rechazada:** 400 con motivo corto, 200 OK con `OPERATOR`.
- **Inicio de trabajo técnico:** 401 sin token, 403 con `OPERATOR`, 200 OK con `TECHNICIAN`.
- **Resolución técnica:** 403 con `OPERATOR`, 200 OK con `TECHNICIAN`.
- **Cierre administrativo:** 403 con `TECHNICIAN`, 200 OK con `OPERATOR`.
- **Reapertura de incidencia:** 400 con motivo corto, 200 OK con `ADMIN`.
- **Asignaciones multidepartamento:** 403 con `CITIZEN`, 400 con lista departamental vacía, 200 OK con `OPERATOR`.
- **Desactivación de asignaciones:** 200 OK con `OPERATOR`.
- **Consulta de comentarios:** 401 sin token, 200 OK autenticado (`CITIZEN`).
- **Creación de comentarios:** 401 sin token, 403 con `CITIZEN`, 400 con comentario en blanco, 201 Created con `TECHNICIAN`.
- **Edición de comentarios:** 400 con contenido en blanco, 200 OK autenticado.

---

## 3. Estado de Ejecución de la Suite Global

- **Tests ejecutados:** 458 tests unitarios y de integración.
- **Fallos:** 0.
- **Errores:** 0.
- **Omitidos:** 1 (test de integración con base de datos real PostgreSQL `@EnabledIfPostgresAvailable`, que se omite limpiamente si Docker no está levantado según la directriz `AGENTS.md`).
- **Resultado:** BUILD SUCCESS. Cobertura JaCoCo regenerada exitosamente.

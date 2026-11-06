# 005 — Gestión y Triaje Operativo de Incidencias — Plan

_Cómo se implementa lo descrito en `spec.md`. Respeta escrupulosamente los principios de `constitution/` y referencia la arquitectura decidida en `docs/ADR/ADR-07-gestion-ciclo-vida-asignacion-multidepartamental.md`._

## Enfoque

Implementar un flujo de triaje y gestión operativa desacoplado, modular y fuertemente auditado. La capa backend centraliza las transiciones de ciclo de vida (`REPORTED` → `VALIDATED`/`REJECTED` → `ASSIGNED` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`/`REOPENED`) y la gestión de asignaciones multidepartamentales dentro de servicios transaccionales atómicos (`@Transactional`), registrando de forma inmutable cada cambio en `status_changes`.
Se introducen servicios de dominio específicos para:
1. El cálculo determinista de prioridad asistida según factores de vía y seguridad.
2. La validación de correspondencia atómica entre departamentos activos y técnicos asignados con rol `TECHNICIAN`.
3. La segregación estricta de notas técnicas internas frente a comentarios públicos en base al rol autenticado del usuario (RBAC), asegurando que los ciudadanos solo accedan a comentarios públicos de sus propias incidencias.
4. Permisos universales de ciclo de vida para el rol `ADMIN` (pudiendo ejecutar `start-work` y `resolve` sin requerir asignación técnica previa) y segregación estricta de visualización para `TECHNICIAN` (quien solo visualiza las incidencias con asignación técnica activa `is_active = true`).

En el frontend, se amplía el panel operativo (`OperatorPanelPage`) y los componentes de detalle de incidencias con controles ergonómicos de triaje, calculadora de prioridad asistida, selector multidepartamental en cascada, muro colaborativo de comentarios con edición controlada y adaptación de vistas y acciones según el rol del usuario autenticado.

La documentación de la API se autogenera mediante SpringDoc OpenAPI a partir de las firmas y anotaciones de los controladores REST.

---

## Implementación

### 1. Modelo de Datos y Persistencia (Backend)
- **Modificación en entidad `ComentariosIncidencias` (`Backend/src/main/java/org/bulbaseros/entity/ComentariosIncidencias.java`):**
  - Añadir campo `updatedAt` (`OffsetDateTime`) para soportar la trazabilidad de modificaciones en comentarios (RF-22).
- **Creación de Repositorios Spring Data JPA:**
  - `Backend/src/main/java/org/bulbaseros/repository/TareasRepository.java`:
    - `List<Tareas> findByIncidentIdAndIsActiveTrue(Long incidentId)`
    - `boolean existsByIncidentIdAndTechnicianIdAndIsActiveTrue(Long incidentId, Long technicianId)`
    - `List<Tareas> findByIncidentId(Long incidentId)`
    - `List<Tareas> findByIncidentIdAndDepartmentIdAndIsActiveTrue(Long incidentId, Long departmentId)`
  - `Backend/src/main/java/org/bulbaseros/repository/ComentariosIncidenciasRepository.java`:
    - `List<ComentariosIncidencias> findByIncidentIdOrderByCreatedAtAsc(Long incidentId)`
    - `List<ComentariosIncidencias> findByIncidentIdAndIsInternalFalseOrderByCreatedAtAsc(Long incidentId)`
  - `Backend/src/main/java/org/bulbaseros/repository/DepartamentoRepository.java`:
    - `List<Departamento> findAllByOrderByNameAsc()`
  - **Actualización de `UserRepository` (`Backend/src/main/java/org/bulbaseros/repository/UserRepository.java`):**
    - `List<Usuario> findByRoleAndDepartmentId(RolUsuario role, Long departmentId)` (restringido estrictamente a `RolUsuario.TECHNICIAN`).

### 2. DTOs y Mappers (Backend)
- **Creación de DTOs en `Backend/src/main/java/org/bulbaseros/dto/`:**
  - `SafetyRiskLevel.java` (Enum: `NONE`, `LOW`, `HIGH`).
  - `PriorityCalculationRequest.java`: `Boolean streetCut`, `SafetyRiskLevel safetyRisk`.
  - `PriorityCalculationResponse.java`: `PrioridadIncidencia suggestedPriority`, `String priorityReason`.
  - `UpdatePriorityRequest.java`: `PrioridadIncidencia priority`, `String priorityReason`.
  - `ValidateIncidentRequest.java`: `String reason`.
  - `RejectIncidentRequest.java`: `String reason` (anotado con `@NotBlank`, `@Size(min=5)`).
  - `ReevaluateIncidentRequest.java`: `EstadoIncidencia targetStatus` (validado para permitir únicamente `REPORTED` o `VALIDATED`), `String reason` (`@NotBlank`, `@Size(min=5)`).
  - `AssignIncidentRequest.java`: `List<Long> departmentIds` (`@NotEmpty`), `List<Long> technicianIds`.
  - `DeactivateAssignmentRequest.java`: `List<Long> departmentIds`, `List<Long> technicianIds`.
  - `ResolveIncidentRequest.java`: `String resolutionNotes`.
  - `ReopenIncidentRequest.java`: `String reason` (`@NotBlank`, `@Size(min=5)`).
  - `CreateCommentRequest.java`: `String comment` (`@NotBlank`), `Boolean isInternal`.
  - `UpdateCommentRequest.java`: `String comment` (`@NotBlank`).
  - `AssignmentResponse.java`: `Long id`, `Long departmentId`, `String departmentName`, `Long technicianId`, `String technicianName`, `Long assignedById`, `String assignedByName`, `Boolean isActive`, `OffsetDateTime assignedAt`.
  - `CommentResponse.java`: `Long id`, `Long incidentId`, `Long authorId`, `String authorName`, `String authorRole`, `String comment`, `Boolean isInternal`, `OffsetDateTime createdAt`, `OffsetDateTime updatedAt`.
  - `DepartmentResponse.java`: `Long id`, `String name`, `String description`.
  - `UserSummaryResponse.java`: `Long id`, `String fullName`, `String email`, `RolUsuario role`, `Long departmentId`.
- **Mappers en `Backend/src/main/java/org/bulbaseros/mappers/`:**
  - `AssignmentMapper.java`: Conversión bidireccional entre entidad `Tareas` y `AssignmentResponse`.
  - `CommentMapper.java`: Conversión entre `ComentariosIncidencias` y `CommentResponse`.
  - `DepartmentMapper.java`: Conversión entre `Departamento` y `DepartmentResponse`.

### 3. Capa de Servicios de Negocio (Backend)
- **`Backend/src/main/java/org/bulbaseros/service/PriorityService.java`:**
  - Lógica pura determinista para cálculo asistido según la matriz RF-1 y redacción de justificación estándar.
- **`Backend/src/main/java/org/bulbaseros/service/AssignmentService.java`:**
  - Asignación atómica multidepartamental y técnica dentro de transacción `@Transactional` (RF-8, RF-9).
  - Validación atómica: ningún técnico puede asignarse si su departamento no está activo simultáneamente en la incidencia (RF-10), evitando condiciones de carrera.
  - Desactivación lógica de asignaciones (`is_active = false`) (RF-11).
  - Regla de negocio: reversión automática a `VALIDATED` si se desactivan todas las asignaciones departamentales activas (RF-12).
  - Inactivación masiva de asignaciones al cerrar (`CLOSED`) o rechazar (`REJECTED`) (RF-5, RF-17).
- **`Backend/src/main/java/org/bulbaseros/service/CommentService.java`:**
  - Registro de notas internas y públicas con verificación de estado no terminal (RF-20, RF-24).
  - Consulta filtrada y autorizada:
    - Personal municipal (`OPERATOR`, `TECHNICIAN`, `ADMIN`): consulta todos los comentarios (internos y públicos).
    - Ciudadano (`CITIZEN`): consulta únicamente comentarios con `is_internal = false` y solo si es el creador de la incidencia; en caso contrario, deniega con 403 Forbidden.
  - Edición con verificación estricta de autoría original (`comment.getAuthor().getId().equals(currentUser.getId())`) o rol `ADMIN`, sellado de `updatedAt` y preservación del autor (RF-22, RF-23).
- **`Backend/src/main/java/org/bulbaseros/service/IncidentTriageService.java`:**
  - Orquestación de transiciones de estado con guardas estrictas de rol y precondiciones de estado (`validateIncident`, `rejectIncident`, `reevaluateIncident`, `startIntervention`, `resolveIncident`, `closeIncident`, `reopenIncident`, `updatePriority`).
  - Permisos universales de ciclo de vida para `ADMIN`: tanto `startIntervention` como `resolveIncident` permiten su ejecución por usuarios con rol `ADMIN` directamente o por usuarios `TECHNICIAN` que posean asignación activa (`is_active = true`) en la incidencia (RF-13, RF-15).
  - Validación de estado destino en reevaluación: restringe explícitamente a `REPORTED` o `VALIDATED` (arrojando `IllegalArgumentException` o 400 Bad Request si es cualquier otro estado).
  - Inserción transaccional obligatoria de registros inmutables en `CambiosEstadoRepository`.
- **`Backend/src/main/java/org/bulbaseros/service/IncidentService.java` / `IncidentController.java`:**
  - Segregación técnica en `GET /api/incidents`: cuando el usuario autenticado posea rol `TECHNICIAN`, la consulta retornará únicamente las incidencias donde dicho técnico figure con asignación activa (`is_active = true`) (RF-25). Los roles `OPERATOR` y `ADMIN` continúan visualizando todas las incidencias.

### 4. Capa de Controladores y Seguridad (Backend)
- **`Backend/src/main/java/org/bulbaseros/controller/IncidentTriageController.java`:**
  - `POST /api/incidents/{id}/priority/calculate` (`hasAnyRole('OPERATOR', 'ADMIN')`)
  - `PUT /api/incidents/{id}/priority` (`hasAnyRole('OPERATOR', 'ADMIN')`)
  - `POST /api/incidents/{id}/validate` (`hasAnyRole('OPERATOR', 'ADMIN')`)
  - `POST /api/incidents/{id}/reject` (`hasAnyRole('OPERATOR', 'ADMIN')`)
  - `POST /api/incidents/{id}/reevaluate` (`hasAnyRole('OPERATOR', 'ADMIN')`)
  - `POST /api/incidents/{id}/assignments` (`hasAnyRole('OPERATOR', 'ADMIN')`)
  - `POST /api/incidents/{id}/assignments/deactivate` (`hasAnyRole('OPERATOR', 'ADMIN')`) — Desactivación lógica explícita en lugar de borrado físico.
  - `POST /api/incidents/{id}/start-work` (`hasAnyRole('TECHNICIAN', 'ADMIN')`)
  - `POST /api/incidents/{id}/resolve` (`hasAnyRole('TECHNICIAN', 'ADMIN')`)
  - `POST /api/incidents/{id}/close` (`hasAnyRole('OPERATOR', 'ADMIN')`)
  - `POST /api/incidents/{id}/reopen` (`hasAnyRole('OPERATOR', 'ADMIN')`)
  - `GET /api/incidents/{id}/comments` (`isAuthenticated()`) — Autorización granular y filtrado en servicio.
  - `POST /api/incidents/{id}/comments` (`hasAnyRole('OPERATOR', 'TECHNICIAN', 'ADMIN')`)
  - `PUT /api/incidents/{id}/comments/{commentId}` (`isAuthenticated()`) — Verificación de autoría o `ADMIN` en servicio.
- **`Backend/src/main/java/org/bulbaseros/controller/DepartmentController.java`:**
  - `GET /api/departments` (`isAuthenticated()`)
  - `GET /api/departments/{id}/technicians` (`hasAnyRole('OPERATOR', 'ADMIN')`) — Filtra exclusivamente usuarios con rol `TECHNICIAN`.

### 5. Capa Cliente (Frontend)
- **Servicio API (`frontend/src/services/triageService.js`):**
  - Métodos HTTP con Axios para cálculo de prioridad, transiciones de estado, asignaciones multidepartamentales, carga de técnicos por departamento y muro de comentarios.
- **Enrutamiento y Vistas:**
  - `frontend/src/App.jsx` y `frontend/src/pages/HomePage.jsx`: Habilitación de ruta `/operator-panel` para `allowedRoles={['OPERATOR', 'ADMIN', 'TECHNICIAN']}`.
  - `frontend/src/components/Triage/PriorityAssistantModal.jsx`: Modal interactivo para indicar corte de vía y nivel de seguridad, mostrar cálculo recomendado y permitir ajuste del operador antes de confirmar.
  - `frontend/src/components/Triage/AssignmentModal.jsx`: Selector de uno o varios departamentos y asignación de técnicos filtrados estrictamente por dichos departamentos.
  - `frontend/src/components/Comments/IncidentCommentsSection.jsx`: Lista cronológica de comentarios con distintivo visual (`Nota Interna` / `Aviso Público`), indicador de edición, formulario para publicar comentarios nuevos y botón de edición para autor/administrador.
  - `frontend/src/pages/OperatorPanelPage.jsx`:
    - Visualización adaptativa por rol: si el usuario es `TECHNICIAN`, encabezado enfocado a "Mis Asignaciones Técnicas" y ocultación de controles exclusivos de operador/admin.
    - Acciones de intervención técnica ("Iniciar Trabajo" y "Resolver"): accesibles para `ADMIN` y para el técnico asignado.
  - `frontend/src/components/Map/IncidentDetailModal.jsx` y `frontend/src/pages/MyIncidentsPage.jsx`: Inclusión de la sección de comentarios públicos para seguimiento ciudadano transparente.

---

## Decisiones

- **Servicio Centralizado de Triaje vs Máquina de Estados Externa:** Se optó por centralizar las transiciones en `IncidentTriageService` con validaciones de guardas en código en lugar de introducir librerías de Workflow/BPM complejas como Spring StateMachine o Camunda. Permite código directo, limpio, testeable con Mockito y sin configuraciones propietarias opacas. *(Documentado en [ADR-07](file:///C:/Users/Jorge/Desktop/Ingenieria/Proyecto-Urbanpulse/docs/ADR/ADR-07-gestion-ciclo-vida-asignacion-multidepartamental.md))*.
- **Permisos Universales de Ciclo de Vida para ADMIN:** El administrador ostenta la capacidad de gestionar y desbloquear incidencias en cualquier estado operativo sin obligarle a autoasignarse artificialmente como técnico de campo.
- **Segregación de Incidencias en Backend para TECHNICIAN:** La restricción de visión del técnico se garantiza en la capa de servicio (`IncidentService`), evitando fugas de información operacional no pertinente para su labor.
- **Matriz de Priorización Asistida Determinista:** Se implementa un motor de reglas puras en `PriorityService` desacoplado de dependencias externas. Se descartó el uso de modelos de lenguaje o machine learning en este sprint para garantizar determinismo absoluto, respuesta inmediata (< 5 ms) y explicabilidad transparente sin costes de inferencia.
- **Segregación de Comentarios a Nivel de Consulta SQL y DTOs:** La separación entre comentarios internos y públicos se filtra en el backend en base a las credenciales del usuario autenticado (`SecurityContext`). Se descartó crear dos tablas independientes (`internal_notes` y `public_comments`) porque comparten atributos idénticos y una columna `is_internal` con índice optimiza el modelo relacional.
- **Desactivación Lógica Explícita (`is_active = false`):** Se mantiene el historial de asignaciones previas sin eliminarlas físicamente de la base de datos para preservar la trazabilidad de qué técnicos intervinieron en cada etapa. El endpoint `POST /api/incidents/{id}/assignments/deactivate` expone claramente la semántica de desactivación lógica.

---

## Riesgos

- **Concurrencia en transiciones de estado y asignaciones múltiples:** Dos operadores intentando validar o asignar la misma incidencia simultáneamente.
  - *Mitigación:* Verificación de estado previo dentro de la transacción atómica `@Transactional`. Si el estado ya no coincide con la precondición esperada, se rechaza con error de conflicto 409 o 400 descriptivo.
- **Inconsistencia de técnico y departamento asignado por condiciones de carrera:** Desasignar un departamento mientras se asigna un técnico del mismo departamento.
  - *Mitigación:* `AssignmentService` valida atómicamente en la misma transacción que el departamento del técnico continúa activo en la incidencia antes de persistir la fila activa en `assignments` (RF-10).
- **Fuga involuntaria de notas técnicas internas a la ciudadanía:** Exponer notas confidenciales del personal municipal a usuarios con rol `CITIZEN`.
  - *Mitigación:* Filtro estricto en `CommentService`: si el usuario es `CITIZEN`, el servicio exige que sea el autor del reporte y ejecuta exclusivamente la consulta con `is_internal = false`. Tests unitarios y de integración específicos para corroborar esta protección.

---

## Estrategia de tests

### Tests Unitarios y de Persistencia (Backend)
- **`PriorityServiceTest`:**
  - Cobertura exhaustiva de las 5 combinaciones de la matriz determinista de prioridad asistida (corte de calle `true`/`false` y niveles de seguridad `NONE`, `LOW`, `HIGH`).
  - Verificación del formato y consistencia de la justificación textual devuelta.
- **`AssignmentServiceTest`:**
  - Asignación exitosa multidepartamental y técnica.
  - Rechazo de asignación de técnico ajeno al departamento.
  - Desactivación individual e inactivación masiva en cierre/rechazo.
  - Reversión automática del estado a `VALIDATED` al desactivar el último departamento activo.
- **`CommentServiceTest`:**
  - Creación de notas internas y públicas.
  - Bloqueo de comentarios en incidencias cerradas o rechazadas.
  - Filtrado de comentarios para rol `CITIZEN` (solo públicos y solo si es dueño) frente a `OPERATOR` (todos).
  - Edición exitosa por el autor original o administrador; rechazo de edición por terceros no autorizados.
- **`IncidentTriageServiceTest`:**
  - Pruebas exhaustivas de transiciones válidas e inválidas para cada estado del ciclo de vida.
  - Validación de permisos universales de `ADMIN` para `startIntervention` y `resolveIncident`.
  - Validación de inicio de trabajo y resolución por técnico asignado con `is_active = true`, y rechazo si es técnico no asignado.
  - Validación de campos obligatorios (motivo de rechazo ≥ 5 chars, motivo de reapertura ≥ 5 chars, descripción de resolución ≥ 10 chars).
  - Validación de estados permitidos en reevaluación (`REPORTED` o `VALIDATED`).
  - Verificación de inserción en `CambiosEstadoRepository` en cada transición.
- **`IncidentServiceTest`:**
  - Validación de filtrado segregado de incidencias cuando el usuario es `TECHNICIAN` (solo incidencias con asignación activa).
- **Tests de Repositorios con `@DataJpaTest`:**
  - `TareasRepositoryTest`: comprobación de consultas activas (`findByIncidentIdAndIsActiveTrue`) y existencia de técnico activo (`existsByIncidentIdAndTechnicianIdAndIsActiveTrue`).
  - `ComentariosIncidenciasRepositoryTest`: comprobación de ordenación cronológica y filtro por `is_internal = false`.

### Tests de Integración (Backend con MockMvc)
- **`IncidentTriageControllerTest`:**
  - Pruebas de seguridad HTTP con `@WithMockUser`: verificación de respuestas 401 (sin token), 403 (rol insuficiente, ej. `CITIZEN` intentando validar o `TECHNICIAN` no asignado intentando iniciar trabajo), 400 (errores de validación de Bean Validation) y 200/201 en operaciones válidas.
  - Comprobación de que `ADMIN` puede invocar `/start-work` y `/resolve`.
  - Validación de serialización JSON y estructura de DTOs de respuesta.
- **`IncidentControllerTest`:**
  - Comprobación de que `GET /api/incidents` para usuario `TECHNICIAN` aplica la segregación de incidencias.

### Tests Unitarios y de Integración (Frontend con Vitest y Testing Library)
- **`triageService.test.js`:**
  - Pruebas de llamadas HTTP simuladas con Axios mockeado para todos los endpoints de triaje y comentarios.
- **`PriorityAssistantModal.test.jsx`:**
  - Renderizado de opciones de corte de calle y seguridad, cálculo automático asistido y emisión del evento de confirmación con la prioridad ajustada.
- **`AssignmentModal.test.jsx`:**
  - Carga de departamentos, filtrado dependiente de técnicos y validación de selección requerida.
- **`IncidentCommentsSection.test.jsx`:**
  - Visualización diferenciada de notas internas y públicas.
  - Ocultación de controles de edición para usuarios que no son autores del comentario.
  - Bloqueo del formulario de comentarios si la incidencia está cerrada o rechazada.
- **`OperatorPanelPage.test.jsx` (Integración):**
  - Comprobación del flujo integral de triaje: selección de incidencia, cálculo asistido de prioridad, validación de reporte, asignación de departamento/técnico y visualización en el panel.
  - Verificación de permisos y botones contextuales según rol (`ADMIN`, `OPERATOR`, `TECHNICIAN`).
- **`HomePage.test.jsx` y `App.test.jsx`:**
  - Verificación de acceso al panel para rol `TECHNICIAN`.
# QA Note — Feature 005: Gestión y Triaje Operativo de Incidencias

**Estado de Preparación de QA:** ✅ LISTO PARA VALIDACIÓN (`sdd-validate`)  
**Fecha de finalización de desarrollo:** 2026-11-06  
**Rama de desarrollo:** `feature/005-gestion-y-triaje-operativo-de-incidencias`  
**Referencia arquitectónica:** [ADR-07: Gestión del Ciclo de Vida y Asignación Multidepartamental](file:///c:/Users/felix/Desktop/Desarrollo_en_la_nube/Proyecto-Urbanpulse/docs/ADR/ADR-07-gestion-ciclo-vida-asignacion-multidepartamental.md)  
**Especificación funcional:** [spec.md](file:///c:/Users/felix/Desktop/Desarrollo_en_la_nube/Proyecto-Urbanpulse/features/005-gestion-y-triaje-operativo-de-incidencias/spec.md)  
**Plan técnico:** [plan.md](file:///c:/Users/felix/Desktop/Desarrollo_en_la_nube/Proyecto-Urbanpulse/features/005-gestion-y-triaje-operativo-de-incidencias/plan.md)  

---

## 1. Resumen Global de la Feature 005

La Feature **005 — Gestión y Triaje Operativo de Incidencias** implementa el flujo operacional completo de supervisión municipal, triaje, asignación departamental multidisciplinar, intervención técnica y trazabilidad auditable en UrbanPulse.

Cubre de forma exhaustiva las siguientes Historias de Usuario:
- **HU07 (Validación o Rechazo de Reportes):** Operadores y administradores evalúan reportes iniciales (`REPORTED`), validándolos formalmente (`VALIDATED`) o rechazándolos con motivo justificado (`REJECTED`, mínimo 5 caracteres) e inactivación de asignaciones. Permite además la reevaluación rectificativa de expedientes rechazados.
- **HU08 (Asignación Multidepartamental y Técnica):** Coordinación multidisciplinar permitiendo asignar simultáneamente múltiples áreas municipales (e.g., Alumbrado, Parques, Obras) y técnicos especialistas adscritos a dichas áreas, con reversión automática a `VALIDATED` si no restan departamentos activos.
- **HU09 (Intervención Técnica y Resolución):** Técnicos municipales asignados inician la actuación de campo (`IN_PROGRESS`) y declaran la resolución (`RESOLVED`) aportando memoria descriptiva obligatoria (mínimo 10 caracteres) para prioridades medias, altas y críticas.
- **HU10 (Reapertura o Cierre Definitivo):** Supervisores cierran formalmente el expediente (`CLOSED`) con inactivación automática de asignaciones o lo reabren (`REOPENED`) con motivo obligatorio si las actuaciones resultaron insuficientes.
- **HU11 (Muro de Comentarios y Notas Técnicas):** Historial colaborativo de intervenciones con segregación estricta RBAC entre notas técnicas internas (`is_internal = true`, restringidas a personal municipal) y avisos públicos para la ciudadanía (`is_internal = false`), sellado temporal en ediciones (`updated_at`) y bloqueo en expedientes terminales.
- **HU12 (Priorización Asistida y Manual):** Motor determinista puro de recomendación de prioridad basado en corte de vía y nivel de riesgo para la seguridad, con justificación textual explicativa automática y confirmación por el operador.

---

## 2. Mapeo de Requisitos Funcionales (RF-1 a RF-24)

| RF | Requisito Funcional (EARS) | Componentes de Implementación | Tests de Verificación | Estado |
|:---|:---|:---|:---|:---:|
| **RF-1** | Cálculo determinista de prioridad asistida según matriz (vía y seguridad) con justificación textual explicativa. | `PriorityService`, `PriorityCalculationRequest`, `PriorityCalculationResponse` | `PriorityServiceTest` (9 tests), `PriorityAssistantModal.test.jsx` | ✅ PASS |
| **RF-2** | Fijación y actualización de prioridad operativa definitiva (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) y `priority_reason`. | `IncidentTriageService.updatePriority`, `UpdatePriorityRequest`, `Incidencia` | `IncidentTriageServiceTest`, `IncidentTriageControllerTest`, `PriorityAssistantModal.test.jsx` | ✅ PASS |
| **RF-3** | Restricción RBAC: Denegación HTTP 403 en cálculo o fijación de prioridad a usuarios sin rol `OPERATOR` ni `ADMIN`. | `IncidentTriageController`, `SecurityConfig` (`@PreAuthorize`) | `IncidentTriageControllerTest` | ✅ PASS |
| **RF-4** | Validación formal de reportes (`REPORTED` &rarr; `VALIDATED`) con registro inmutable en `CambiosEstado`. | `IncidentTriageService.validateIncident`, `CambiosEstadoRepository` | `IncidentTriageServiceTest`, `OperatorPanelPage.test.jsx` | ✅ PASS |
| **RF-5** | Rechazo de reporte con motivo &ge; 5 caracteres, transición a `REJECTED`, inactivación de asignaciones y auditoría. | `IncidentTriageService.rejectIncident`, `AssignmentService.deactivateAllActiveAssignments` | `IncidentTriageServiceTest`, `AssignmentServiceTest`, `ReasonModal.test.jsx`, `OperatorPanelPage.test.jsx` | ✅ PASS |
| **RF-6** | Bloqueo por validación al intentar rechazar sin motivo o con menos de 5 caracteres útiles. | `RejectIncidentRequest` (`@Size(min=5)`), `IncidentTriageService` | `IncidentTriageServiceTest`, `DtoValidationTest`, `ReasonModal.test.jsx` | ✅ PASS |
| **RF-7** | Reevaluación de incidencia rechazada devolviéndola a `REPORTED` o `VALIDATED` con motivo justificado. | `IncidentTriageService.reevaluateIncident`, `ReevaluateIncidentRequest` | `IncidentTriageServiceTest`, `ReasonModal.test.jsx`, `OperatorPanelPage.test.jsx` | ✅ PASS |
| **RF-8** | Asignación multidepartamental en incidencias `VALIDATED` o `REOPENED` transicionando a `ASSIGNED`. | `AssignmentService.assignDepartmentsAndTechnicians`, `TareasRepository` | `AssignmentServiceTest`, `AssignmentModal.test.jsx`, `OperatorPanelPage.test.jsx` | ✅ PASS |
| **RF-9** | Asignación de técnicos adscritos estrictamente a departamentos asignados a la incidencia. | `AssignmentService`, `UserRepository.findByRoleAndDepartmentId` | `AssignmentServiceTest`, `AssignmentModal.test.jsx` | ✅ PASS |
| **RF-10** | Denegación atómica si un técnico no pertenece a ninguno de los departamentos asignados activos. | `AssignmentService.validateTechniciansBelongToActiveDepartments` | `AssignmentServiceTest`, `AssignmentModal.test.jsx` | ✅ PASS |
| **RF-11** | Desactivación lógica de asignaciones departamentales y técnicas (`is_active = false`). | `AssignmentService.deactivateAssignments`, `Tareas` | `AssignmentServiceTest`, `TareasRepositoryTest`, `AssignmentModal.test.jsx` | ✅ PASS |
| **RF-12** | Reversión automática de estado de `ASSIGNED` a `VALIDATED` si no restan departamentos activos tras desasignar. | `AssignmentService.checkAndRevertStatusIfNoActiveDepartments` | `AssignmentServiceTest`, `OperatorPanelPage.test.jsx` | ✅ PASS |
| **RF-13** | Inicio de trabajos técnicos (`ASSIGNED`/`REOPENED` &rarr; `IN_PROGRESS`) por técnico asignado activo. | `IncidentTriageService.startIntervention`, `StartWorkButton` | `IncidentTriageServiceTest`, `StartWorkButton.test.jsx`, `OperatorPanelPage.test.jsx` | ✅ PASS |
| **RF-14** | Denegación de inicio de trabajos si el usuario no es un técnico asignado activamente a la incidencia. | `IncidentTriageService`, `TareasRepository.existsByIncidentIdAndTechnicianIdAndIsActiveTrue` | `IncidentTriageServiceTest`, `IncidentTriageControllerTest` | ✅ PASS |
| **RF-15** | Reporte de resolución técnica (`RESOLVED`) con memoria &ge; 10 caracteres (opcional en prioridad `LOW`). | `IncidentTriageService.resolveIncident`, `ResolveIncidentModal` | `IncidentTriageServiceTest`, `ResolveIncidentModal.test.jsx`, `OperatorPanelPage.test.jsx` | ✅ PASS |
| **RF-16** | Bloqueo de resolución en prioridades medias, altas y críticas si la memoria técnica tiene &lt; 10 caracteres. | `IncidentTriageService`, `ResolveIncidentModal` | `IncidentTriageServiceTest`, `ResolveIncidentModal.test.jsx` | ✅ PASS |
| **RF-17** | Cierre administrativo definitivo (`CLOSED`) con inactivación automática de asignaciones y registro auditable. | `IncidentTriageService.closeIncident`, `AssignmentService.deactivateAllActiveAssignments` | `IncidentTriageServiceTest`, `AssignmentServiceTest`, `OperatorPanelPage.test.jsx` | ✅ PASS |
| **RF-18** | Reapertura operativa (`RESOLVED` &rarr; `REOPENED`) con motivo obligatorio &ge; 5 caracteres. | `IncidentTriageService.reopenIncident`, `ReopenIncidentRequest` | `IncidentTriageServiceTest`, `ReasonModal.test.jsx`, `OperatorPanelPage.test.jsx` | ✅ PASS |
| **RF-19** | Denegación de cierre o reapertura a usuarios con roles no autorizados (`TECHNICIAN`, `CITIZEN`). | `IncidentTriageController` (`@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")`) | `IncidentTriageControllerTest` | ✅ PASS |
| **RF-20** | Publicación de notas técnicas internas (`is_internal = true`) y comentarios públicos (`is_internal = false`). | `CommentService.createComment`, `ComentariosIncidenciasRepository` | `CommentServiceTest`, `IncidentCommentsSection.test.jsx` | ✅ PASS |
| **RF-21** | Segregación estricta RBAC: los ciudadanos nunca reciben comentarios marcados como internos (`is_internal = true`). | `CommentService.getCommentsForIncident`, `IncidentTriageController` | `CommentServiceTest`, `IncidentTriageControllerTest`, `IncidentCommentsSection.test.jsx` | ✅ PASS |
| **RF-22** | Edición de comentarios por su autor original o `ADMIN`, sellando `updated_at` y preservando el autor. | `CommentService.updateComment`, `ComentariosIncidencias.updatedAt` | `CommentServiceTest`, `ComentariosIncidenciasRepositoryTest`, `IncidentCommentsSection.test.jsx` | ✅ PASS |
| **RF-23** | Denegación de edición de comentarios a terceros no autores sin rol `ADMIN`. | `CommentService.validateCommentEditAuthorization` | `CommentServiceTest`, `IncidentCommentsSection.test.jsx` | ✅ PASS |
| **RF-24** | Bloqueo estricto de adición o edición de comentarios en incidencias en estado terminal (`CLOSED`, `REJECTED`). | `CommentService.assertIncidentNotTerminal` | `CommentServiceTest`, `IncidentCommentsSection.test.jsx` | ✅ PASS |

---

## 3. Componentes Construidos

### 3.1 Backend (Java 21, Spring Boot 3.4, JPA, Spring Security)
- **Persistencia y Entidades:**
  - `ComentariosIncidencias`: campo `updatedAt` (`OffsetDateTime`) con Javadoc.
  - Repositorios: `TareasRepository`, `ComentariosIncidenciasRepository`, `DepartamentoRepository`, `UserRepository` (consulta `findByRoleAndDepartmentId`).
- **DTOs y Validación:**
  - `SafetyRiskLevel`, `PriorityCalculationRequest`, `PriorityCalculationResponse`, `UpdatePriorityRequest`.
  - `ValidateIncidentRequest`, `RejectIncidentRequest`, `ReevaluateIncidentRequest`, `ResolveIncidentRequest`, `ReopenIncidentRequest`.
  - `AssignIncidentRequest`, `DeactivateAssignmentRequest`, `AssignmentResponse`, `DepartmentResponse`, `UserSummaryResponse`.
  - `CreateCommentRequest`, `UpdateCommentRequest`, `CommentResponse`.
- **Mappers Desacoplados:**
  - `AssignmentMapper`, `CommentMapper`, `DepartmentMapper`.
- **Servicios de Dominio:**
  - `PriorityService`: motor determinista puro de priorización asistida.
  - `AssignmentService`: coordinación multidepartamental atómica, asignación y desactivación lógica.
  - `CommentService`: control de autorización granular RBAC y segregación de notas internas/públicas.
  - `IncidentTriageService`: orquestador transaccional del ciclo de vida y auditoría inmutable en `CambiosEstado`.
- **Controladores REST y Seguridad:**
  - `IncidentTriageController`: endpoints `/api/incidents/{id}/**` con anotaciones `@PreAuthorize`.
  - `DepartmentController`: catálogo de áreas municipales y técnicos especialistas adscritos.
  - `SecurityConfig`: políticas de ruta segura y autorización RBAC.

### 3.2 Frontend (React 19, Vite, Testing Library, Vitest)
- **Servicios HTTP:**
  - `triageService.js`: cliente Axios tipado con JSDoc para todas las operaciones de triaje.
  - `incidentService.js`: soporte para comentarios públicos en seguimiento ciudadano.
- **Componentes y Modales de Triaje:**
  - `PriorityAssistantModal.jsx` / `.css`: asistente de cálculo determinista y fijación de prioridad.
  - `AssignmentModal.jsx` / `.css`: asignador multidepartamental en cascada y visor de asignaciones activas.
  - `ResolveIncidentModal.jsx` / `.css`: reporte de resolución técnica con contador de memoria de actuaciones.
  - `ReasonModal.jsx` / `.css`: modal accesible para motivos obligatorios (Rechazo, Reapertura y Reevaluación).
  - `StartWorkButton.jsx`: botón de inicio de trabajos técnicos sobre el terreno.
  - `IncidentCommentsSection.jsx` / `.css`: muro colaborativo cronológico con discriminación visual de visibilidad y edición en línea.
- **Vistas Integradas:**
  - `OperatorPanelPage.jsx` / `.css`: centro de mando operativo con tarjetas de estadísticas, tabla de incidencias, badges departamentales y botones contextuales de acción reactiva.
  - `IncidentDetailModal.jsx` y `MyIncidentsPage.jsx`: integración de comentarios públicos para seguimiento ciudadano transparente.

---

## 4. Matriz de Endpoints y Seguridad RBAC

| Método | Endpoint | Roles Autorizados | Estado Precondición | Descripción |
|:---|:---|:---|:---|:---|
| `POST` | `/api/incidents/{id}/priority/calculate` | `OPERATOR`, `ADMIN` | Cualquiera | Calcula prioridad sugerida según vía y seguridad. |
| `PUT` | `/api/incidents/{id}/priority` | `OPERATOR`, `ADMIN` | No terminal | Fija la prioridad definitiva y su justificación. |
| `POST` | `/api/incidents/{id}/validate` | `OPERATOR`, `ADMIN` | `REPORTED` | Valida el reporte para entrar al circuito operativo. |
| `POST` | `/api/incidents/{id}/reject` | `OPERATOR`, `ADMIN` | No terminal | Rechaza el reporte con motivo &ge; 5 caracteres e inactiva tareas. |
| `POST` | `/api/incidents/{id}/reevaluate` | `OPERATOR`, `ADMIN` | `REJECTED` | Reactiva una incidencia rechazada a `REPORTED` o `VALIDATED`. |
| `POST` | `/api/incidents/{id}/assignments` | `OPERATOR`, `ADMIN` | `VALIDATED`, `REOPENED`, `ASSIGNED` | Asigna departamentos y técnicos adscritos. |
| `POST` | `/api/incidents/{id}/assignments/deactivate` | `OPERATOR`, `ADMIN` | `ASSIGNED` | Desactiva lógicamente asignaciones existentes. |
| `POST` | `/api/incidents/{id}/start-work` | `TECHNICIAN` | `ASSIGNED`, `REOPENED` | Inicia trabajos de campo (pasa a `IN_PROGRESS`). |
| `POST` | `/api/incidents/{id}/resolve` | `TECHNICIAN` | `IN_PROGRESS` | Resuelve con memoria técnica obligatoria. |
| `POST` | `/api/incidents/{id}/close` | `OPERATOR`, `ADMIN` | `RESOLVED` | Cierra administrativamente el expediente. |
| `POST` | `/api/incidents/{id}/reopen` | `OPERATOR`, `ADMIN` | `RESOLVED` | Reabre con motivo obligatorio &ge; 5 caracteres. |
| `GET` | `/api/incidents/{id}/comments` | `AUTHENTICATED` | Cualquiera | Ciudadanos reciben solo públicos; personal recibe todos. |
| `POST` | `/api/incidents/{id}/comments` | `OPERATOR`, `TECHNICIAN`, `ADMIN` | No terminal | Publica nota interna o aviso público. |
| `PUT` | `/api/incidents/{id}/comments/{commentId}` | `AUTHENTICATED` | No terminal | Edición permitida solo al autor original o `ADMIN`. |
| `GET` | `/api/departments` | `AUTHENTICATED` | — | Catálogo de departamentos municipales. |
| `GET` | `/api/departments/{id}/technicians` | `OPERATOR`, `ADMIN` | — | Técnicos adscritos al departamento indicado. |

---

## 5. Resultados de Pruebas Automatizadas

### 5.1 Backend (`mvn clean test -f Backend/pom.xml`)
- **Total de pruebas ejecutadas:** 458 tests.
- **Fallos:** 0.
- **Errores:** 0.
- **Omitidos:** 1 (prueba de integración real con PostgreSQL `@EnabledIfPostgresAvailable`, limpia sin penalización).
- **Resultado de compilación:** `BUILD SUCCESS`.
- **Reporte JaCoCo:** Generado con éxito sobre 55 clases analizadas en `Backend/target/site/jacoco/index.html`.

### 5.2 Frontend (`npm test` & `npm run lint`)
- **Suites de prueba:** 18 suites pasadas (100%).
- **Tests unitarios e integración:** 155 tests pasados (100%).
- **Linter (ESLint):** 0 errores, 0 advertencias (`eslint .` completado limpiamente).

---

## 6. Guía Paso a Paso para la Verificación Manual / Aceptación en `sdd-validate`

### Prerrequisitos de Entorno
1. Iniciar la base de datos PostgreSQL: `docker compose up -d`.
2. Iniciar el servidor backend: `cd Backend && mvn spring-boot:run`.
3. Iniciar la aplicación cliente: `cd frontend && npm run dev` y acceder a `http://localhost:5173`.

### Escenarios de Verificación Operativa

#### Escenario 1: Priorización Asistida y Validación Inicial
1. Iniciar sesión con un usuario con rol `OPERATOR` (e.g. `operador@malaga.es`).
2. Dirigirse al **Panel de Operador** (`/operator`).
3. Localizar una incidencia en estado `REPORTED`.
4. Pulsar el botón **`⚡ Asistente`**:
   - Activar "Corte de Calle o Calzada" (`streetCut = true`) y seleccionar riesgo de seguridad "Alto" (`HIGH`).
   - Comprobar que el cálculo automático sugiere `CRITICAL` y redacta la justificación.
   - Ajustar manualmente a `HIGH` si se desea y confirmar.
   - Verificar que la tabla actualiza inmediatamente el badge de prioridad y muestra el banner de confirmación.
5. Pulsar el botón **`Validar`**:
   - Verificar que el estado cambia reactivamente a `VALIDATED` (`badge-status-validated`).

#### Escenario 2: Asignación Multidepartamental en Cascada
1. Sobre la incidencia validada, pulsar **`Asignar Departamentos/Técnicos`**.
2. Seleccionar el departamento **Alumbrado** y **Vías Públicas**.
3. En el selector de técnicos, verificar que solo se listan técnicos pertenecientes a los departamentos seleccionados.
4. Seleccionar un técnico y pulsar **`Guardar Asignaciones`**.
5. Verificar que el modal se cierra, la tabla actualiza el estado a `ASSIGNED` y se visualizan los badges con los nombres de los departamentos y técnicos asignados.

#### Escenario 3: Intervención Técnica y Resolución
1. Iniciar sesión con el usuario técnico asignado (`TECHNICIAN`) o continuar como `ADMIN`.
2. Localizar la incidencia asignada y pulsar **`Iniciar Trabajo`**.
   - Comprobar que el estado pasa inmediatamente a `IN_PROGRESS`.
3. Pulsar el botón **`Resolver`**:
   - Intentar confirmar con una memoria de actuaciones vacía o menor a 10 caracteres; verificar que el botón se mantiene deshabilitado o muestra mensaje de error.
   - Escribir una descripción válida (&ge; 10 caracteres, e.g., "Sustitución de balasto y luminaria LED completada con éxito.").
   - Confirmar; comprobar que el estado pasa a `RESOLVED`.

#### Escenario 4: Muro de Comentarios y Segregación de Visibilidad
1. Sobre cualquier incidencia activa, pulsar el botón **`💬 Comentarios`**.
2. Redactar una **Nota Interna** (`is_internal = true`) indicando "Revisión técnica urgente requerida por avería secundaria".
3. Redactar un **Aviso Público** (`is_internal = false`) indicando "Trabajos en curso programados para finalizar hoy".
4. Cerrar sesión e iniciar sesión como ciudadano creador del reporte (`CITIZEN`).
5. Abrir la vista de detalle de la incidencia (`/my-incidents` o modal cartográfico):
   - Verificar que el aviso público es visible.
   - **Verificar que la nota interna NO es visible ni se encuentra en la respuesta HTTP.**

#### Escenario 5: Cierre Administrativo y Reapertura
1. Volver al panel de operador como `OPERATOR` o `ADMIN`.
2. Localizar una incidencia en estado `RESOLVED`.
3. Pulsar **`Reabrir`**:
   - El diálogo `ReasonModal` solicita el motivo obligatorio (&ge; 5 caracteres).
   - Introducir "La farola continúa parpadeando tras la prueba nocturna" y confirmar.
   - Comprobar que pasa a `REOPENED` y se habilitan nuevamente los botones de intervención.
4. Una vez resuelta de nuevo, pulsar **`Cerrar`**:
   - Comprobar que pasa a estado terminal `CLOSED`.
   - Comprobar que todas sus asignaciones pasan a inactivas.
   - Comprobar que en el muro de comentarios el formulario queda bloqueado con aviso de expediente cerrado.

#### Escenario 6: Rechazo y Reevaluación Rectificativa
1. Localizar un reporte improcedente en `REPORTED` o `VALIDATED`.
2. Pulsar **`Rechazar`**:
   - Introducir motivo &ge; 5 caracteres (e.g. "Competencia exclusiva de Carreteras del Estado") y confirmar.
   - Comprobar transición a `REJECTED`.
3. Pulsar el botón **`Reevaluar`**:
   - Seleccionar estado destino `VALIDATED`.
   - Aportar motivo rectificativo &ge; 5 caracteres (e.g. "Se subsana informe limítrofe confirmando término municipal").
   - Confirmar; comprobar que la incidencia se reactiva limpiamente en estado `VALIDATED`.

---

## 7. Conclusión

La implementación de la Feature 005 cumple íntegramente con la arquitectura definida en `ADR-07`, el estándar de gobierno de UrbanPulse (`AGENTS.md`) y la totalidad de los criterios de aceptación de `spec.md`. Queda formalmente lista y preparada para la fase de validación de usuario y aceptación en `sdd-validate`.

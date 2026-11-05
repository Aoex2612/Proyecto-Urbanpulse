# Tarea 4: Servicio de Orquestación del Ciclo de Vida y Transiciones de Estado (`IncidentTriageService`)

**Feature:** `005-gestion-y-triaje-operativo-de-incidencias`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-1, RF-2, RF-3, RF-4, RF-5, RF-6, RF-7, RF-13, RF-14, RF-15, RF-16, RF-17, RF-18, RF-19  

---

## 1. Resumen de la Tarea

En esta tarea se ha implementado el servicio central de orquestación del ciclo de vida y auditoría inmutable de incidencias urbanas (`IncidentTriageService`), junto con su suite exhaustiva de pruebas unitarias con Mockito (`IncidentTriageServiceTest`):

1. **`IncidentTriageService` (`Backend/src/main/java/org/bulbaseros/service/IncidentTriageService.java`):**
   - Inyección de dependencias Spring: `IncidenciaRepository`, `UserRepository`, `CambiosEstadoRepository`, `TareasRepository`, `AssignmentService`, `PriorityService` e `IncidentMapper`.
   - **4.1 `calculateAssistedPriority(Long incidentId, PriorityCalculationRequest request, String userEmail)` - RF-1, RF-3:**
     * Verifica que el usuario autenticado exista y disponga de rol `OPERATOR` o `ADMIN`. Deniega con HTTP 403 Forbidden (`ResponseStatusException`) ante cualquier otro rol (`CITIZEN`, `TECHNICIAN`).
     * Valida la existencia de la incidencia en base de datos (`IncidentNotFoundException` si no existe).
     * Delega en `priorityService.calculateAssistedPriority(request)` y devuelve `PriorityCalculationResponse`.
   - **4.2 `updatePriority(Long incidentId, UpdatePriorityRequest request, String userEmail)` - RF-2, RF-3:**
     * Valida autorización para roles `OPERATOR` o `ADMIN` (403 Forbidden en caso contrario).
     * Valida que la incidencia exista y que la prioridad suministrada no sea nula.
     * Actualiza la prioridad operativa (`priority`) y su justificación explicativa (`priorityReason`) en la entidad `Incidencia` refrescando la marca temporal `updatedAt`.
     * Retorna `IncidentResponse` utilizando `IncidentMapper`.
   - **4.3 `validateIncident(Long incidentId, ValidateIncidentRequest request, String userEmail)` - RF-4:**
     * Valida autorización para roles `OPERATOR` o `ADMIN`.
     * Valida que la incidencia se encuentre en estado inicial `REPORTED`. Si se encuentra en otro estado, lanza `IllegalStateException`.
     * Transiciona el estado a `VALIDATED` y actualiza `updatedAt`.
     * Inserta un registro inmutable de auditoría en `CambiosEstadoRepository` con `fromStatus = "REPORTED"`, `toStatus = "VALIDATED"`, el usuario responsable y el motivo opcional.
     * Retorna `IncidentResponse`.
   - **4.3 `rejectIncident(Long incidentId, RejectIncidentRequest request, String userEmail)` - RF-5, RF-6:**
     * Valida autorización para roles `OPERATOR` o `ADMIN`.
     * Valida que el motivo de rechazo sea obligatorio y contenga al menos 5 caracteres no vacíos (`IllegalArgumentException` si no cumple).
     * Valida que la incidencia no se encuentre ya en un estado terminal (`REJECTED` o `CLOSED`), arrojando `IllegalStateException`.
     * Desactiva de forma masiva todas las asignaciones activas existentes invocando `assignmentService.deactivateAllActiveAssignments(incidentId)`.
     * Transiciona el estado a `REJECTED`, inserta el registro inmutable en `CambiosEstadoRepository` con el motivo obligatorio y actualiza `updatedAt`.
     * Retorna `IncidentResponse`.
   - **4.4 `reevaluateIncident(Long incidentId, ReevaluateIncidentRequest request, String userEmail)` - RF-7:**
     * Valida autorización para roles `OPERATOR` o `ADMIN`.
     * Valida que el estado destino (`targetStatus`) sea estrictamente `REPORTED` o `VALIDATED` (`IllegalArgumentException` en caso contrario).
     * Valida que el motivo de rectificación sea obligatorio y tenga al menos 5 caracteres no vacíos.
     * Valida que la incidencia se encuentre en estado `REJECTED` (`IllegalStateException` si no lo está).
     * Transiciona la incidencia al estado destino seleccionado, registra el cambio en `CambiosEstadoRepository` y actualiza `updatedAt`.
     * Retorna `IncidentResponse`.
   - **4.5 `startIntervention(Long incidentId, String userEmail)` - RF-13, RF-14:**
     * Valida que el usuario tenga rol `TECHNICIAN` o `ADMIN`.
     * Si el usuario es `TECHNICIAN`, verifica que cuente con una asignación activa en la incidencia mediante `tareasRepository.existsByIncidentIdAndTechnicianIdAndIsActiveTrue(incidentId, user.getId())`. Si no está asignado, deniega con HTTP 403 Forbidden indicando "No es un técnico asignado a esta incidencia".
     * Valida que la incidencia se encuentre en estado `ASSIGNED` o `REOPENED` (`IllegalStateException` si no cumple).
     * Transiciona el estado a `IN_PROGRESS`, actualiza `updatedAt` y registra el hito en `CambiosEstadoRepository`.
     * Retorna `IncidentResponse`.
   - **4.6 `resolveIncident(Long incidentId, ResolveIncidentRequest request, String userEmail)` - RF-15, RF-16:**
     * Valida rol `TECHNICIAN` asignado activamente (o `ADMIN`) (RF-14).
     * Valida que la incidencia se encuentre en estado `IN_PROGRESS` (`IllegalStateException` si no cumple).
     * Comprueba la justificación de resolución: si `priority != LOW`, la descripción de actuaciones es obligatoria y debe tener al menos 10 caracteres no vacíos. Si no cumple, lanza `IllegalArgumentException` (RF-16). En incidencias con prioridad `LOW`, la descripción es opcional.
     * Transiciona el estado a `RESOLVED`, registra la descripción de actuaciones como motivo en `CambiosEstadoRepository` y actualiza `updatedAt`.
     * Retorna `IncidentResponse`.
   - **4.7 `closeIncident(Long incidentId, String userEmail)` - RF-17, RF-19:**
     * Valida rol `OPERATOR` o `ADMIN` (RF-19).
     * Valida que la incidencia se encuentre en estado `RESOLVED` (`IllegalStateException` si no cumple).
     * Inactiva automáticamente todas las asignaciones activas de la incidencia mediante `assignmentService.deactivateAllActiveAssignments(incidentId)`.
     * Transiciona el estado a `CLOSED`, registra el evento en `CambiosEstadoRepository` y actualiza `updatedAt`.
     * Retorna `IncidentResponse`.
   - **4.7 `reopenIncident(Long incidentId, ReopenIncidentRequest request, String userEmail)` - RF-18, RF-19:**
     * Valida rol `OPERATOR` o `ADMIN` (RF-19).
     * Valida que la incidencia se encuentre en estado `RESOLVED`.
     * Valida que el motivo de reapertura tenga al menos 5 caracteres no vacíos (`IllegalArgumentException` si no cumple).
     * Transiciona el estado a `REOPENED`, registra el motivo en `CambiosEstadoRepository` y actualiza `updatedAt`.
     * Retorna `IncidentResponse`.

---

## 2. Cobertura de Pruebas Unitarias

Se ha creado la suite `IncidentTriageServiceTest` (`Backend/src/test/java/org/bulbaseros/service/IncidentTriageServiceTest.java`) con 40 pruebas unitarias con Mockito cubriendo:
- Cálculo asistido de prioridad y control 403 ante roles no autorizados.
- Actualización de prioridad manual y validación de parámetros.
- Validación de reporte inicial (`REPORTED` &rarr; `VALIDATED`) con y sin observaciones, y bloqueo ante estados ilegales.
- Rechazo (`REJECTED`) con validación de motivo (&ge; 5 caracteres), inactivación de asignaciones y bloqueo en terminales.
- Reevaluación (`REJECTED` &rarr; `REPORTED`/`VALIDATED`) con comprobación estricta de estado destino y motivo.
- Inicio de intervención técnica (`ASSIGNED`/`REOPENED` &rarr; `IN_PROGRESS`) con comprobación de asignación activa de técnico y bypass de administración.
- Resolución de incidencias con descripción opcional para `LOW` y obligatoria (&ge; 10 caracteres) para `MEDIUM`, `HIGH` y `CRITICAL`.
- Cierre definitivo (`RESOLVED` &rarr; `CLOSED`) con inactivación total de tareas y bloqueo a técnicos/ciudadanos.
- Reapertura (`RESOLVED` &rarr; `REOPENED`) con motivo obligatorio &ge; 5 caracteres.
- Verificación de inserción atómica en `CambiosEstadoRepository` en cada una de las 7 transiciones de estado del ciclo de vida.

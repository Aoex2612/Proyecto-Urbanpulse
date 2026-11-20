# Tarea 3: Servicios de Negocio: Priorización Asistida, Asignaciones Multidepartamentales y Comentarios

**Feature:** `005-gestion-y-triaje-operativo-de-incidencias`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-1, RF-2, RF-8, RF-9, RF-10, RF-11, RF-12, RF-17, RF-20, RF-21, RF-22, RF-23, RF-24  

---

## 1. Resumen de la Tarea

En esta tarea se han implementado y probado unitariamente con Mockito los servicios de lógica de negocio esenciales para el triaje operativo, priorización asistida, asignaciones multidepartamentales y el muro de comentarios de incidencias:

1. **`PriorityService` (`Backend/src/main/java/org/bulbaseros/service/PriorityService.java`) - RF-1:**
   - Implementa el motor de reglas determinista como función pura libre de efectos colaterales para el cálculo asistido de prioridad.
   - Aplica exhaustivamente la matriz determinista de decisión:
     * `streetCut == true && safetyRisk == HIGH` &rarr; `CRITICAL` con justificación descriptiva.
     * `streetCut == true && (safetyRisk == LOW || safetyRisk == NONE)` &rarr; `HIGH` con justificación descriptiva.
     * `streetCut == false && safetyRisk == HIGH` &rarr; `HIGH` con justificación descriptiva.
     * `streetCut == false && safetyRisk == LOW` &rarr; `MEDIUM` con justificación descriptiva.
     * `streetCut == false && safetyRisk == NONE` &rarr; `LOW` con justificación descriptiva.
   - Valida la obligatoriedad de los factores de riesgo lanzando `IllegalArgumentException` ante parámetros nulos.
   - Retorna `PriorityCalculationResponse` con la sugerencia de prioridad y el motivo explicativo en español.

2. **`AssignmentService` (`Backend/src/main/java/org/bulbaseros/service/AssignmentService.java`) - RF-8, RF-9, RF-10, RF-11, RF-12, RF-17:**
   - Inyección de dependencias Spring: `TareasRepository`, `DepartamentoRepository`, `UserRepository`, `IncidenciaRepository` y `AssignmentMapper`.
   - Método `@Transactional assignDepartmentsAndTechnicians(Long incidentId, AssignIncidentRequest request, String userEmail)`:
     * Verifica la existencia de la incidencia (`IncidentNotFoundException` si no existe).
     * Comprueba que la incidencia se encuentre en un estado asignable (`VALIDATED`, `ASSIGNED`, `REOPENED`).
     * Valida la existencia de los departamentos solicitados.
     * Valida exhaustivamente que cada técnico indicado posea rol `TECHNICIAN` y pertenezca (`department.getId()`) a alguno de los departamentos activos de la incidencia (RF-9, RF-10), denegando con `IllegalArgumentException` si no cumple.
     * Crea o actualiza registros en `Tareas` (`assignments`) vinculando técnico y/o departamento con `isActive = true`, `assignedAt` y `assignedBy`.
     * Si la incidencia se encontraba en `VALIDATED` o `REOPENED`, transiciona su estado a `ASSIGNED` (RF-8).
     * Devuelve la lista actualizada de `AssignmentResponse` activas.
   - Método `@Transactional deactivateAssignments(Long incidentId, DeactivateAssignmentRequest request, String userEmail)`:
     * Desactiva lógicamente (`isActive = false`) las asignaciones específicas por departamento o técnico (RF-11).
     * Si tras la desactivación ya no quedan departamentos activos asignados, transiciona automáticamente el estado de la incidencia a `VALIDATED` (RF-12).
     * Devuelve la lista de asignaciones activas resultantes.
   - Método `@Transactional deactivateAllActiveAssignments(Long incidentId)`:
     * Desactiva de forma masiva todas las asignaciones activas de la incidencia (para dar soporte al rechazo o cierre definitivo) (RF-5, RF-17).
   - Método `getActiveAssignments(Long incidentId)`:
     * Recupera y mapea la colección de asignaciones activas vinculadas a una incidencia.

3. **`CommentService` (`Backend/src/main/java/org/bulbaseros/service/CommentService.java`) - RF-20, RF-21, RF-22, RF-23, RF-24:**
   - Inyección de dependencias Spring: `ComentariosIncidenciasRepository`, `IncidenciaRepository`, `UserRepository` y `CommentMapper`.
   - Método `@Transactional addComment(Long incidentId, CreateCommentRequest request, String userEmail)`:
     * Bloquea la publicación si la incidencia se encuentra en estado terminal (`CLOSED` o `REJECTED`) con `IllegalArgumentException` (RF-24).
     * Restringe la creación a personal municipal (`OPERATOR`, `TECHNICIAN`, `ADMIN`). Si un usuario con rol `CITIZEN` intenta publicar, deniega con HTTP 403 Forbidden (`ResponseStatusException`) (RF-20).
     * Persiste la entidad `ComentariosIncidencias` distinguiendo notas internas (`isInternal = true`) de avisos públicos (`isInternal = false`) y sella `createdAt`.
     * Retorna `CommentResponse`.
   - Método `getComments(Long incidentId, String userEmail)`:
     * Si el solicitante posee rol `CITIZEN`, comprueba que sea el autor del reporte; en caso afirmativo, devuelve exclusivamente los comentarios públicos (`isInternal = false`), impidiendo el acceso a notas internas (RF-21). Si no es el autor, deniega con 403 Forbidden.
     * Si el solicitante es personal municipal (`OPERATOR`, `TECHNICIAN`, `ADMIN`), devuelve todos los comentarios ordenados cronológicamente (`findByIncidentIdOrderByCreatedAtAsc`).
     * Retorna la lista de `CommentResponse`.
   - Método `@Transactional updateComment(Long incidentId, Long commentId, UpdateCommentRequest request, String userEmail)`:
     * Bloquea la edición si la incidencia se encuentra en estado terminal (`CLOSED` o `REJECTED`) (RF-24).
     * Valida que el comentario exista y pertenezca a la incidencia.
     * Verifica que el usuario autenticado sea el autor original del comentario O posea rol `ADMIN` (RF-22, RF-23). En caso contrario, deniega con HTTP 403 Forbidden.
     * Actualiza el contenido y sella la marca temporal `updatedAt = OffsetDateTime.now()`.
     * Retorna el `CommentResponse` actualizado.

4. **Batería de Pruebas Unitarias con Mockito (`Backend/src/test/java/org/bulbaseros/service/`):**
   - `PriorityServiceTest`: 9 pruebas unitarias verificando las 5 combinaciones de la matriz determinista, la consistencia de los motivos explicativos y el control de parámetros nulos.
   - `AssignmentServiceTest`: 15 pruebas unitarias cubriendo asignación exitosa, transiciones desde `VALIDATED` y `REOPENED`, complementación de asignaciones departamentales existentes, validación de estados permitidos, detección de departamentos inexistentes, rechazo a técnicos ajenos al departamento (RF-10), desactivación selectiva por departamento y por técnico, reversión automática a `VALIDATED` ante 0 asignaciones activas (RF-12), e inactivación masiva.
   - `CommentServiceTest`: 14 pruebas unitarias cubriendo alta de notas internas y avisos públicos, bloqueo en cerradas y rechazadas (RF-24), rechazo 403 a ciudadanos al comentar (RF-20), filtrado estricto para ciudadanos vs personal municipal (RF-21), edición por autor original y por admin con sellado de fecha (RF-22), rechazo 403 a terceros no autorizados (RF-23) y validaciones de pertenencia de comentarios.

---

## 2. Verificación y Resultados de Tests

Se ejecutó la suite completa de pruebas del backend mediante Maven (`cd Backend && mvn clean test`):

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.bulbaseros.service.PriorityServiceTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.service.AssignmentServiceTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.service.CommentServiceTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
...
[INFO] Results:
[INFO] Tests run: 373, Failures: 0, Errors: 0, Skipped: 1
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

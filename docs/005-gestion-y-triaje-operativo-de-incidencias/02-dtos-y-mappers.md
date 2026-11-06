# Tarea 2: DTOs, Enums de Validación y Mapeadores de Dominio

**Feature:** `005-gestion-y-triaje-operativo-de-incidencias`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-1, RF-2, RF-5, RF-6, RF-7, RF-8, RF-15, RF-16, RF-18, RF-20, RF-22  

---

## 1. Resumen de la Tarea

En esta tarea se ha implementado la capa completa de transferencia de datos (DTOs), enumeraciones de apoyo y mapeadores desacoplados para la gestión operativa, triaje, priorización asistida, transiciones de ciclo de vida, asignaciones multidepartamentales y muro de comentarios:

1. **Enum y DTOs de Priorización Asistida (`org.bulbaseros.dto`):**
   - `SafetyRiskLevel`: Enumeración con niveles de riesgo para la seguridad ciudadana (`NONE`, `LOW`, `HIGH`).
   - `PriorityCalculationRequest`: DTO con indicadores obligatorios `@NotNull` para corte de vía (`Boolean streetCut`) y nivel de riesgo (`SafetyRiskLevel safetyRisk`).
   - `PriorityCalculationResponse`: DTO que expone la prioridad recomendada (`PrioridadIncidencia suggestedPriority`) y la justificación explicativa (`String priorityReason`).
   - `UpdatePriorityRequest`: DTO para fijar la prioridad definitiva con `@NotNull PrioridadIncidencia priority` y justificación técnica opcional (`String priorityReason`).

2. **DTOs de Transiciones de Ciclo de Vida (`org.bulbaseros.dto`):**
   - `ValidateIncidentRequest`: DTO para validación operativa con motivo u observaciones opcionales (`String reason`).
   - `RejectIncidentRequest`: DTO de rechazo con validación obligatoria de motivo (`@NotBlank`, `@Size(min = 5)`).
   - `ReevaluateIncidentRequest`: DTO de reevaluación con validación de estado destino (`@NotNull EstadoIncidencia targetStatus`), motivo obligatorio (`@NotBlank`, `@Size(min = 5)`) y método de conveniencia `isValidTargetStatus()` restringiendo a `REPORTED` o `VALIDATED`.
   - `ResolveIncidentRequest`: DTO de resolución técnica con notas descriptivas de actuación (`String resolutionNotes`).
   - `ReopenIncidentRequest`: DTO de reapertura con justificación obligatoria (`@NotBlank`, `@Size(min = 5)`).

3. **DTOs de Gestión de Asignaciones y Departamentos (`org.bulbaseros.dto`):**
   - `AssignIncidentRequest`: DTO con lista obligatoria no vacía de departamentos (`@NotEmpty List<Long> departmentIds`) y lista opcional de técnicos (`List<Long> technicianIds`).
   - `DeactivateAssignmentRequest`: DTO para solicitud de desactivación lógica selectiva de asignaciones departamentales y técnicas (`List<Long> departmentIds`, `List<Long> technicianIds`).
   - `AssignmentResponse`: DTO representativo de una asignación operativa con proyección plana de identificadores y nombres (`id`, `departmentId`, `departmentName`, `technicianId`, `technicianName`, `assignedById`, `assignedByName`, `isActive`, `assignedAt`).
   - `DepartmentResponse`: DTO de área o departamento municipal (`id`, `name`, `description`).
   - `UserSummaryResponse`: DTO de resumen de usuario para listados técnicos (`id`, `fullName`, `email`, `role`, `departmentId`).

4. **DTOs de Comentarios y Notas Operativas (`org.bulbaseros.dto`):**
   - `CreateCommentRequest`: DTO de publicación con contenido obligatorio (`@NotBlank String comment`) e indicador de visibilidad interna (`@NotNull Boolean isInternal`).
   - `UpdateCommentRequest`: DTO de modificación con contenido obligatorio (`@NotBlank String comment`).
   - `CommentResponse`: DTO de lectura de comentario con datos de auditoría y autor (`id`, `incidentId`, `authorId`, `authorName`, `authorRole`, `comment`, `isInternal`, `createdAt`, `updatedAt`).

5. **Mapeadores de Dominio Desacoplados (`org.bulbaseros.mappers`):**
   - `AssignmentMapper`: Componente Spring (`@Component`) que extiende `BaseMapper<Tareas, AssignmentResponse>`, proporcionando mapeo seguro de entidades a DTOs y método constructor `toEntity(incident, department, technician, assignedBy)`.
   - `CommentMapper`: Componente Spring (`@Component`) que extiende `BaseMapper<ComentariosIncidencias, CommentResponse>`, gestionando la proyección plana del autor y método `toEntity(incident, author, comment, isInternal)`.
   - `DepartmentMapper`: Componente Spring (`@Component`) que extiende `BaseMapper<Departamento, DepartmentResponse>`, ofreciendo conversiones bidireccionales (`toDto`, `toEntity`) y métodos auxiliares `toUserSummaryDto` y `toUserSummaryDtoList` para usuarios adscritos.

6. **Batería de Pruebas Unitarias (`Backend/src/test/java/org/bulbaseros/`):**
   - `AssignmentMapperTest`: 5 pruebas que verifican conversión completa de entidad a DTO, manejo de relaciones nulas, mapeo de listas y creación de entidades.
   - `CommentMapperTest`: 5 pruebas que verifican mapeo de comentarios, proyección del rol del autor, valores por defecto de visibilidad y listas.
   - `DepartmentMapperTest`: 8 pruebas que cubren mapeo bidireccional de departamentos, gestión de nulos y proyección de entidades `Usuario` a `UserSummaryResponse`.
   - `DtoValidationTest`: 21 pruebas unitarias que validan exhaustivamente Bean Validation (`@NotNull`, `@NotBlank`, `@Size`, `@NotEmpty`) y comportamiento estructural de todos los DTOs de triaje creados.

---

## 2. Verificación y Resultados de Tests

Se ejecutó la suite completa de pruebas del backend mediante Maven (`mvn -f Backend/pom.xml clean test`):

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.bulbaseros.mappers.AssignmentMapperTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.mappers.CommentMapperTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.mappers.DepartmentMapperTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.dto.DtoValidationTest
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
...
[INFO] Results:
[INFO] Tests run: 335, Failures: 0, Errors: 0, Skipped: 1
[INFO] BUILD SUCCESS
```

- **Tests totales ejecutados:** 335 pruebas.
- **Fallos y errores:** 0.
- **Tests omitidos:** 1 (`IncidentPostgresIntegrationTest`, omitido limpiamente al requerir Docker/PostgreSQL local).
- **Cobertura:** Informe JaCoCo generado satisfactoriamente en `Backend/target/site/jacoco/index.html`.

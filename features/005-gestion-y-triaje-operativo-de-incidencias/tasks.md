# 005 — Gestión y Triaje Operativo de Incidencias — Tareas

_Checklist accionable derivada del `plan.md`. Tareas principales y subtareas pequeñas y concretas; marca `[x]` al completarlas._

## [x] Tarea 1: Persistencia, Entidades, Repositorios Spring Data JPA y Pruebas DataJpaTest - RF: RF-8, RF-9, RF-10, RF-11, RF-20, RF-21, RF-22
- [x] 1.1 Modificar entidad `ComentariosIncidencias` en `Backend/src/main/java/org/bulbaseros/entity/ComentariosIncidencias.java` añadiendo campo `updatedAt` (`OffsetDateTime`) documentado con Javadoc - RF: RF-22.
- [x] 1.2 Crear interfaz de repositorio `TareasRepository` en `org.bulbaseros.repository` con métodos de consulta `findByIncidentIdAndIsActiveTrue`, `existsByIncidentIdAndTechnicianIdAndIsActiveTrue`, `findByIncidentId` y `findByIncidentIdAndDepartmentIdAndIsActiveTrue` - RF: RF-8, RF-9, RF-10, RF-11.
- [x] 1.3 Crear interfaz de repositorio `ComentariosIncidenciasRepository` en `org.bulbaseros.repository` con consultas `findByIncidentIdOrderByCreatedAtAsc` y `findByIncidentIdAndIsInternalFalseOrderByCreatedAtAsc` - RF: RF-20, RF-21.
- [x] 1.4 Crear interfaz de repositorio `DepartamentoRepository` en `org.bulbaseros.repository` con consulta `findAllByOrderByNameAsc` - RF: RF-8.
- [x] 1.5 Actualizar `UserRepository` en `org.bulbaseros.repository` añadiendo método `findByRoleAndDepartmentId(RolUsuario role, Long departmentId)` para filtrar técnicos por departamento - RF: RF-9.
- [x] 1.6 Crear pruebas `@DataJpaTest` en `TareasRepositoryTest` y `ComentariosIncidenciasRepositoryTest` validando el filtrado por asignaciones activas, inactividad lógica y segregación de notas internas - RF: RF-8, RF-9, RF-10, RF-11, RF-20, RF-21, RF-22.

## [x] Tarea 2: DTOs, Enums de Validación y Mapeadores de Dominio - Depende de Tarea 1 - RF: RF-1, RF-2, RF-5, RF-6, RF-7, RF-8, RF-15, RF-16, RF-18, RF-20, RF-22
- [x] 2.1 Crear enum `SafetyRiskLevel` (`NONE`, `LOW`, `HIGH`) y DTOs de cálculo y actualización de prioridad (`PriorityCalculationRequest`, `PriorityCalculationResponse`, `UpdatePriorityRequest`) en `org.bulbaseros.dto` - RF: RF-1, RF-2.
- [x] 2.2 Crear DTOs de transiciones de ciclo de vida (`ValidateIncidentRequest`, `RejectIncidentRequest`, `ReevaluateIncidentRequest`, `ResolveIncidentRequest`, `ReopenIncidentRequest`) con validaciones de Bean Validation (`@NotBlank`, `@Size`) - RF: RF-5, RF-6, RF-7, RF-15, RF-16, RF-18.
- [x] 2.3 Crear DTOs de gestión de asignaciones (`AssignIncidentRequest`, `DeactivateAssignmentRequest`, `AssignmentResponse`, `DepartmentResponse`, `UserSummaryResponse`) - RF: RF-8, RF-10, RF-11.
- [x] 2.4 Crear DTOs de comentarios (`CreateCommentRequest`, `UpdateCommentRequest`, `CommentResponse`) - RF: RF-20, RF-22.
- [x] 2.5 Crear mapeadores desacoplados `AssignmentMapper`, `CommentMapper` y `DepartmentMapper` en `org.bulbaseros.mappers` documentados con Javadoc - RF: RF-8, RF-20, RF-22.
- [x] 2.6 Crear pruebas unitarias verificando conversiones bidireccionales de los mapeadores y validaciones de restricciones en DTOs en `AssignmentMapperTest`, `CommentMapperTest`, `DepartmentMapperTest` y `DtoValidationTest` - RF: RF-1, RF-2, RF-5, RF-6, RF-7, RF-8, RF-15, RF-16, RF-18, RF-20, RF-22.

## [x] Tarea 3: Servicios de Negocio: Priorización Asistida, Asignaciones Multidepartamentales y Comentarios - Depende de Tareas 1 y 2 - RF: RF-1, RF-2, RF-8, RF-9, RF-10, RF-11, RF-12, RF-17, RF-20, RF-21, RF-22, RF-23, RF-24
- [x] 3.1 Implementar `PriorityService` en `org.bulbaseros.service` con la función pura de cálculo asistido según la matriz determinista (corte de vía y nivel de seguridad) y redacción de `priorityReason`.
- [x] 3.2 Implementar en `AssignmentService` los métodos `assignDepartmentsAndTechnicians` y `deactivateAssignments` (para desactivación manual a petición de operador), garantizando la correspondencia técnico-departamento, la desactivación lógica y la reversión automática a `VALIDATED` si no restan departamentos activos.
- [x] 3.3 Implementar en `AssignmentService` el método `deactivateAllActiveAssignments(Long incidentId)` para soporte de inactivación automática al cerrar o rechazar.
- [x] 3.4 Implementar `CommentService` en `org.bulbaseros.service` con autorización granular (ciudadanos solo leen públicos de sus incidencias; personal lee todos), bloqueo en incidencias cerradas/rechazadas y edición con sellado de `updatedAt`.
- [x] 3.5 Crear pruebas unitarias con Mockito en `PriorityServiceTest`, `AssignmentServiceTest` y `CommentServiceTest` cubriendo todas las combinaciones y casos límite.

## [x] Tarea 4: Servicio de Orquestación del Ciclo de Vida y Transiciones de Estado (`IncidentTriageService`) - Depende de Tarea 3 - RF: RF-1, RF-2, RF-3, RF-4, RF-5, RF-6, RF-7, RF-13, RF-14, RF-15, RF-16, RF-17, RF-18, RF-19
- [x] 4.1 Implementar método `calculateAssistedPriority(Long incidentId, PriorityCalculationRequest request)` que delega en `PriorityService` con verificación de autorización para operadores/administradores.
- [x] 4.2 Implementar método `updatePriority(Long incidentId, UpdatePriorityRequest request)` registrando la prioridad definitiva y su justificación en la entidad `Incidencia` con auditoría.
- [x] 4.3 Implementar métodos `validateIncident(id, reason)` y `rejectIncident(id, reason)` con comprobación de motivo ≥ 5 caracteres, inactivación de asignaciones en rechazo vía `AssignmentService` e inserción en `CambiosEstadoRepository`.
- [x] 4.4 Implementar método de reevaluación (`reevaluateIncident`) con validación estricta de estado destino (`REPORTED` o `VALIDATED`) y motivo obligatorio.
- [x] 4.5 Implementar inicio de intervención técnica (`startIntervention`) verificando rol y asignación activa del técnico, transicionando desde `ASSIGNED` o `REOPENED` a `IN_PROGRESS`.
- [x] 4.6 Implementar reporte de resolución (`resolveIncident`) exigiendo justificación descriptiva de actuaciones ≥ 10 caracteres para prioridades medias, altas y críticas.
- [x] 4.7 Implementar cierre definitivo (`closeIncident`) y reapertura (`reopenIncident`) para operadores/administradores, inactivando asignaciones al cerrar vía `AssignmentService` y registrando motivos.
- [x] 4.8 Crear pruebas unitarias exhaustivas con Mockito en `IncidentTriageServiceTest` validando transiciones legales, estados bloqueados y auditoría de cambios.

## [ ] Tarea 5: Controladores REST, Seguridad RBAC y Documentación OpenAPI en Backend - Depende de Tarea 4 - RF: RF-1 al RF-24
- [ ] 5.1 Crear `IncidentTriageController` en `org.bulbaseros.controller` exponiendo endpoints de cálculo y actualización de prioridad, validación, rechazo, reevaluación, inicio de trabajo, resolución, cierre y reapertura, protegidos con anotaciones de seguridad (`hasRole`).
- [ ] 5.2 Exponer endpoints de asignaciones (`POST /api/incidents/{id}/assignments`, `POST /api/incidents/{id}/assignments/deactivate`) y comentarios (`GET /api/incidents/{id}/comments`, `POST /api/incidents/{id}/comments`, `PUT /api/incidents/{id}/comments/{commentId}`) en `IncidentTriageController`.
- [ ] 5.3 Crear `DepartmentController` en `org.bulbaseros.controller` exponiendo `GET /api/departments` y `GET /api/departments/{id}/technicians`.
- [ ] 5.4 Actualizar `SecurityConfig` garantizando las políticas de acceso por roles y rutas seguras.
- [ ] 5.5 Crear pruebas de integración en `IncidentTriageControllerTest` y `DepartmentControllerTest` con `MockMvc` y `@WithMockUser` verificando respuestas 200, 400, 401 y 403.

## [ ] Tarea 6: Servicios API y Modales de Triaje y Asignación en Frontend - Depende de Tarea 5 - RF: RF-1, RF-2, RF-8, RF-9, RF-10, RF-11, RF-12
- [ ] 6.1 Crear servicio cliente `frontend/src/services/triageService.js` con Axios documentado con JSDoc para interactuar con todos los endpoints de triaje, asignación, comentarios, departamentos y técnicos.
- [ ] 6.2 Ampliar `frontend/src/services/incidentService.js` con método `getIncidentPublicComments(incidentId)` para el seguimiento ciudadano.
- [ ] 6.3 Crear componente `PriorityAssistantModal.jsx` y su hoja de estilos para la selección de factores de riesgo, cálculo en tiempo real y confirmación de prioridad.
- [ ] 6.4 Crear componente `AssignmentModal.jsx` y su hoja de estilos con soporte para selección multidepartamento y selector dependiente de técnicos.
- [ ] 6.5 Crear pruebas unitarias con Vitest en `triageService.test.js`, `PriorityAssistantModal.test.jsx` y `AssignmentModal.test.jsx`.

## [ ] Tarea 7: Muro de Comentarios y Sección de Intervención Técnica en Frontend - Depende de Tarea 6 - RF: RF-13, RF-14, RF-15, RF-16, RF-20, RF-21, RF-22, RF-23, RF-24
- [ ] 7.1 Crear componente `IncidentCommentsSection.jsx` y estilos asociados, visualizando notas internas vs públicas, fecha de actualización para editados y formulario con selector de visibilidad.
- [ ] 7.2 Implementar modal/controles para intervención técnica: botón "Iniciar Trabajo" (`IN_PROGRESS`) y modal de resolución con validación de descripción de actuaciones (`RESOLVED`).
- [ ] 7.3 Actualizar `IncidentDetailModal.jsx` y `MyIncidentsPage.jsx` integrando la visualización de avisos públicos para seguimiento ciudadano.
- [ ] 7.4 Crear pruebas unitarias con Vitest en `IncidentCommentsSection.test.jsx` comprobando renderizado según rol, bloqueo en cerradas y edición de comentarios.

## [ ] Tarea 8: Integración en Panel Operativo, Pruebas de Integración y Preparación de QA - Depende de Tareas 5, 6 y 7 - RF: Todos (RF-1 al RF-24)
- [ ] 8.1 Integrar en `OperatorPanelPage.jsx` los modales de la Tarea 6 (asistente de prioridad y asignación departamental).
- [ ] 8.2 Integrar en `OperatorPanelPage.jsx` los controles de intervención y resolución técnica de la Tarea 7 y el muro de comentarios.
- [ ] 8.3 Integrar botones de acción contextuales según estado (Validar, Rechazar, Reevaluar, Asignar, Resolver, Cerrar, Reabrir) y badges de departamentos asignados.
- [ ] 8.4 Crear prueba de integración en `OperatorPanelPage.test.jsx` validando la secuencia completa de triaje y visualización reactiva.
- [ ] 8.5 Ejecutar verificación completa de tests en Backend (`cd Backend && mvn clean test`) asegurando 100% de tests en verde y reporte JaCoCo.
- [ ] 8.6 Ejecutar suite de pruebas de Frontend (`cd frontend && npm test`) y linter (`npm run lint`) garantizando 0 errores y advertencias.
- [ ] 8.7 Redactar documento técnico y guía de pruebas de la feature en `docs/005-gestion-y-triaje-operativo-de-incidencias/qa-note.md` para la posterior fase de validación (`sdd-validate`).

---
## Mantenimiento (checklist recurrente)

- [ ] Verificar que ninguna operación de cambio de estado omita la inserción obligatoria en `status_changes`.
- [ ] Comprobar que los comentarios con `is_internal = true` nunca sean serializados en respuestas dirigidas a ciudadanos.
- [ ] Asegurar que toda asignación técnica activa pertenezca siempre a un departamento activo de la misma incidencia.
- [ ] Validar que las transiciones a `RESOLVED` en incidencias no menores mantengan la obligatoriedad de la descripción.

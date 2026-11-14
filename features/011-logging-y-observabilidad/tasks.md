# 011 — Logging y Observabilidad — Tareas

_Checklist accionable derivado del `plan.md`. Tareas principales y subtareas pequeñas y concretas; marca `[x]` al completarlas._

## [ ] Tarea 1: Registro de errores no controlados en backend (RF: RF-1, RNF: NFR-2)

- [ ] Añadir logger con `@Slf4j` al `GlobalExceptionHandler`.
- [ ] Registrar errores no controlados con `log.error` e incluir stacktrace.
- [ ] Validar contra los criterios de aceptación de `spec.md`.

## [ ] Tarea 2: Eventos de seguridad y autenticación (RF: RF-2, RNF: NFR-1)

- [ ] Registrar token inválido/expirado y acceso denegado con nivel `WARN` y URI de la petición.
- [ ] Aplicar política de no-redacción de credenciales en logs.
- [ ] Validar contra los criterios de aceptación de `spec.md`.

## [ ] Tarea 3: Cobertura de logs en services faltantes y controllers (RF: RF-3, RF-4)

- [ ] Añadir `@Slf4j` a los services sin logger: `AuthService`, `AssignmentService`, `CommentService`, `DuplicateDetectionService`, `IncidentTriageService`, `NotificationService`, `PriorityService`, `UrbanAssetService`, `AttachmentService`.
- [ ] Registrar fallos recuperables previos a fallback (RF-3) y contexto de incidencia en eventos de creación/cambio de estado (RF-4).
- [ ] Validar contra los criterios de aceptación de `spec.md`.

## [ ] Tarea 4: Configuración de niveles de log por perfil (RF: RF-6)

- [ ] Configurar niveles de log en `application.properties` y `application-dev.properties` para ERROR, WARN, INFO y DEBUG.
- [ ] Validar contra los criterios de aceptación de `spec.md`.

## [ ] Tarea 5: Logger centralizado en frontend y reemplazo de console (RF: RF-5)

- [ ] Crear utilidad de logging centralizado en `frontend/src/utils/logger.js`.
- [ ] Reemplazar `console.error` en `incidentService.js`, `CreateIncidentModal.jsx` e `IncidentDetailModal.jsx`.
- [ ] Validar contra los criterios de aceptación de `spec.md`.

---

## Mantenimiento (checklist recurrente)

- [ ] Revisar niveles de log y eventos registrados en futuras modificaciones de flujos críticos.

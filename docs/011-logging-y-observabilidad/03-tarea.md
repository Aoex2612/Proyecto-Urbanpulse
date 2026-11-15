# 011 — Logging y Observabilidad — Tarea 3: Cobertura en servicios

## Resumen

Se añadieron `@Slf4j` a los nueve servicios indicados en el plan y se reforzaron los logs de negocio de `IncidentService`: creación y transición de estado incluyen el identificador de incidencia con nivel `INFO`; los fallos recuperables registran `WARN` antes de activar sus fallbacks.

## Pruebas

- `mvn -q -Dtest=IncidentServiceTest test` — correcto.
- Suite focalizada de servicios — correcta.

## Requisitos

Implementación dirigida a RF-3 y RF-4. La validación formal contra los criterios de aceptación queda reservada a `sdd-validate`.

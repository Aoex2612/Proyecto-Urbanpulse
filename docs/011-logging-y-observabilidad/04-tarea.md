# 011 — Logging y Observabilidad — Tarea 4: Niveles por perfil

## Resumen

Se configuraron niveles de logging por perfil en `application.properties` y `application-dev.properties`. Producción limita el ruido global y conserva `INFO` para UrbanPulse; desarrollo habilita `DEBUG` para el paquete de la aplicación y mantiene niveles restrictivos para Spring e Hibernate.

## Verificación

- `mvn -q -DskipTests validate` — correcto.
- `git diff --check` — correcto.

## Requisitos

Implementación dirigida a RF-6. La validación formal contra los criterios de aceptación queda reservada a `sdd-validate`.

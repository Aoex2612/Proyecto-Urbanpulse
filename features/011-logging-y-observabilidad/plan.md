# 011 — Logging y Observabilidad — Plan

_¿Cómo se implementa lo descrito en `spec.md`? Respeta estrictamente la `constitution/` y los principios de diseño de UrbanPulse._

## Enfoque

La solución completa la cobertura de logs en el backend y centraliza la captura de errores en el frontend, respetando el patrón técnico ya adoptado en el proyecto: **SLF4J con Lombok `@Slf4j`** en Java y **utilidades propias en `src/utils`** en React. No se altera la lógica de negocio, solo se añade observabilidad.

## Implementación

_Pasos técnicos concretos, en orden. Indica los archivos/módulos que se tocan._

### 1. Backend — Registro de errores no controlados

- `Backend/src/main/java/org/bulbaseros/exception/GlobalExceptionHandler.java`:
  - Añadir `@Slf4j` e inyectar el logger.
  - Registrar con `log.error(...)` (incluyendo stacktrace) los errores no controlados capturados (RF-1).

### 2. Backend — Eventos de seguridad y autenticación

- `Backend/src/main/java/org/bulbaseros/security/` (handlers de entrada/denegación y validación JWT):
  - Registrar eventos de token inválido/expirado y acceso denegado con nivel `WARN` y URI de la petición (RF-2, decisión resuelta de la Opción A).

### 3. Backend — Cobertura en services y controllers faltantes

- Añadir `@Slf4j` a los services sin logger: `AuthService`, `AssignmentService`, `CommentService`, `DuplicateDetectionService`, `IncidentTriageService`, `NotificationService`, `PriorityService`, `UrbanAssetService`, `AttachmentService`.
- Registrar logs de negocio: fallos recuperables previos a fallback (RF-3) y contexto de incidencia en eventos de creación/cambio de estado (RF-4).

### 4. Backend — Configuración de niveles

- `Backend/src/main/resources/application.properties` y `application-dev.properties`:
  - Configurar niveles de log por perfil para ERROR, WARN, INFO y DEBUG (RF-6).

### 5. Frontend — Logger centralizado

- Crear utilidad de logging centralizado (por ejemplo, `frontend/src/utils/logger.js`) con niveles error/warn/info.
- Reemplazar `console.error` en `frontend/src/services/incidentService.js`, `frontend/src/components/Map/CreateIncidentModal.jsx` y `frontend/src/components/Map/IncidentDetailModal.jsx`.
- Registrar errores de operaciones async con el logger (RF-5).

## Decisiones

_Elecciones de diseño relevantes y su justificación. Alternativas descartadas y por qué._

- **SLF4J con Lombok `@Slf4j`** — Se reutiliza la dependencia y el patrón ya presentes en el proyecto, descartando introducir Log4j2 o slf4j-simple por cambio innecesario de stack.
- **Logger utilitario propio en frontend** — Se implementa una utilidad ligera en `src/utils` en lugar de adoptar librerías como pino/winston, descartándolas por sobrecarga de bundle y configuración para el volumen actual de errores.
- **Niveles de seguridad definidos (Opción A resuelta)** — Token inválido/expirado y acceso denegado se registran con nivel `WARN`; credenciales y tokens completos quedan excluidos de los logs.

## Riesgos

_Qué puede salir mal o requerir cuidado, y cómo se mitiga._

- **Riesgo: Fuga de información sensible en logs (NFR-1).**
  - Mitigación: política de no-log de contraseñas/tokens y revisión de mensajes antes de registrar.
- **Riesgo: Ruido en los logs por eventos no priorizados.**
  - Mitigación: asignación de niveles adecuados (ERROR/WARN/INFO/DEBUG) según el diseño de la spec.

## Estrategia de tests

_Estrategia que se seguirá para los tests._

- Backend: pruebas unitarias de `GlobalExceptionHandler` (verificar log de nivel ERROR con stacktrace), handlers de seguridad y services con fallback.
- Frontend: pruebas del logger centralizado y verificación de que los servicios/componentes usan el logger en lugar de `console`.

---

_Notas: la feature NO requiere nuevo ADR porque no se adoptan decisiones arquitectónicas estructurales nuevas (patrón ya existente)._

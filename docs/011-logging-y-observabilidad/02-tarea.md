# 011 — Logging y Observabilidad — Tarea 2: Eventos de seguridad y autenticación (RF: RF-2, NFR: NFR-1)

## Resumen global

Se completó la Tarea 2 de la feature 011: se implementó el registro de eventos de seguridad y autenticación con nivel `WARN`, incluyendo la URI de la petición y el motivo del fallo, y se aplicó la política de no-redacción de credenciales en los mensajes de log.

En el flujo de autenticación del backend se centralizó el evento de token inválido/expirado en el método `handleAuthenticationFailure` del `JwtAuthenticationFilter`, invocando `log.warn("Token de autenticación inválido o expirado en la ruta {}: {}", request.getRequestURI(), errorMessage)` antes de delegar en el `JwtAuthenticationEntryPoint`. Los `log.debug` que existían en los bloques catch del filtro fueron sustituidos por este registro centralizado de seguridad con nivel WARN.

Asimismo, se añadió logger con `LoggerFactory.getLogger(...)` (logger estático consistente con el resto del paquete `security`) al `JwtAuthenticationEntryPoint` y al `CustomAccessDeniedHandler`, registrando con `log.warn` el evento de autenticación fallida o acceso denegado, incluyendo la URI de la petición.

El contrato HTTP existente no se alteró: los códigos 401/403 y los mensajes del `ErrorResponse` se mantienen sin cambios.

Ficheros modificados:

- `Backend/src/main/java/org/bulbaseros/security/JwtAuthenticationFilter.java`
- `Backend/src/main/java/org/bulbaseros/security/JwtAuthenticationEntryPoint.java`
- `Backend/src/main/java/org/bulbaseros/security/CustomAccessDeniedHandler.java`
- `Backend/src/test/java/org/bulbaseros/security/JwtAuthenticationFilterTest.java`
- `Backend/src/test/java/org/bulbaseros/security/JwtAuthenticationEntryPointTest.java`
- `Backend/src/test/java/org/bulbaseros/security/CustomAccessDeniedHandlerTest.java`

## Subtareas

### 1. Registrar token inválido/expirado y acceso denegado con nivel WARN y URI de la petición

- Se eliminó el `log.debug` de los catch (`ExpiredJwtException` y `JwtException | IllegalArgumentException`) y se registró el evento de seguridad con `log.warn` en `handleAuthenticationFailure`, con el formato `ruta: <uri>` y el motivo (`errorMessage`), antes de delegar en el entry point.
- El `JwtAuthenticationEntryPoint` registra con `log.warn` el evento de autenticación fallida (`Token de autenticación ausente o inválido en la ruta {}`) incluyendo la URI de la petición y la excepción de autenticación.
- El `CustomAccessDeniedHandler` registra con `log.warn` el evento de acceso denegado (`Acceso denegado en la ruta {}`) incluyendo la URI de la petición y la excepción.

### 2. Aplicar política de no-redacción de credenciales en logs

- Los mensajes de log solo incluyen la URI de la petición, el motivo o identidad/rol del fallo; nunca tokens completos, contraseñas ni credenciales (NFR-1).

## Decisiones de diseño

- Se registró el evento de seguridad en `handleAuthenticationFailure`, único punto de delegación en el flujo de fallo de autenticación del filtro, de modo que cubre token expirado, token malformado y email ausente/inválido con un único evento WARN.
- Se mantuvo el logger estático (`private static final Logger log = LoggerFactory.getLogger(...)`), consistente con el resto del paquete `security`, en lugar de `@Slf4j`.

## Problemas y solución

- Los tests de log siguen el patrón del `ListAppender` de Logback ya aplicado en la Tarea 1 del `GlobalExceptionHandler` (`LoggerFactory.getLogger(...)`, appender `ListAppender`, nivel DEBUG, assertions sobre `Level`, `getFormattedMessage()` y colección `list`).

## Comandos de test ejecutados

```bash
cd Backend && mvn -q -Dtest='JwtAuthenticationFilterTest,JwtAuthenticationEntryPointTest,CustomAccessDeniedHandlerTest' test
# Tests run: 37, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

## Requisitos cubiertos

RF-2 y NFR-1 en la capa de seguridad: el evento de token inválido/expirado y acceso denegado se registra con nivel WARN, incluyendo la URI de la petición y el motivo; los mensajes de log no contienen tokens completos ni credenciales.

La validación formal contra los criterios de aceptación de `spec.md` (caso RF-2: "La RF-2 quedará verificada con test que confirme el registro del evento de validación JWT o acceso denegado") se enmarca en la fase de validación posterior (`sdd-validate`).

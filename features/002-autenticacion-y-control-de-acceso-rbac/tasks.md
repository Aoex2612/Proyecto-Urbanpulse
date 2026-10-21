# 002 — Autenticación y Control de Acceso por Roles (RBAC) — Tareas

_Checklist accionable derivada del `plan.md`. Tareas principales y subtareas pequeñas y concretas; marca `[x]` al completarlas._

## [x] Tarea 1: Manejadores de Excepciones de Seguridad (401 y 403) en Backend - RF: RF-1, RF-2, RF-5
- [x] 1.1 Implementar `JwtAuthenticationEntryPoint` (`AuthenticationEntryPoint`) para capturar fallos de autenticación (401) y serializar un JSON con la estructura oficial `ErrorResponse` (`timestamp`, `status: 401`, `error: "Unauthorized"`, `message: "Token de autenticación ausente o inválido"`, `path`) documentado con Javadoc.
- [x] 1.2 Implementar `CustomAccessDeniedHandler` (`AccessDeniedHandler`) para capturar denegaciones de permisos por rol (403) y serializar un JSON con `ErrorResponse` (`timestamp`, `status: 403`, `error: "Forbidden"`, `message: "Acceso denegado: permisos insuficientes"`, `path`) documentado con Javadoc.
- [x] 1.3 Configurar `ObjectMapper` con `JavaTimeModule` en los handlers para garantizar la serialización correcta de `OffsetDateTime`.
- [x] 1.4 Crear pruebas unitarias para `JwtAuthenticationEntryPoint` y `CustomAccessDeniedHandler`.

## [x] Tarea 2: Filtro de Autenticación JWT (`JwtAuthenticationFilter`) en Backend - Depende de Tarea 1 - RF: RF-1, RF-2, RF-3, RF-4
- [x] 2.1 Crear clase `JwtAuthenticationFilter` que extienda `OncePerRequestFilter` con Javadoc completo.
- [x] 2.2 Implementar extracción de cabecera `Authorization` tolerando variantes de mayúsculas/minúsculas (`Bearer` / `bearer`) y espacios múltiples.
- [x] 2.3 Validar token con `JwtService`, extraer `sub` y `role`, e inyectar `UsernamePasswordAuthenticationToken` con autoridad `ROLE_<ROL>` en `SecurityContextHolder`.
- [x] 2.4 Manejar excepciones (`JwtException`, `ExpiredJwtException`, etc.) delegando de forma segura en `JwtAuthenticationEntryPoint` sin propagar error 500.
- [x] 2.5 Excluir o permitir paso transparente sin validación en rutas públicas (`/api/auth/**`, `/h2-console/**`).
- [x] 2.6 Crear pruebas unitarias para `JwtAuthenticationFilterTest` cubriendo tokens válidos con roles `CITIZEN`/`OPERATOR`/`ADMIN`, cabecera ausente, prefijos con mayúsculas/minúsculas y token expirado.

## [x] Tarea 3: Configuración de Seguridad y Reglas de Autorización RBAC - Depende de Tareas 1 y 2 - RF: RF-3, RF-5, RF-6
- [x] 3.1 Registrar en `SecurityConfig` los beans `JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint` y `CustomAccessDeniedHandler`.
- [x] 3.2 Configurar `exceptionHandling` asociando el entry point (401) y el handler de acceso denegado (403).
- [x] 3.3 Configurar reglas de autorización: rutas públicas `/api/auth/**` con `permitAll()`, ruta `PATCH /api/incidents/*/status` con `.hasAnyRole("OPERATOR", "ADMIN")`, y resto con `.authenticated()`.
- [x] 3.4 Añadir `JwtAuthenticationFilter` antes de `UsernamePasswordAuthenticationFilter` en el `SecurityFilterChain`.
- [x] 3.5 Actualizar pruebas existentes en `SecurityConfigTest` para validar la nueva configuración de seguridad.

## [x] Tarea 4: DTOs y Controlador de Triaje de Incidencias en Backend - Depende de Tarea 3 - RF: RF-5, RF-6
- [x] 4.1 Crear DTOs `UpdateIncidentStatusRequest` y `IncidentStatusResponse` con validación y Javadoc.
- [x] 4.2 Implementar `IncidentController` con endpoint `PATCH /api/incidents/{id}/status` retornando HTTP 200 OK y confirmación de estado para usuarios con roles autorizados (`OPERATOR` y `ADMIN`).
- [x] 4.3 Crear pruebas de integración en `IncidentControllerSecurityTest`:
  - Petición sin token responde HTTP 401 Unauthorized (`ErrorResponse`).
  - Petición con rol `CITIZEN` responde HTTP 403 Forbidden (`ErrorResponse`).
  - Petición con rol `OPERATOR` responde HTTP 200 OK.
  - Petición con rol `ADMIN` responde HTTP 200 OK.

## [x] Tarea 5: Componente `RoleGuard`, Extensión de `ProtectedRoute` y Manejo Axios en Frontend - RF: RF-7, RF-8, RF-10, RF-12
- [x] 5.1 Añadir funciones helper en `authService.js` (`getUserRole()` normalizado y `hasAnyRole(allowedRoles)` con JSDoc).
- [x] 5.2 Ajustar interceptor de respuestas en `api.js` para asegurar que HTTP 403 no ejecute logout ni elimine credenciales.
- [x] 5.3 Implementar componente `RoleGuard.jsx` con JSDoc que evalúe si el rol del usuario coincide con `allowedRoles`; si no coincide, redirigir a `/` transmitiendo estado de navegación `accessDenied: true` y mensaje explicativo sin invocar `logout()`.
- [x] 5.4 Actualizar `ProtectedRoute.jsx` con JSDoc para admitir opcionalmente la prop `allowedRoles`, componiendo internamente `RoleGuard` si el usuario está autenticado.

## [x] Tarea 6: Vistas Frontend del Panel de Operador y Gestión de Acceso Denegado - Depende de Tarea 5 - RF: RF-8, RF-9, RF-10, RF-11
- [x] 6.1 Crear componente `OperatorPanelPage.jsx` con interfaz informativa de triaje para roles `OPERATOR` y `ADMIN` con JSDoc.
- [x] 6.2 Actualizar `HomePage.jsx` para detectar `location.state.accessDenied`, mostrando un banner/alerta visual de advertencia y limpiando el estado al descartar o navegar.
- [x] 6.3 Añadir acceso directo o enlace visible en `HomePage.jsx` hacia `/operator-panel` para usuarios con rol `OPERATOR` o `ADMIN`.
- [x] 6.4 Configurar la ruta `/operator-panel` en `App.jsx` protegida con `ProtectedRoute` y `allowedRoles={['OPERATOR', 'ADMIN']}`.
- [x] 6.5 Validar frontend con `npm run lint` y `npm run build` sin errores ni advertencias.

## [x] Tarea 7: Verificación Integral de Criterios de Aceptación - Depende de Tareas 4 y 6 - RF: Todos
- [x] 7.1 Ejecutar suite completa de tests de Backend con Maven y verificar 100% de éxito.
- [x] 7.2 Validar contra todos los criterios de aceptación del `spec.md`.
- [x] 7.3 Actualizar checklist de tareas y preparar notas de validación QA (`qa-note.md`).
- [x] 7.4 Redactar la documentación de la tarea en `docs/002-autenticacion-y-control-de-acceso-rbac/07-tarea.md`.

---
## Mantenimiento (checklist recurrente)

- [x] Verificar que los tests de Maven y `npm run lint` pasen limpios en cada commit.

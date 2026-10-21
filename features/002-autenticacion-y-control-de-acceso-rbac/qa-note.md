# QA Note — Validación de Feature 002: Autenticación y Control de Acceso por Roles (RBAC)

**Fecha de validación:** 2026-10-21  
**Rol evaluador:** QA Engineer  
**Feature:** `002-autenticacion-y-control-de-acceso-rbac`  
**Rama:** `featute/HU2`  
**Veredicto final:** ✅ **APROBADA (Cumplimiento al 100%)**

---

## 1. Trazabilidad de Requisitos Funcionales (EARS) y Cobertura de Tests

| ID Requisito | Descripción EARS | Tests / Mecanismo de Validación | Resultado |
|---|---|---|---|
| **RF-1** | CUANDO petición HTTP arriba a endpoint protegido sin `Authorization` o sin formato `Bearer <token>` (case-insensitive), EL SISTEMA deniega con HTTP 401 y `ErrorResponse` JSON (`message: "Token de autenticación ausente o inválido"`) | `JwtAuthenticationEntryPointTest.commence_ShouldReturn401WithStandardErrorResponse`<br>`JwtAuthenticationFilterTest.doFilterInternal_MissingAuthorizationHeader_ShouldInvokeEntryPoint`<br>`IncidentControllerSecurityTest.shouldRejectUnauthenticatedRequestWith401` | ✅ PASA |
| **RF-2** | SI token JWT ha expirado, está malformado o su firma criptográfica no es válida en ruta protegida, ENTONCES EL SISTEMA deniega con HTTP 401 y `ErrorResponse` JSON (`message: "Token de autenticación expirado o inválido"`) | `JwtAuthenticationFilterTest.doFilterInternal_ExpiredToken_ShouldInvokeEntryPoint`<br>`JwtAuthenticationFilterTest.doFilterInternal_MalformedToken_ShouldInvokeEntryPoint`<br>`JwtAuthenticationFilterTest.doFilterInternal_SignatureException_ShouldInvokeEntryPoint`<br>`JwtAuthenticationEntryPointTest.commence_ShouldPreserveSpecificExceptionMessage` | ✅ PASA |
| **RF-3** | CUANDO petición arriba a ruta pública permitida (`/api/auth/**`, `/h2-console/**`), EL SISTEMA ignora ausencia o invalidez del token permitiendo tramitación normal sin emitir error 401 | `JwtAuthenticationFilterTest.shouldNotFilter_PublicAuthEndpoints_ShouldReturnTrue`<br>`JwtAuthenticationFilterTest.shouldNotFilter_H2ConsoleEndpoints_ShouldReturnTrue`<br>`SecurityConfigTest.publicEndpoints_ShouldBeAccessibleWithoutAuth` | ✅ PASA |
| **RF-4** | CUANDO token JWT provisto en endpoint protegido es válido, EL SISTEMA extrae `sub` y `role`, inyectando `UsernamePasswordAuthenticationToken` con autoridad `ROLE_<ROL>` en `SecurityContextHolder` | `JwtAuthenticationFilterTest.doFilterInternal_ValidTokenWithRole_ShouldSetAuthenticationWithPrefixedRole`<br>`JwtAuthenticationFilterTest.doFilterInternal_ValidTokensForAllRoles_ShouldAuthenticateCorrectly` (`CITIZEN`, `OPERATOR`, `ADMIN`) | ✅ PASA |
| **RF-5** | CUANDO usuario autenticado con rol `CITIZEN` (u otro sin permisos de triaje) invoca `PATCH /api/incidents/{id}/status`, EL SISTEMA deniega con HTTP 403 Forbidden y `ErrorResponse` JSON (`message: "Acceso denegado: permisos insuficientes"`) | `CustomAccessDeniedHandlerTest.handle_ShouldReturn403WithStandardErrorResponse`<br>`IncidentControllerSecurityTest.shouldRejectCitizenUserWith403`<br>`IncidentControllerSecurityTest.shouldRejectRealJwtTokenWithCitizenRoleWith403` | ✅ PASA |
| **RF-6** | CUANDO usuario autenticado con rol `OPERATOR` o `ADMIN` invoca `PATCH /api/incidents/{id}/status`, EL SISTEMA autoriza y retorna respuesta HTTP 200 OK con `IncidentStatusResponse` | `IncidentControllerSecurityTest.shouldAuthorizeOperatorUserWith200`<br>`IncidentControllerSecurityTest.shouldAuthorizeAdminUserWith200`<br>`IncidentControllerSecurityTest.shouldAuthorizeRealJwtTokenWithOperatorRoleWith200`<br>`IncidentControllerSecurityTest.shouldAuthorizeRealJwtTokenWithAdminRoleWith200` | ✅ PASA |
| **RF-7** | SI usuario anónimo intenta navegar a una ruta protegida (`/operator-panel`), ENTONCES EL SISTEMA lo redirige a la pantalla de `/login` | `ProtectedRoute.jsx` (`!isAuthenticated()` redirige con `<Navigate to="/login" replace />`) | ✅ PASA |
| **RF-8** | SI usuario autenticado intenta acceder a ruta protegida cuyos roles autorizados excluyen su rol (`CITIZEN` en `/operator-panel`), ENTONCES EL SISTEMA lo redirige a `/` con `state: { accessDenied: true, message: ... }` | `RoleGuard.jsx` (`!hasAnyRole(allowedRoles)` redirige a `/` transmitiendo `location.state` sin invocar logout) | ✅ PASA |
| **RF-9** | CUANDO vista principal (`/`) detecta en su estado de navegación una alerta de acceso denegado, EL SISTEMA renderiza un banner visual destacando la advertencia al usuario | `HomePage.jsx` (banner reactivo con rol `alert`, botón de descarte y limpieza con `replaceState`) | ✅ PASA |
| **RF-10** | MIENTRAS usuario experimente denegación de acceso por rol no autorizado en frontend o backend (HTTP 403), EL SISTEMA mantiene la sesión del usuario intacta en `localStorage` sin ejecutar `logout` ni eliminar token | `api.js` (interceptor excluye el status 403 de la purga de credenciales)<br>`RoleGuard.jsx` (redirección no destructiva) | ✅ PASA |
| **RF-11** | CUANDO usuario autenticado con rol `OPERATOR` o `ADMIN` accede a la ruta `/operator-panel`, EL SISTEMA renderiza la vista del panel de gestión operativa | `OperatorPanelPage.jsx` y `App.jsx` (`<ProtectedRoute allowedRoles={['OPERATOR', 'ADMIN']}>`) | ✅ PASA |
| **RF-12** | EL SISTEMA modulará el control de acceso en frontend mediante la composición de componentes (`ProtectedRoute` para autenticación y `RoleGuard` para verificación de roles específicos) | `ProtectedRoute.jsx` (soporte de prop opcional `allowedRoles` delegando internamente en `RoleGuard`) | ✅ PASA |

---

## 2. Evaluación de Requisitos No Funcionales (RNF)

1. **Seguridad Perimetral y Sin Estado (Stateless):**
   - Inspección stateless mediante `JwtAuthenticationFilter` extendiendo `OncePerRequestFilter`, ejecutado antes de `UsernamePasswordAuthenticationFilter`.
   - Extracción tolerante a mayúsculas/minúsculas (`Bearer`, `bearer`) y múltiples espacios en blanco (`\\s+`).
   - Propagación controlada de excepciones (`JwtException`, `ExpiredJwtException`, etc.) delegando en `JwtAuthenticationEntryPoint` sin filtrar excepciones no controladas ni exponer trazas HTTP 500.
   - Restricción perimetral de endpoints de gestión con `.hasAnyRole("OPERATOR", "ADMIN")` configurado en `SecurityConfig`.
   - *Resultado: CUMPLIDO.*

2. **Estandarización de Respuestas de Error (DTO `ErrorResponse`):**
   - Ambas respuestas de seguridad (HTTP 401 y HTTP 403) se serializan con la estructura estricta de `ErrorResponse` (`timestamp`, `status`, `error`, `message`, `path`).
   - Configuración de `ObjectMapper` con `JavaTimeModule` e inhabilitación de `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS` para formato ISO-8601 estándar (`OffsetDateTime`).
   - *Resultado: CUMPLIDO.*

3. **Resiliencia y Experiencia de Usuario (UX) en Frontend:**
   - La denegación de autorización (403) no expulsa al usuario ni invalida su token en `localStorage`.
   - Mensaje de advertencia amigable y descartable en la página de inicio ante redirecciones de guardias de rol.
   - Despliegue condicional de elementos de navegación: los botones y enlaces al panel de operador sólo se muestran a usuarios con roles operativos autorizados.
   - *Resultado: CUMPLIDO.*

---

## 3. Revisión de Calidad de Código y Estándares

- **Backend (Java / Spring Boot):**
  - Todas las clases, interfaces, métodos y atributos cuentan con documentación **Javadoc** exhaustiva (`@param`, `@return`, `@throws`).
  - Suite de pruebas de Maven ejecutada: **116 tests ejecutados, 0 fallos, 0 errores, 0 omitidos (`BUILD SUCCESS`)**.
- **Frontend (React 19 / Vite):**
  - Documentación con **JSDoc** en componentes (`RoleGuard`, `OperatorPanelPage`, `ProtectedRoute`) y servicios (`authService`, `api`).
  - Validación con ESLint: **0 errores, 0 advertencias**.
  - Compilación de producción con Vite: **`vite build` exitoso con 90 módulos transformados**.

---

## 4. Comprobación de Criterios de Aceptación de `spec.md`

- [x] Filtro backend (`JwtAuthenticationFilter`) implementado e integrado en `SecurityConfig`, extrayendo rol y registrando las autoridades en `SecurityContextHolder`.
- [x] Peticiones sin token o con token corrupto/expirado a endpoints protegidos responden HTTP 401 con DTO `ErrorResponse` en JSON.
- [x] Rutas públicas (`/api/auth/**`, `/h2-console/**`) permanecen accesibles sin token o con token inválido.
- [x] Endpoint `PATCH /api/incidents/{id}/status` protegido exclusivamente para roles `OPERATOR` y `ADMIN`.
- [x] Peticiones de usuarios con rol `CITIZEN` a `PATCH /api/incidents/{id}/status` son rechazadas con HTTP 403 Forbidden y DTO `ErrorResponse`.
- [x] Componente `RoleGuard` implementado y compuesto con `ProtectedRoute` en el Frontend.
- [x] Navegación de usuario anónimo a `/operator-panel` redirige a `/login`.
- [x] Navegación de usuario con rol `CITIZEN` a `/operator-panel` redirige a `/` mostrando alerta visual sin cerrar sesión.
- [x] Navegación de usuario con rol `OPERATOR` o `ADMIN` a `/operator-panel` renderiza correctamente el panel.
- [x] Pruebas unitarias y de integración de backend y frontend ejecutadas y en verde.

---

## 5. Veredicto del QA

> **¿La especificación está cumplida?**  
> **SÍ.** Todos los requisitos funcionales, no funcionales y criterios de aceptación se cumplen estrictamente sin desviaciones ni errores. No se requiere archivo auxiliar de reparaciones (`repare.md`). La feature está lista para ser integrada a la rama principal mediante Pull Request.

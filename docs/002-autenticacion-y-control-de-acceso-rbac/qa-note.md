# QA Note — Validación de Feature 002: Autenticación y Control de Acceso por Roles (RBAC)

**Fecha de validación:** 2026-10-22  
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
  - Documentación **Javadoc** en clases, interfaces, métodos y atributos (`@param`, `@return`, `@throws`).
  - Suite de pruebas de Maven ejecutada: **116 tests ejecutados, 0 fallos, 0 errores, 0 omitidos (`BUILD SUCCESS`)**.
- **Frontend (React 19 / Vite):**
  - Documentación con **JSDoc** en componentes (`RoleGuard`, `OperatorPanelPage`, `ProtectedRoute`) y servicios (`authService`, `api`).
  - Validación con ESLint: **0 errores, 0 advertencias**.
  - Compilación de producción con Vite: **`vite build` exitoso con 90 módulos transformados**.

---

## 4. Comprobación de Criterios de Aceptación de `spec.md`

- [x] Filtro backend (`JwtAuthenticationFilter`) implementado e integrado en `SecurityConfig`, extrayendo rol y registrando autoridades en `SecurityContextHolder`.
- [x] Peticiones sin token o con token inválido/expirado a endpoints protegidos responden HTTP 401 con DTO `ErrorResponse` en JSON.
- [x] Rutas públicas (`/api/auth/**`, `/h2-console/**`) permanecen accesibles sin token.
- [x] Endpoint `PATCH /api/incidents/{id}/status` protegido exclusivamente para roles `OPERATOR` y `ADMIN`.
- [x] Peticiones de usuarios con rol `CITIZEN` a `PATCH /api/incidents/{id}/status` reciben HTTP 403 Forbidden y DTO `ErrorResponse`.
- [x] Componente `RoleGuard` implementado y compuesto con `ProtectedRoute` en el Frontend.
- [x] Navegación anónima a `/operator-panel` redirige a `/login`.
- [x] Navegación de `CITIZEN` a `/operator-panel` redirige a `/` mostrando alerta visual sin cerrar sesión.
- [x] Navegación de `OPERATOR` o `ADMIN` a `/operator-panel` renderiza el panel correctamente.
- [x] Suite completa de pruebas unitarias y de integración en verde.

---

## 5. Guía de Pruebas Manuales Paso a Paso (Para Humanos)

Esta guía permite a cualquier persona comprobar manualmente el funcionamiento de la autenticación y control de acceso (RBAC) en pocos minutos de forma sencilla.

### Paso 0: Levantar el entorno local
1. En una terminal, inicia el backend:
   ```bash
   cd Backend && mvn spring-boot:run
   ```
2. En otra terminal, inicia el frontend:
   ```bash
   cd frontend && npm run dev
   ```
3. Abre tu navegador en `http://localhost:5173`.

---

### Prueba 1: Acceso no autenticado (Usuario anónimo)
1. Con la sesión cerrada, escribe directamente en la barra de direcciones del navegador:  
   👉 `http://localhost:5173/operator-panel`
2. **Resultado esperado:**  
   El sistema no te permite entrar y te redirige automáticamente a `http://localhost:5173/login`.

---

### Prueba 2: Acceso no autorizado (Rol Ciudadano / `CITIZEN`)
1. Inicia sesión en `/login` con una cuenta de ciudadano (o regístrate en `/register`).
2. Una vez dentro de la app (en `/`), intenta forzar la navegación al panel de operador escribiendo en la URL:  
   👉 `http://localhost:5173/operator-panel`
3. **Resultado esperado:**  
   - No te deja entrar al panel de operador.
   - Te devuelve a la página de inicio (`/`).
   - Muestra un banner de advertencia avisando que no tienes permisos de operador.
   - **Tu sesión continúa iniciada** (tu nombre sigue apareciendo y no se te ha cerrado la sesión).

---

### Prueba 3: Acceso autorizado (Rol Operador o Administrador)
1. Cierra sesión y entra con una cuenta que tenga rol `OPERATOR` o `ADMIN`.
2. En la barra de navegación verás visible el enlace/botón **"Panel de Operador"**.
3. Haz clic en el botón o navega a `http://localhost:5173/operator-panel`.
4. **Resultado esperado:**  
   Carga correctamente la pantalla del **Panel de Operador**, mostrando las opciones de gestión de incidencias sin ningún bloqueo.

---

### Prueba 4: Validación de seguridad en Backend (API REST)
Abre una terminal y ejecuta estas tres peticiones rápidas:

1. **Petición sin token (debe fallar con 401):**
   ```bash
   curl -i -X PATCH http://localhost:8080/api/incidents/1/status
   ```
   *Respuesta esperada:* Código `HTTP/1.1 401 Unauthorized` con mensaje JSON indicando token ausente.

2. **Petición con token de Ciudadano (debe fallar con 403):**
   ```bash
   curl -i -X PATCH http://localhost:8080/api/incidents/1/status -H "Authorization: Bearer <TOKEN_CIUDADANO>"
   ```
   *Respuesta esperada:* Código `HTTP/1.1 403 Forbidden` con mensaje `"Acceso denegado: permisos insuficientes"`.

3. **Petición con token de Operador (debe responder 200 OK):**
   ```bash
   curl -i -X PATCH http://localhost:8080/api/incidents/1/status -H "Authorization: Bearer <TOKEN_OPERADOR>"
   ```
   *Respuesta esperada:* Código `HTTP/1.1 200 OK`.

---

## 6. Veredicto del QA

> **¿La especificación está cumplida?**  
> **SÍ.** Todos los requisitos funcionales, no funcionales, criterios de aceptación y pruebas manuales son satisfactorios. No hay defectos bloqueantes. La feature está aprobada y lista para producción.

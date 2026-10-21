# 002 — Autenticación y Control de Acceso por Roles (RBAC) — Plan

_Cómo se implementa lo descrito en `spec.md`. Respeta la `constitution/`._

## Enfoque

Completar la infraestructura de seguridad perimetral de UrbanPulse mediante el control de acceso basado en roles (RBAC). 
En el backend (Spring Boot), se incorpora un filtro servlet de autenticación JWT (`JwtAuthenticationFilter`) que se antepone a la cadena estándar de Spring Security. Dicho filtro inspecciona la cabecera `Authorization`, valida criptográficamente el token y extrae el sujeto (`sub`) y rol (`role`), inyectando en `SecurityContextHolder` una autenticación con la autoridad correspondiente (`ROLE_<ROL>`). Se implementan manejadores personalizados de excepciones de seguridad (`JwtAuthenticationEntryPoint` para 401 y `CustomAccessDeniedHandler` para 403) para retornar de manera consistente el DTO `ErrorResponse` en formato JSON. Asimismo, se expone un endpoint representativo para operaciones de triaje (`PATCH /api/incidents/{id}/status`), protegido exclusivamente para roles `OPERATOR` y `ADMIN`, verificando que peticiones de usuarios con rol `CITIZEN` sean rechazadas con HTTP 403 Forbidden.

En el frontend (React 19 + React Router), se amplía el esquema de rutas protegidas mediante la creación de un componente `RoleGuard` que se compone con el `ProtectedRoute` existente. Si un usuario no autenticado intenta acceder a una ruta protegida, se redirige a `/login`; si un usuario autenticado carece del rol necesario (ej. `CITIZEN` intentando entrar a `/operator-panel`), se redirige a la vista principal (`/`) con notificación visual mediante `location.state`, preservando en todo momento su sesión y token en `localStorage` sin forzar logout.

---

## Implementación

_Pasos técnicos concretos en orden y módulos afectados:_

### 1. Manejadores de excepciones de seguridad en Backend
- **Archivo:** `Backend/src/main/java/org/bulbaseros/security/JwtAuthenticationEntryPoint.java`
  - Implementa `AuthenticationEntryPoint` de Spring Security.
  - Se activa cuando una petición a un recurso protegido carece de autenticación o presenta un token ausente/inválido (RF-1, RF-2).
  - Serializa y retorna en la respuesta HTTP un objeto `ErrorResponse` (status 401, error "Unauthorized", mensaje descriptivo, timestamp y path) en formato JSON.
- **Archivo:** `Backend/src/main/java/org/bulbaseros/security/CustomAccessDeniedHandler.java`
  - Implementa `AccessDeniedHandler` de Spring Security.
  - Se activa cuando un usuario autenticado intenta acceder a un recurso para el cual no posee la autoridad requerida (RF-5).
  - Serializa y retorna en la respuesta HTTP un objeto `ErrorResponse` (status 403, error "Forbidden", mensaje "Acceso denegado: permisos insuficientes", timestamp y path) en formato JSON.

### 2. Filtro de autenticación JWT en Backend
- **Archivo:** `Backend/src/main/java/org/bulbaseros/security/JwtAuthenticationFilter.java`
  - Extiende `OncePerRequestFilter`.
  - Inyecta `JwtService`.
  - Método `doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)`:
    - Extrae la cabecera `Authorization`.
    - Si la cabecera existe y comienza por `Bearer ` (o variantes insensibles a mayúsculas/espacios), extrae el token.
    - Valida el token con `jwtService.extractEmail(token)` y `jwtService.extractRole(token)`.
    - Si el token es válido y no hay autenticación previa en `SecurityContextHolder`, crea un `UsernamePasswordAuthenticationToken` con la autoridad `SimpleGrantedAuthority("ROLE_" + role.toUpperCase())` y detalles de la petición (`WebAuthenticationDetailsSource`), asignándolo al contexto de seguridad (RF-4).
    - Si el token es inválido o ha expirado y la ruta no es pública, permite que el flujo prosiga hacia el `JwtAuthenticationEntryPoint` o captura la excepción emitiendo directamente el error 401 estructurado (RF-1, RF-2, RF-3).

### 3. Configuración de seguridad y autorización de rutas
- **Archivo:** `Backend/src/main/java/org/bulbaseros/config/SecurityConfig.java`
  - Inyecta `JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint` y `CustomAccessDeniedHandler`.
  - Configura el `exceptionHandling` asociando los handlers de 401 y 403.
  - Configura `authorizeHttpRequests`:
    - Rutas públicas: `/api/auth/**`, `/h2-console/**` con `permitAll()` (RF-3).
    - Rutas de triaje y estado de incidencias: `PATCH /api/incidents/*/status` restringido a autoridades `ROLE_OPERATOR` y `ROLE_ADMIN` (`hasAnyRole("OPERATOR", "ADMIN")`) (RF-5, RF-6).
    - Resto de rutas: `anyRequest().authenticated()`.
  - Añade el filtro `JwtAuthenticationFilter` antes de `UsernamePasswordAuthenticationFilter`.

### 4. Controlador representativo de incidencias para triaje
- **Archivo:** `Backend/src/main/java/org/bulbaseros/dto/UpdateIncidentStatusRequest.java`
  - DTO con validación `@NotNull` para el nuevo estado de la incidencia (`EstadoIncidencia` o cadena de estado).
- **Archivo:** `Backend/src/main/java/org/bulbaseros/dto/IncidentStatusResponse.java`
  - DTO de respuesta que confirma la actualización del estado (`id`, `status`, `message`, `updatedAt`).
- **Archivo:** `Backend/src/main/java/org/bulbaseros/controller/IncidentController.java`
  - Endpoint `PATCH /api/incidents/{id}/status`: recibe el ID de la incidencia y el payload de cambio de estado. Retorna HTTP 200 OK con `IncidentStatusResponse` para operadores y administradores (RF-5, RF-6).

### 5. Servicios y componentes de control de acceso en Frontend
- **Archivo:** `frontend/src/services/authService.js`
  - Función `getUserRole()`: helper que obtiene de forma segura el rol del usuario autenticado a partir de los datos en `localStorage` o claims de sesión.
  - Función `hasAnyRole(allowedRoles)`: helper que comprueba si el rol del usuario activo coincide con alguno de los roles permitidos.
- **Archivo:** `frontend/src/components/RoleGuard.jsx`
  - Componente que recibe `allowedRoles` y `children`.
  - Verifica si el usuario actual posee alguno de los roles permitidos.
  - Si cumple el rol, renderiza los `children` (RF-11).
  - Si no cumple el rol, utiliza `<Navigate to="/" replace state={{ accessDenied: true, message: 'Acceso denegado: no dispones de permisos de operador o administrador' }} />` (RF-8, RF-10).
- **Archivo:** `frontend/src/components/ProtectedRoute.jsx`
  - Actualización modular: permite opcionalmente recibir la propiedad `allowedRoles`.
  - Si el usuario no está autenticado (`!authService.isAuthenticated()`), redirige a `/login` (RF-7).
  - Si está autenticado y se especifican `allowedRoles`, envuelve el renderizado en `RoleGuard` (RF-12).

### 6. Vista del panel de operador y notificación en Home
- **Archivo:** `frontend/src/pages/OperatorPanelPage.jsx`
  - Vista representativa del panel de triaje e incidencias para roles `OPERATOR` y `ADMIN` (RF-11).
- **Archivo:** `frontend/src/pages/HomePage.jsx`
  - Lee el estado de navegación `useLocation().state`.
  - Si contiene `accessDenied: true`, renderiza un banner visual de advertencia con el mensaje informativo sin cerrar la sesión (RF-9, RF-10).
- **Archivo:** `frontend/src/App.jsx`
  - Configura la ruta `/operator-panel` envuelta con `<ProtectedRoute allowedRoles={['OPERATOR', 'ADMIN']}><OperatorPanelPage /></ProtectedRoute>` (RF-6, RF-7, RF-11).

---

## Decisiones

- **Ubicación del Filtro JWT antes de `UsernamePasswordAuthenticationFilter`:**
  - _Justificación:_ En una arquitectura REST sin estado (stateless), la autenticación proviene exclusivamente del encabezado HTTP `Authorization`. Anteponer `JwtAuthenticationFilter` garantiza que el contexto de seguridad se pueble de inmediato antes de que intervengan las reglas de autorización y filtros predeterminados de Spring Security.
  - _Descartado:_ Usar un interceptor de Spring MVC tradicional (`HandlerInterceptor`), ya que no intercepta peticiones rechazadas en la capa previa de filtros de seguridad.
- **Manejadores personalizados `AuthenticationEntryPoint` y `AccessDeniedHandler` frente al comportamiento por defecto:**
  - _Justificación:_ Spring Security por defecto devuelve HTML o cuerpos vacíos ante errores 401 y 403. Implementar estos manejadores garantiza que todas las respuestas de error sigan el estándar JSON definido por `ErrorResponse` en la constitución del proyecto.
  - _Descartado:_ Dejar que Spring Security maneje los errores con su configuración por defecto o capturarlos en un `@ControllerAdvice`, el cual no siempre captura excepciones lanzadas dentro de la cadena de filtros.
- **Composición de `ProtectedRoute` con `RoleGuard`:**
  - _Justificación:_ Permite una separación limpia de responsabilidades (autenticación vs autorización por roles) manteniendo una interfaz intuitiva en el router sin duplicar lógica de redirección.
  - _Descartado:_ Crear componentes completamente independientes e inconexos que obliguen a anidar múltiples tags redundantes en cada ruta.
- **Manejo de HTTP 403 en Frontend sin cierre de sesión:**
  - _Justificación:_ Un error 403 indica que el usuario es quien dice ser, pero no tiene privilegios para una acción concreta. Purgar el token en un 403 arruinaría la experiencia de usuario (cerrando su sesión legítima de ciudadano).
  - _Descartado:_ Limpiar `localStorage` en interceptores de Axios ante códigos 403.

---

## Riesgos

- **Excepciones no controladas en el filtro JWT:**
  - _Riesgo:_ Si un token malformado lanza una excepción dentro de `JwtAuthenticationFilter`, podría provocar un error HTTP 500 no deseado en lugar de un 401.
  - _Mitigación:_ Envolver la lógica de análisis del token en bloques `try-catch` que capturen `JwtException`, `ExpiredJwtException` o `IllegalArgumentException`, delegando de forma segura en `JwtAuthenticationEntryPoint`.
- **Manipulación de datos en cliente (`localStorage`):**
  - _Riesgo:_ Un usuario podría modificar el rol guardado en `localStorage.user` para intentar ver componentes de operador.
  - _Mitigación:_ Aunque el frontend cambie de vista visualmente, el backend verifica rigurosamente la firma criptográfica del JWT en cada petición REST y responde 403 Forbidden si el claim `role` del token firmado no coincide.
- **Rutas públicas bloqueadas por el filtro:**
  - _Riesgo:_ Peticiones a `/api/auth/login` o `/api/auth/register` con tokens residuales o inválidos podrían ser rechazadas indebidamente.
  - _Mitigación:_ Comprobar si la ruta solicitada coincide con rutas públicas permitidas (`shouldNotFilter` o paso transparente con `filterChain.doFilter`).

---

## Estrategia de tests

- **Backend — Tests Unitarios:**
  - `JwtAuthenticationFilterTest`:
    - Petición con cabecera `Authorization` válida inyecta `ROLE_CITIZEN` en `SecurityContextHolder`.
    - Petición con cabecera `Authorization` válida inyecta `ROLE_OPERATOR` en `SecurityContextHolder`.
    - Petición con cabecera `Authorization` ausente o prefijo erróneo no altera el contexto.
    - Petición con token expirado/corrupto delega en el entry point de autenticación.
  - `JwtAuthenticationEntryPointTest`:
    - Verifica que escribe un JSON con estructura `ErrorResponse`, status 401 y cabecera `Content-Type: application/json`.
  - `CustomAccessDeniedHandlerTest`:
    - Verifica que escribe un JSON con estructura `ErrorResponse`, status 403 y cabecera `Content-Type: application/json`.
- **Backend — Tests de Integración (`MockMvc`):**
  - `IncidentControllerSecurityTest`:
    - Invocación anónima (sin token) a `PATCH /api/incidents/1/status` → HTTP 401 Unauthorized (`ErrorResponse` JSON).
    - Invocación con token de usuario con rol `CITIZEN` a `PATCH /api/incidents/1/status` → HTTP 403 Forbidden (`ErrorResponse` JSON).
    - Invocación con token de usuario con rol `OPERATOR` a `PATCH /api/incidents/1/status` → HTTP 200 OK.
    - Invocación con token de usuario con rol `ADMIN` a `PATCH /api/incidents/1/status` → HTTP 200 OK.
  - `SecurityConfigTest`:
    - Invocaciones a `/api/auth/login` y `/api/auth/register` permanecen públicas (HTTP 200/400 según payload, no 401).
- **Frontend — Verificación estática y de integración:**
  - Validación de linting con `npm run lint` (0 errores).
  - Build de producción con `npm run build` (0 errores).
  - Pruebas manuales/automáticas de navegación:
    - Acceso a `/operator-panel` sin sesión redirige a `/login`.
    - Acceso a `/operator-panel` con rol `CITIZEN` redirige a `/` mostrando notificación de acceso denegado y conservando token en `localStorage`.
    - Acceso a `/operator-panel` con rol `OPERATOR` renderiza la vista correctamente.

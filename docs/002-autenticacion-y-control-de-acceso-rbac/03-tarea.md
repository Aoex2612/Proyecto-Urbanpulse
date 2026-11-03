# Tarea 3: Configuración de Seguridad y Reglas de Autorización RBAC en Backend

**Feature:** `002-autenticacion-y-control-de-acceso-rbac`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-3, RF-5, RF-6  

---

## 1. Resumen de la Tarea

En esta tarea se consolidó la arquitectura perimetral y el control de acceso basado en roles (RBAC) en el Backend dentro de la clase central de configuración `SecurityConfig`. Se integraron los componentes implementados en las Tareas 1 y 2 (`JwtAuthenticationEntryPoint`, `CustomAccessDeniedHandler` y `JwtAuthenticationFilter`), garantizando que:

1. El filtro perimetral `JwtAuthenticationFilter` se posicione antes del filtro convencional `UsernamePasswordAuthenticationFilter` en la cadena `SecurityFilterChain`.
2. Las excepciones de seguridad se deleguen de forma controlada a los manejadores personalizados en `exceptionHandling`: emisión de HTTP 401 Unauthorized (`JwtAuthenticationEntryPoint`) ante peticiones no autenticadas o con token inválido/ausente, y HTTP 403 Forbidden (`CustomAccessDeniedHandler`) ante accesos con privilegios insuficientes.
3. Se configuren formalmente las reglas de autorización HTTP con RBAC:
   - Rutas públicas `/api/auth/**` y `/h2-console/**` abiertas sin requerimiento de credenciales (`permitAll()`).
   - Operaciones de triaje y transición de estado de incidencias `PATCH /api/incidents/*/status` reservadas exclusivamente a los roles `OPERATOR` y `ADMIN` (`.hasAnyRole("OPERATOR", "ADMIN")`).
   - El resto de endpoints protegidos mediante requerimiento estricto de autenticación previa (`anyRequest().authenticated()`).
4. Se actualice e incremente la suite de pruebas en `SecurityConfigTest`, incorporando 13 pruebas que validan exhaustivamente tanto la unidad de configuración (beans, constructores, codificador BCrypt, CORS) como las reglas de autorización y manejo de errores mediante simulación HTTP con `MockMvc` y roles mockeados (`@WithMockUser`).

---

## 2. Subtareas implementadas

### 2.1 Registro de dependencias en `SecurityConfig` (Subtarea 3.1)
- **Archivo:** `Backend/src/main/java/org/bulbaseros/config/SecurityConfig.java`
- Se incorporaron los campos inmutables:
  - `private final JwtAuthenticationFilter jwtAuthenticationFilter;`
  - `private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;`
  - `private final CustomAccessDeniedHandler customAccessDeniedHandler;`
- Se dotó a la clase de soporte para inyección de dependencias dual:
  - Constructor por defecto `public SecurityConfig()` para instanciación directa en pruebas unitarias desacopladas con fallbacks seguros.
  - Constructor anotado con `@Autowired` para inyección gestionada por el contenedor Spring Boot (`@Autowired(required = false)` con inicialización predeterminada preventiva).
  - Métodos accesores públicos (`getJwtAuthenticationFilter()`, `getJwtAuthenticationEntryPoint()`, `getCustomAccessDeniedHandler()`) con Javadoc exhaustivo.

### 2.2 Configuración de `exceptionHandling` (Subtarea 3.2)
- En `securityFilterChain(HttpSecurity http)` se asociaron los manejadores de excepción de seguridad:
  ```java
  .exceptionHandling(exceptions -> exceptions
          .authenticationEntryPoint(jwtAuthenticationEntryPoint)
          .accessDeniedHandler(customAccessDeniedHandler)
  )
  ```
- Ante ausencia de token o token corrupto en endpoints protegidos, se canaliza la respuesta a través de `JwtAuthenticationEntryPoint`, produciendo el DTO `ErrorResponse` (HTTP 401).
- Ante peticiones autenticadas pero con rol denegado (ej. `CITIZEN` accediendo a triaje), se canaliza a través de `CustomAccessDeniedHandler`, produciendo el DTO `ErrorResponse` (HTTP 403).

### 2.3 Reglas de autorización y control de acceso RBAC (Subtarea 3.3)
- Se configuró la política de autorización con `authorizeHttpRequests`:
  ```java
  .authorizeHttpRequests(auth -> auth
          .requestMatchers("/api/auth/**", "/h2-console/**").permitAll()
          .requestMatchers(HttpMethod.PATCH, "/api/incidents/*/status").hasAnyRole("OPERATOR", "ADMIN")
          .anyRequest().authenticated()
  )
  ```
- Se respeta la directriz RF-3 garantizando el acceso público y sin token a `/api/auth/**` y `/h2-console/**`.
- Se implementan los requisitos RF-5 y RF-6 garantizando que `PATCH /api/incidents/*/status` admita únicamente a roles `OPERATOR` y `ADMIN`, rechazando a `CITIZEN` con 403 Forbidden.
- Se asegura que cualquier otra ruta privada requiera autenticación previa.

### 2.4 Posicionamiento del filtro JWT en `SecurityFilterChain` (Subtarea 3.4)
- Se añadió la instrucción:
  ```java
  .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
  ```
- Al posicionar `JwtAuthenticationFilter` antes de `UsernamePasswordAuthenticationFilter`, se garantiza que en cada petición HTTP sin estado (stateless) la cabecera `Authorization` sea procesada e inyectada en el `SecurityContextHolder` con las autoridades `ROLE_<ROL>` antes de que intervengan las reglas de autorización de Spring Security.

### 2.5 Actualización y ampliación de pruebas en `SecurityConfigTest` (Subtarea 3.5)
- **Archivo:** `Backend/src/test/java/org/bulbaseros/config/SecurityConfigTest.java`
- Se transformó la suite para combinar pruebas unitarias y de integración mediante `@SpringBootTest` y `@AutoConfigureMockMvc`:
  1. `shouldEncodeAndMatchPasswordsWithBCrypt`: validación de BCryptPasswordEncoder.
  2. `shouldConfigureCorsCorrectly`: validación de orígenes, métodos, cabeceras y credenciales CORS.
  3. `shouldInstantiateWithDefaultDependencies`: validación del constructor sin argumentos.
  4. `shouldInstantiateWithExplicitDependencies`: validación del constructor parametrizado con dependencias explícitas.
  5. `shouldRegisterJwtAuthenticationFilterInFilterChain`: verificación de que `SecurityFilterChain.getFilters()` contiene una instancia de `JwtAuthenticationFilter`.
  6. `shouldIntegrateInjectedSecurityBeans`: comprobación de la inyección de los beans de seguridad desde el contexto de Spring.
  7. `shouldAllowPublicAuthEndpoints`: comprobación de que peticiones a `/api/auth/register` y `/h2-console` son públicas y no emiten 401 ni 403 de seguridad perimetral.
  8. `shouldRejectUnauthenticatedPatchIncidentStatus`: validación de rechazo con HTTP 401 Unauthorized y estructura `ErrorResponse` ante peticiones sin autenticación a `PATCH /api/incidents/100/status`.
  9. `shouldRejectCitizenRoleOnPatchIncidentStatus`: validación de rechazo con HTTP 403 Forbidden y estructura `ErrorResponse` ante peticiones con `@WithMockUser(roles = "CITIZEN")` a `PATCH /api/incidents/100/status`.
  10. `shouldAuthorizeOperatorRoleOnPatchIncidentStatus`: validación de autorización concedida (supera barrera 401/403) para rol `OPERATOR` en `PATCH /api/incidents/100/status`.
  11. `shouldAuthorizeAdminRoleOnPatchIncidentStatus`: validación de autorización concedida (supera barrera 401/403) para rol `ADMIN` en `PATCH /api/incidents/100/status`.
  12. `shouldRejectUnauthenticatedAccessToGeneralProtectedRoutes`: comprobación de que rutas protegidas generales (`/api/incidents`) sin credenciales responden 401 Unauthorized con `ErrorResponse`.
  13. `shouldAllowAuthenticatedAccessToGeneralProtectedRoutes`: comprobación de que rutas protegidas generales con usuario autenticado (`@WithMockUser(roles = "CITIZEN")`) superan la barrera de autenticación.

---

## 3. Decisiones técnicas y arquitectura

- **Mapeo Ant-Style con `HttpMethod.PATCH, "/api/incidents/*/status"`:** Permite capturar de forma genérica cualquier identificador numérico o alfanumérico en el segmento intermedio de la ruta del recurso sin acoplar la configuración de seguridad a la implementación interna de los controladores.
- **Inyección no estricta con fallback defensivo (`@Autowired(required = false)`):** Facilita que pruebas de slice web (como `@WebMvcTest` en `AuthControllerTest`) que importan directamente `SecurityConfig` sin levantar todo el contexto puedan instanciar la configuración de forma segura sin fallos por beans ausentes.
- **Evaluación mediante `@WithMockUser` en `SecurityConfigTest`:** Permite verificar aisladamente las reglas de autorización del `SecurityFilterChain` sin necesidad de generar tokens JWT completos en cada aserción de configuración de ruta.

---

## 4. Validación y Ejecución de Pruebas

### 4.1 Ejecución de las pruebas de la tarea (`SecurityConfigTest`)
```text
[INFO] Running org.bulbaseros.config.SecurityConfigTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.187 s -- in org.bulbaseros.config.SecurityConfigTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 4.2 Ejecución de la suite completa de pruebas en `Backend`
```text
[INFO] Running org.bulbaseros.AppTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.config.SecurityConfigTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.controller.AuthControllerTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.dto.ValidationDtoTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.exception.GlobalExceptionHandlerTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.model.UserTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.repository.UserRepositoryTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.security.CustomAccessDeniedHandlerTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.security.JwtAuthenticationEntryPointTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.security.JwtAuthenticationFilterTest
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.security.JwtServiceTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.service.AuthServiceTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 103, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

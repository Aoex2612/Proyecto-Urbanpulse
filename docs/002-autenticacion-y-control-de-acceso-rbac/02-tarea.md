# Tarea 2: Filtro de Autenticación JWT (`JwtAuthenticationFilter`) en Backend

**Feature:** `002-autenticacion-y-control-de-acceso-rbac`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-1, RF-2, RF-3, RF-4  

---

## 1. Resumen de la Tarea

Se implementó el filtro perimetral de seguridad `JwtAuthenticationFilter` extendiendo `OncePerRequestFilter`, encargado de interceptar cada petición HTTP entrante para extraer, analizar y autenticar credenciales basadas en tokens JWT suministrados en la cabecera `Authorization`.

El filtro garantiza la tolerancia a variaciones en la cabecera (mayúsculas/minúsculas en el esquema `Bearer` y espacios múltiples separadores), la inyección sin estado de objetos `UsernamePasswordAuthenticationToken` con la autoridad canónica `ROLE_<ROL>` en `SecurityContextHolder` para los roles `CITIZEN`, `OPERATOR` y `ADMIN`, el paso transparente sin validación en rutas públicas designadas (`/api/auth/**`, `/h2-console/**`), y el manejo seguro y controlado de excepciones (`ExpiredJwtException`, `JwtException`, `IllegalArgumentException`) delegando en `JwtAuthenticationEntryPoint` sin propagar errores 500 no capturados.

Se incorporó una batería de 22 pruebas unitarias en `JwtAuthenticationFilterTest`, alcanzando una cobertura exhaustiva de todos los escenarios positivos, variantes de formato, delegación de excepciones y exclusión de rutas públicas.

---

## 2. Subtareas implementadas

### 2.1 Implementación de `JwtAuthenticationFilter` (Subtarea 2.1)
- **Ubicación:** `Backend/src/main/java/org/bulbaseros/security/JwtAuthenticationFilter.java`
- Extiende `OncePerRequestFilter` de Spring Web Security y está anotada con `@Component`.
- Proporciona constructores sobrecargados:
  - Constructor con `@Autowired(required = false)` para inyección opcional de `JwtService` y `JwtAuthenticationEntryPoint`, garantizando resiliencia en contextos de prueba desacoplados (como `@WebMvcTest`).
  - Constructor directo `public JwtAuthenticationFilter(JwtService jwtService, JwtAuthenticationEntryPoint authenticationEntryPoint)` para ensamblado explícito.
  - Constructor `public JwtAuthenticationFilter(JwtService jwtService)` con fallback por defecto al entry point.
- Documentación rigurosa con Javadoc en clase, constantes, constructores y métodos.

### 2.2 Extracción tolerante de la cabecera `Authorization` (Subtarea 2.2)
- Implementación de la expresión regular `^(?i)bearer\\s+(.+)$`:
  - Flag case-insensitive `(?i)` para admitir de manera homogénea `Bearer`, `bearer`, `BEARER`, `BeArEr`.
  - Cuantificador `\\s+` que tolera espacios simples, tabulaciones o múltiples espacios consecutivos entre el prefijo y el token (`Bearer        <token>`).
  - Limpieza de espacios en blanco residuales con `.trim()` al inicio y fin de la cabecera.
  - Validación de cadena no vacía para descartar cabeceras con únicamente el prefijo `Bearer`.

### 2.3 Validación e inyección en `SecurityContextHolder` (Subtarea 2.3)
- Validación con `JwtService.extractEmail(token)` y `JwtService.isTokenExpired(token)`.
- Extracción de rol con `JwtService.extractRole(token)`:
  - Normalización de rol a mayúsculas y adición preventiva del prefijo `ROLE_` (`ROLE_CITIZEN`, `ROLE_OPERATOR`, `ROLE_ADMIN`).
  - Si el token no contiene el claim de rol, se asigna una lista vacía de autoridades, provocando la denegación oportuna (HTTP 403) ante endpoints restringidos.
- Construcción e inyección del token `UsernamePasswordAuthenticationToken` con detalles de la petición mediante `WebAuthenticationDetailsSource().buildDetails(request)` en el `SecurityContextHolder` activo solo si no existía autenticación previa.

### 2.4 Manejo controlado de excepciones sin error 500 (Subtarea 2.4)
- Captura especializada de `ExpiredJwtException`:
  - Limpieza del contexto con `SecurityContextHolder.clearContext()`.
  - Asignación del atributo de petición `request.setAttribute("auth_error_message", "Token de autenticación expirado o inválido")`.
  - Invocación de `authenticationEntryPoint.commence(request, response, ...)` para emitir la respuesta JSON HTTP 401 estructurada.
  - Interrupción inmediata del flujo con `return;` evitando invocar `filterChain.doFilter(...)`.
- Captura de `JwtException` y `IllegalArgumentException` (firmas corruptas, tokens malformados):
  - Idéntico mecanismo seguro de delegación a `JwtAuthenticationEntryPoint`, asegurando la no propagación de errores no controlados (500).

### 2.5 Exclusión y paso transparente en rutas públicas (Subtarea 2.5)
- Sobrescritura de `shouldNotFilter(HttpServletRequest request)`:
  - Detecta rutas bajo `/api/auth/**` (login, register) y `/h2-console/**`.
  - Verifica tanto `request.getRequestURI()` como `request.getServletPath()`.
- Verificación complementaria al inicio de `doFilterInternal`:
  - Permite la ejecución directa de la cadena (`filterChain.doFilter`) sin procesar cabeceras ni lanzar excepciones de validación si la ruta es pública o si el servicio JWT no fue aprovisionado.

### 2.6 Pruebas unitarias exhaustivas en `JwtAuthenticationFilterTest` (Subtarea 2.6)
- **Ubicación:** `Backend/src/test/java/org/bulbaseros/security/JwtAuthenticationFilterTest.java`
- Batería de 22 pruebas unitarias:
  - `shouldAuthenticateValidTokenWithCitizenRole`: valida inyección de `ROLE_CITIZEN`.
  - `shouldAuthenticateValidTokenWithOperatorRole`: valida inyección de `ROLE_OPERATOR`.
  - `shouldAuthenticateValidTokenWithAdminRole`: valida inyección de `ROLE_ADMIN`.
  - `shouldSupportLowercaseBearerPrefix`: tolerancia a `bearer`.
  - `shouldSupportUppercaseBearerPrefix`: tolerancia a `BEARER`.
  - `shouldSupportMixedCaseBearerPrefix`: tolerancia a `BeArEr`.
  - `shouldSupportMultipleSpacesBetweenBearerAndToken`: tolerancia a espacios múltiples.
  - `shouldSupportLeadingAndTrailingSpacesInHeader`: tolerancia a padding de espacios.
  - `shouldHandleMissingAuthorizationHeaderGracefully`: cabecera ausente no altera contexto.
  - `shouldHandleBlankAuthorizationHeader`: cabecera en blanco no altera contexto.
  - `shouldHandleInvalidHeaderPrefixWithoutBearer`: prefijo no Bearer (`Basic`) no altera contexto.
  - `shouldHandleBearerHeaderWithoutToken`: cabecera con solo prefijo Bearer no altera contexto.
  - `shouldHandleExpiredTokenAndDelegateToEntryPoint`: token expirado emite HTTP 401 con `"Token de autenticación expirado o inválido"`.
  - `shouldHandleMalformedTokenAndDelegateToEntryPoint`: token malformado emite HTTP 401.
  - `shouldHandleJwtExceptionSafelyWithoutThrowing500`: excepciones de parseo emiten 401 sin 500.
  - `shouldSkipFilterOnPublicAuthLoginEndpointEvenWithInvalidToken`: `/api/auth/login` permite paso transparente con token inválido.
  - `shouldSkipFilterOnPublicAuthRegisterEndpointWithExpiredToken`: `/api/auth/register` permite paso transparente con token expirado.
  - `shouldSkipFilterOnH2ConsoleEndpoint`: rutas de consola H2 se omiten.
  - `shouldHandleTokenWithNullOrEmptyRoleGracefully`: token sin rol no inyecta autoridades.
  - `shouldNotOverwriteExistingAuthentication`: respeta autenticación previa en el contexto.
  - `shouldEvaluateShouldNotFilterAccurately`: validación exhaustiva de patrones de ruta en `shouldNotFilter`.
  - `shouldSupportSingleParameterConstructor`: soporte para constructor unario.

---

## 3. Archivos creados / modificados

| Archivo | Tipo de cambio | Descripción |
|---|---|---|
| `Backend/src/main/java/org/bulbaseros/security/JwtAuthenticationFilter.java` | Creado | Filtro servlet perimetral para autenticación JWT con Javadoc exhaustivo |
| `Backend/src/test/java/org/bulbaseros/security/JwtAuthenticationFilterTest.java` | Creado | Suite de 22 pruebas unitarias para `JwtAuthenticationFilter` |
| `features/002-autenticacion-y-control-de-acceso-rbac/tasks.md` | Modificado | Tarea 2 y subtareas 2.1 a 2.6 marcadas como completadas `[x]` |
| `docs/002-autenticacion-y-control-de-acceso-rbac/02-tarea.md` | Creado | Documentación técnica detallada de la Tarea 2 |

---

## 4. Resultado de las Pruebas

Ejecución de las pruebas de la tarea (`JwtAuthenticationFilterTest`):
```
[INFO] Running org.bulbaseros.security.JwtAuthenticationFilterTest
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.913 s -- in org.bulbaseros.security.JwtAuthenticationFilterTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

Ejecución de la suite completa de pruebas en `Backend`:
```
[INFO] Running org.bulbaseros.config.SecurityConfigTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.controller.AuthControllerTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
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
[INFO] Running org.bulbaseros.AppTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 92, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

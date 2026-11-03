# Tarea 1: Manejadores de Excepciones de Seguridad (401 y 403) en Backend

**Feature:** `002-autenticacion-y-control-de-acceso-rbac`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-1, RF-2, RF-5  

---

## 1. Resumen de la Tarea

Se implementaron los manejadores de excepciones de seguridad a nivel de servlet para capturar de forma centralizada y estandarizada los fallos de autenticación (HTTP 401 Unauthorized) y denegación de acceso por permisos insuficientes (HTTP 403 Forbidden). 

Ambos manejadores (`JwtAuthenticationEntryPoint` y `CustomAccessDeniedHandler`) serializan la respuesta empleando el DTO oficial `ErrorResponse`, garantizando el formato JSON canónico del proyecto (`timestamp`, `status`, `error`, `message`, `path`). Se configuró además `ObjectMapper` con el módulo `JavaTimeModule` deshabilitando `WRITE_DATES_AS_TIMESTAMPS` para asegurar que las fechas y marcas de tiempo (`OffsetDateTime`) se emitan de forma legible en estándar ISO-8601. Asimismo, se incorporó una suite completa de pruebas unitarias cubriendo ambos componentes con cobertura exhaustiva.

---

## 2. Subtareas implementadas

### 2.1 Implementación de `JwtAuthenticationEntryPoint` (Subtarea 1.1)
- **Ubicación:** `Backend/src/main/java/org/bulbaseros/security/JwtAuthenticationEntryPoint.java`
- Implementa `AuthenticationEntryPoint` de Spring Security con la anotación `@Component`.
- En el método `commence`:
  - Establece el código de estado `HttpServletResponse.SC_UNAUTHORIZED` (401).
  - Configura las cabeceras `Content-Type: application/json` y codificación `UTF-8`.
  - Construye la respuesta utilizando el DTO `ErrorResponse` con `status: 401`, `error: "Unauthorized"`, `path: request.getRequestURI()` y marca de tiempo actual.
  - Mensaje por defecto: `"Token de autenticación ausente o inválido"`, permitiendo mensajes personalizados provenientes de atributos de petición o excepciones especializadas (ej. `"Token de autenticación expirado o inválido"` según RF-2).
  - Documentación rigurosa con Javadoc en clase, atributos y métodos.

### 2.2 Implementación de `CustomAccessDeniedHandler` (Subtarea 1.2)
- **Ubicación:** `Backend/src/main/java/org/bulbaseros/security/CustomAccessDeniedHandler.java`
- Implementa `AccessDeniedHandler` de Spring Security con la anotación `@Component`.
- En el método `handle`:
  - Establece el código de estado `HttpServletResponse.SC_FORBIDDEN` (403).
  - Configura las cabeceras `Content-Type: application/json` y codificación `UTF-8`.
  - Construye la respuesta utilizando el DTO `ErrorResponse` con `status: 403`, `error: "Forbidden"`, `message: "Acceso denegado: permisos insuficientes"`, `path: request.getRequestURI()` y marca de tiempo actual.
  - Permite parametrización o sobrescritura de mensaje mediante atributos del request en caso de requerirse mayor especificidad.
  - Documentación rigurosa con Javadoc en clase, atributos y métodos.

### 2.3 Configuración de `ObjectMapper` con `JavaTimeModule` (Subtarea 1.3)
- Tanto `JwtAuthenticationEntryPoint` como `CustomAccessDeniedHandler` configuran su instancia de `ObjectMapper`:
  - Registro del módulo `JavaTimeModule` de Jackson.
  - Desactivación de `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS` para serializar `OffsetDateTime` en formato cadena ISO-8601.
  - Soporte de inyección por constructor y fallback a nueva instancia preconfigurada para facilitar testing y desacoplamiento.

### 2.4 Pruebas Unitarias Exhaustivas (Subtarea 1.4)
- **`JwtAuthenticationEntryPointTest`:**
  - `shouldReturn401WithStandardErrorResponseWhenCommenceIsInvoked`: Valida código 401, cabecera `Content-Type: application/json`, codificación UTF-8 y estructura completa de `ErrorResponse`.
  - `shouldIncludeCustomMessageFromRequestAttribute`: Valida el soporte de mensajes dinámicos vía atributo de request.
  - `shouldIncludeMessageWhenExceptionHasExpiredTokenMessage`: Valida mensaje específico ante token expirado.
  - `shouldSerializeOffsetDateTimeAsIsoString`: Comprueba que `timestamp` sea textual en formato ISO-8601.
  - `shouldSupportParameterizedConstructorAndNullObjectMapper`: Valida robustez ante constructor con parámetro nulo.
  - `shouldSupportPreconfiguredObjectMapper`: Valida interoperabilidad con instancias preexistentes de `ObjectMapper`.
- **`CustomAccessDeniedHandlerTest`:**
  - `shouldReturn403WithStandardErrorResponseWhenHandleIsInvoked`: Valida código 403, cabecera `Content-Type: application/json`, codificación UTF-8 y estructura canónica de `ErrorResponse`.
  - `shouldIncludeCustomMessageFromRequestAttribute`: Valida mensaje personalizado mediante atributos del request.
  - `shouldSerializeOffsetDateTimeAsIsoString`: Comprueba que `timestamp` sea textual en formato ISO-8601.
  - `shouldSupportParameterizedConstructorAndNullObjectMapper`: Valida manejo seguro con mapper nulo.
  - `shouldSupportPreconfiguredObjectMapper`: Valida constructor parametrizado con `ObjectMapper` personalizado.

---

## 3. Archivos creados / modificados

| Archivo | Tipo de cambio | Descripción |
|---|---|---|
| `Backend/src/main/java/org/bulbaseros/security/JwtAuthenticationEntryPoint.java` | Creado | Manejador de fallos de autenticación 401 con serialización `ErrorResponse` y Javadoc |
| `Backend/src/main/java/org/bulbaseros/security/CustomAccessDeniedHandler.java` | Creado | Manejador de denegación de permisos 403 con serialización `ErrorResponse` y Javadoc |
| `Backend/src/test/java/org/bulbaseros/security/JwtAuthenticationEntryPointTest.java` | Creado | Pruebas unitarias para `JwtAuthenticationEntryPoint` (6 tests) |
| `Backend/src/test/java/org/bulbaseros/security/CustomAccessDeniedHandlerTest.java` | Creado | Pruebas unitarias para `CustomAccessDeniedHandler` (5 tests) |
| `features/002-autenticacion-y-control-de-acceso-rbac/tasks.md` | Modificado | Marcada la Tarea 1 y subtareas como completadas `[x]` |
| `docs/002-autenticacion-y-control-de-acceso-rbac/01-tarea.md` | Creado | Documentación detallada de la Tarea 1 |

---

## 4. Resultado de las Pruebas

Ejecución de los tests específicos de la tarea:
```
[INFO] Running org.bulbaseros.security.CustomAccessDeniedHandlerTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.196 s -- in org.bulbaseros.security.CustomAccessDeniedHandlerTest
[INFO] Running org.bulbaseros.security.JwtAuthenticationEntryPointTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.023 s -- in org.bulbaseros.security.JwtAuthenticationEntryPointTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

Ejecución de la suite completa de pruebas en `Backend`:
```
[INFO] Results:
[INFO] 
[INFO] Tests run: 70, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

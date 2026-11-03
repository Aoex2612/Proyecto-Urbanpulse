# Tarea 4: DTOs, Validaciones y Manejador Global de Excepciones

**Feature:** `001-registro-e-inicio-de-sesion-de-ciudadanos`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-1, RF-2, RF-3, RF-7  

---

## 1. Resumen de la Tarea

Se ha implementado la capa de contratos de datos (DTOs) de entrada y salida para los endpoints de autenticación (`/register` y `/login`), junto con las reglas de validación declarativa (Bean Validation) y el mecanismo centralizado de gestión y mapeo de excepciones (`GlobalExceptionHandler`).

La solución garantiza respuestas de error uniformes en toda la API mediante el objeto `ErrorResponse`, asegurando los códigos de estado HTTP requeridos (400, 401, 409, 500) y protegiendo la seguridad del sistema al emitir mensajes genéricos ante credenciales inválidas para evitar la enumeración de usuarios.

---

## 2. Subtareas implementadas

### 2.1 DTOs de Registro (`Backend/src/main/java/org/bulbaseros/dto/`)
- **`RegisterRequest.java`:**
  - `fullName`: Obligatorio (`@NotBlank`) y acotado a 150 caracteres (`@Size(max = 150)`).
  - `email`: Obligatorio (`@NotBlank`), formato de correo válido (`@Email`) y longitud máxima de 150 caracteres (`@Size(max = 150)`).
  - `password`: Obligatoria (`@NotBlank`) y sujeta a patrón regex (`@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$")`) para exigir mínimo 8 caracteres con al menos una letra y un dígito.
  - Constructores y métodos accesores documentados con Javadoc.
- **`RegisterResponse.java`:**
  - `message`: Confirmación descriptiva ("Usuario registrado exitosamente").
  - `email`: Correo electrónico del usuario creado.

### 2.2 DTOs de Inicio de Sesión y Respuestas de Autenticación
- **`LoginRequest.java`:**
  - `email`: Obligatorio (`@NotBlank`) y con formato de correo electrónico (`@Email`).
  - `password`: Obligatoria (`@NotBlank`).
- **`AuthResponse.java`:**
  - `token`: Token JWT emitido y firmado.
  - `tokenType`: Esquema de autorización por defecto "Bearer".
  - `expiresIn`: Duración en segundos (`86400L` correspondiente a 24 horas según RFC 6749).
  - `role`: Rol funcional del usuario (ej. "CITIZEN").
  - `fullName`: Nombre completo para despliegue visual en interfaz de usuario.
- **`ErrorResponse.java`:**
  - Estructura uniforme para respuestas de error de la API:
    - `timestamp`: Marca temporal del error (`OffsetDateTime`).
    - `status`: Código numérico HTTP (ej. 400, 401, 409, 500).
    - `error`: Razón canónica HTTP (ej. "Bad Request", "Conflict", "Unauthorized").
    - `message`: Mensaje explicativo amigable para el cliente.
    - `path`: Endpoint URI donde se produjo la excepción.
    - `errors`: Mapa opcional de pares campo-mensaje para fallos de validación.

### 2.3 Excepciones de Negocio (`Backend/src/main/java/org/bulbaseros/exception/`)
- **`EmailAlreadyExistsException.java`:** Excepción de conflicto que se dispara cuando un correo electrónico ya está en uso en el registro de usuarios.
- **`InvalidCredentialsException.java`:** Excepción de autenticación con mensaje genérico ("Credenciales inválidas") disparada ante discrepancia en credenciales de inicio de sesión.

### 2.4 Manejador Global de Excepciones (`Backend/src/main/java/org/bulbaseros/exception/GlobalExceptionHandler.java`)
- Anotado con `@RestControllerAdvice` para interceptar excepciones en todos los controladores REST:
  - `MethodArgumentNotValidException` -> **HTTP 400 Bad Request**, extrayendo errores por campo en `errors`.
  - `EmailAlreadyExistsException` -> **HTTP 409 Conflict**, retornando el mensaje del conflicto.
  - `InvalidCredentialsException` y `BadCredentialsException` -> **HTTP 401 Unauthorized**, con mensaje homogéneo "Credenciales inválidas".
  - `Exception` (fallback general) -> **HTTP 500 Internal Server Error**, con mensaje neutral "Ha ocurrido un error interno en el servidor".

### 2.5 Pruebas Unitarias y Verificación
- Creado `Backend/src/test/java/org/bulbaseros/dto/ValidationDtoTest.java` (11 pruebas) validando:
  - Validaciones exitosas de `RegisterRequest` y `LoginRequest`.
  - Rechazo de nombres vacíos o mayores a 150 caracteres.
  - Rechazo de correos sin formato válido.
  - Rechazo de contraseñas de menos de 8 caracteres, sin dígitos o sin letras.
  - Verificación de obligatoriedad en campos de login.
  - Integridad estructural y accesores de `RegisterResponse`, `AuthResponse` y `ErrorResponse`.
- Creado `Backend/src/test/java/org/bulbaseros/exception/GlobalExceptionHandlerTest.java` (5 pruebas) validando:
  - Respuesta HTTP 400 y mapa de errores ante `MethodArgumentNotValidException`.
  - Respuesta HTTP 409 ante `EmailAlreadyExistsException`.
  - Respuesta HTTP 401 ante `InvalidCredentialsException`.
  - Respuesta HTTP 401 ante `BadCredentialsException`.
  - Respuesta HTTP 500 ante excepciones no controladas.
- Verificación limpia mediante `cd Backend && mvn clean test`: 42 pruebas totales ejecutadas, 0 fallos, 0 errores.

---

## 3. Archivos creados / modificados

| Archivo | Tipo de cambio | Descripción |
|---|---|---|
| `Backend/src/main/java/org/bulbaseros/dto/RegisterRequest.java` | Creado | DTO de solicitud de registro con Bean Validation y regex de contraseña |
| `Backend/src/main/java/org/bulbaseros/dto/RegisterResponse.java` | Creado | DTO de respuesta de registro con mensaje descriptivo y correo |
| `Backend/src/main/java/org/bulbaseros/dto/LoginRequest.java` | Creado | DTO de solicitud de login con validación de email y password |
| `Backend/src/main/java/org/bulbaseros/dto/AuthResponse.java` | Creado | DTO de respuesta de login con token JWT, tipo Bearer, expiración y datos de usuario |
| `Backend/src/main/java/org/bulbaseros/dto/ErrorResponse.java` | Creado | DTO estándar para respuestas de error de la API REST |
| `Backend/src/main/java/org/bulbaseros/exception/EmailAlreadyExistsException.java` | Creado | Excepción de conflicto por email duplicado (HTTP 409) |
| `Backend/src/main/java/org/bulbaseros/exception/InvalidCredentialsException.java` | Creado | Excepción por credenciales erróneas (HTTP 401) |
| `Backend/src/main/java/org/bulbaseros/exception/GlobalExceptionHandler.java` | Creado | Manejador global REST advice para mapeo a códigos HTTP 400, 401, 409 y 500 |
| `Backend/src/test/java/org/bulbaseros/dto/ValidationDtoTest.java` | Creado | Pruebas unitarias de validación y DTOs (11 pruebas) |
| `Backend/src/test/java/org/bulbaseros/exception/GlobalExceptionHandlerTest.java` | Creado | Pruebas unitarias del manejador global de excepciones (5 pruebas) |
| `features/001-registro-e-inicio-de-sesion-de-ciudadanos/tasks.md` | Modificado | Marcado de Tarea 4 y subtareas como completadas `[x]` |
| `docs/001-registro-e-inicio-de-sesion-de-ciudadanos/04-tarea.md` | Creado | Documentación detallada de la Tarea 4 |

---

## 4. Resultado de las Pruebas

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.bulbaseros.AppTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.025 s -- in org.bulbaseros.AppTest
[INFO] Running org.bulbaseros.config.SecurityConfigTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.186 s -- in org.bulbaseros.config.SecurityConfigTest
[INFO] Running org.bulbaseros.dto.ValidationDtoTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.186 s -- in org.bulbaseros.dto.ValidationDtoTest
[INFO] Running org.bulbaseros.exception.GlobalExceptionHandlerTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in org.bulbaseros.exception.GlobalExceptionHandlerTest
[INFO] Running org.bulbaseros.model.UserTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.013 s -- in org.bulbaseros.model.UserTest
[INFO] Running org.bulbaseros.repository.UserRepositoryTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.269 s -- in org.bulbaseros.repository.UserRepositoryTest
[INFO] Running org.bulbaseros.security.JwtServiceTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.132 s -- in org.bulbaseros.security.JwtServiceTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 42, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  5.450 s
[INFO] Finished at: 2026-10-20T21:06:10+02:00
[INFO] ------------------------------------------------------------------------
```

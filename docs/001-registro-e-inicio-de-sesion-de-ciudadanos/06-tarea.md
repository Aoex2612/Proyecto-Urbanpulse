# Tarea 6: Pruebas Unitarias y de Integración Backend

**Feature:** `001-registro-e-inicio-de-sesion-de-ciudadanos`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-1, RF-2, RF-3, RF-6, RF-7  

---

## 1. Resumen de la Tarea

Se ha consolidado y verificado la suite completa de pruebas automatizadas en el backend de UrbanPulse, abarcando pruebas unitarias de servicios de negocio (`AuthService`), pruebas de contratos y serialización web mediante `@WebMvcTest` (`AuthController`), pruebas de seguridad criptográfica y gestión de tokens (`JwtService`), pruebas de validaciones declarativas (`ValidationDtoTest`), pruebas del manejador global de excepciones (`GlobalExceptionHandlerTest`), y pruebas de persistencia JPA en base de datos en memoria (`UserRepositoryTest`).

Se añadieron pruebas específicas para validar el rechazo de peticiones con `fullName` o `email` superiores a 150 caracteres (HTTP 400 Bad Request) y la verificación explícita de los claims estándar (`sub` y `role`) en el payload de los tokens JWT emitidos.

---

## 2. Detalle de la Suite de Pruebas y Cobertura de Requisitos Funcionales

### 2.1 Pruebas Unitarias de `AuthService` (`Backend/src/test/java/org/bulbaseros/service/AuthServiceTest.java`) — RF-1, RF-2, RF-6, RF-7
- **Registro exitoso:** Verifica que ante un `RegisterRequest` válido se compruebe la unicidad del correo, se aplique el hash seguro mediante `PasswordEncoder` (BCrypt), se asigne el rol `UserRole.CITIZEN` y se persista en base de datos devolviendo `RegisterResponse` con HTTP 201 (RF-1, RF-2).
- **Normalización de correo:** Valida que los correos sean normalizados a minúsculas y sin espacios iniciales/finales tanto en el registro como en el login.
- **Conflicto por email duplicado:** Verifica que si `existsByEmail` es verdadero, se lance `EmailAlreadyExistsException` y nunca se guarde el usuario duplicado (RF-2).
- **Inicio de sesión exitoso:** Valida la búsqueda por email, la verificación del hash de contraseña con `passwordEncoder.matches`, la generación del token JWT con expiración a 24 horas y el retorno del `AuthResponse` con `expiresIn: 86400` y rol "CITIZEN" (RF-6, RF-7).
- **Fallo por usuario inexistente:** Lanza `InvalidCredentialsException` sin filtrar información interna ni invocar el generador de tokens (RF-7).
- **Fallo por contraseña errónea:** Lanza `InvalidCredentialsException` ante no coincidencia del hash de contraseña (RF-7).
- **Parámetros nulos:** Verifica el lanzamiento de `IllegalArgumentException` ante payloads nulos.

### 2.2 Pruebas de Controlador WebMvc (`Backend/src/test/java/org/bulbaseros/controller/AuthControllerTest.java`) — RF-1, RF-2, RF-3, RF-7
- **`POST /api/auth/register` (HTTP 201):** Valida la serialización JSON de entrada y la respuesta de creación con mensaje y email.
- **`POST /api/auth/register` (HTTP 409):** Valida la captura de `EmailAlreadyExistsException` devolviendo 409 Conflict.
- **`POST /api/auth/register` (HTTP 400 - Campos vacíos o inválidos):** Valida que payloads con nombre vacío, email malformado o clave menor a 8 caracteres retornen HTTP 400 con mapa de errores por campo (RF-3).
- **`POST /api/auth/register` (HTTP 400 - Longitud de `fullName` > 150):** Test específico que verifica el rechazo con HTTP 400 cuando el nombre completo excede los 150 caracteres.
- **`POST /api/auth/register` (HTTP 400 - Longitud de `email` > 150):** Test específico que verifica el rechazo con HTTP 400 cuando el correo excede los 150 caracteres.
- **`POST /api/auth/login` (HTTP 200):** Valida el retorno exitoso de `token`, `tokenType: "Bearer"`, `expiresIn: 86400`, `role` y `fullName`.
- **`POST /api/auth/login` (HTTP 401):** Valida que credenciales inválidas retornen 401 Unauthorized con mensaje genérico ("Credenciales inválidas").
- **`POST /api/auth/login` (HTTP 400 - Campos en blanco):** Valida que si se envían credenciales vacías se retorne HTTP 400 con los errores correspondientes.

### 2.3 Pruebas de Servicio de Tokens JWT (`Backend/src/test/java/org/bulbaseros/security/JwtServiceTest.java`) — RF-6, RF-9
- **Generación y extracción de claims:** Valida la inclusión de `sub` (email) y `role` ("CITIZEN") en el payload JWT y su extracción fiel.
- **Verificación de firma:** Valida que tokens firmados con claves secretas distintas o con firmas adulteradas sean rechazados con excepción criptográfica `JwtException`.
- **Expiración a 24 horas:** Verifica que la fecha de caducidad calculada coincida con `86.400.000` ms (con margen de ejecución < 5 segundos) y que tokens recién generados no figuren como expirados.
- **Detección de expiración:** Valida que tokens con tiempo de expiración transcurrido sean reportados de forma inmediata como expirados e inválidos.
- **Robustez ante malformaciones:** Detección de tokens con estructura corrupta o argumentos nulos.

---

## 3. Resumen de Archivos de Pruebas

| Archivo de Prueba | Tipo | Pruebas | Propósito |
|---|---|---|---|
| `Backend/src/test/java/org/bulbaseros/service/AuthServiceTest.java` | Unitaria (Mockito) | 8 | Lógica de registro, login, encriptación BCrypt y validación de unicidad |
| `Backend/src/test/java/org/bulbaseros/controller/AuthControllerTest.java` | Integración (`@WebMvcTest`) | 8 | Contratos HTTP REST (201, 200, 400 con límites de 150 caracteres, 401, 409) |
| `Backend/src/test/java/org/bulbaseros/security/JwtServiceTest.java` | Unitaria Criptográfica | 9 | HMAC-SHA256, expiración 24h, claims `sub` y `role`, validación de firmas |
| `Backend/src/test/java/org/bulbaseros/dto/ValidationDtoTest.java` | Bean Validation | 11 | Restricciones de validación en DTOs y complejidad de contraseñas |
| `Backend/src/test/java/org/bulbaseros/exception/GlobalExceptionHandlerTest.java` | Unitaria de Excepciones | 5 | Mapeo y serialización de `ErrorResponse` en 400, 401, 409 y 500 |
| `Backend/src/test/java/org/bulbaseros/model/UserTest.java` | Unitaria de Dominio | 10 | Entidad `User`, defaults `CITIZEN`, exclusión de password en `toString()` |
| `Backend/src/test/java/org/bulbaseros/repository/UserRepositoryTest.java` | Persistencia (`@DataJpaTest`) | 5 | Operaciones JPA contra H2, `existsByEmail` y `findByEmail` |
| `Backend/src/test/java/org/bulbaseros/config/SecurityConfigTest.java` | Unitaria de Configuración | 2 | Bean `BCryptPasswordEncoder` y configuración de reglas CORS |
| `Backend/src/test/java/org/bulbaseros/AppTest.java` | Contexto / Carga | 1 | Instanciación básica de la aplicación |

**Total pruebas backend:** 59 pruebas ejecutadas con éxito.

---

## 4. Resultado de las Pruebas

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.bulbaseros.AppTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.056 s -- in org.bulbaseros.AppTest
[INFO] Running org.bulbaseros.config.SecurityConfigTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.357 s -- in org.bulbaseros.config.SecurityConfigTest
[INFO] Running org.bulbaseros.controller.AuthControllerTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.816 s -- in org.bulbaseros.controller.AuthControllerTest
[INFO] Running org.bulbaseros.dto.ValidationDtoTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.176 s -- in org.bulbaseros.dto.ValidationDtoTest
[INFO] Running org.bulbaseros.exception.GlobalExceptionHandlerTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.018 s -- in org.bulbaseros.exception.GlobalExceptionHandlerTest
[INFO] Running org.bulbaseros.model.UserTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.014 s -- in org.bulbaseros.model.UserTest
[INFO] Running org.bulbaseros.repository.UserRepositoryTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.968 s -- in org.bulbaseros.repository.UserRepositoryTest
[INFO] Running org.bulbaseros.security.JwtServiceTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.089 s -- in org.bulbaseros.security.JwtServiceTest
[INFO] Running org.bulbaseros.service.AuthServiceTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.175 s -- in org.bulbaseros.service.AuthServiceTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 59, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  10.898 s
[INFO] Finished at: 2026-10-20T21:13:04+02:00
[INFO] ------------------------------------------------------------------------
```

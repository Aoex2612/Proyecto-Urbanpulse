# Tarea 5: Lógica de Autenticación y Controladores REST

**Feature:** `001-registro-e-inicio-de-sesion-de-ciudadanos`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-1, RF-2, RF-6, RF-7  

---

## 1. Resumen de la Tarea

Se ha implementado la capa de servicios de negocio (`AuthService`) y la capa de controladores REST (`AuthController`) para el flujo integral de registro de ciudadanos e inicio de sesión en UrbanPulse. La solución valida y normaliza las entradas de usuario, garantiza la unicidad de las direcciones de correo electrónico, hashea las contraseñas de manera segura mediante BCrypt, asigna el rol predeterminado `CITIZEN`, y emite tokens JWT firmados con algoritmo HMAC-SHA256 con validez de 24 horas (86.400 segundos). Toda la API expone respuestas JSON estructuradas con códigos HTTP canónicos (201 Created para registro, 200 OK para login, 400 Bad Request ante validaciones fallidas, 401 Unauthorized ante credenciales erróneas y 409 Conflict ante duplicidad de correo).

---

## 2. Subtareas implementadas

### 2.1 Servicio de Autenticación (`Backend/src/main/java/org/bulbaseros/service/AuthService.java`)
- Declarado como `@Service` de Spring e inyectado con `UserRepository`, `PasswordEncoder` y `JwtService` mediante constructor.
- **Método `register(RegisterRequest request)`**:
  - Valida que la solicitud no sea nula.
  - Normaliza el correo electrónico (eliminando espacios periféricos y convirtiendo a minúsculas).
  - Consulta `userRepository.existsByEmail(...)` y, en caso de coincidencia, lanza `EmailAlreadyExistsException("El correo electrónico ya está registrado.")`.
  - Cifra la contraseña con BCrypt mediante `passwordEncoder.encode(...)`.
  - Instancia la entidad de dominio `User` asignando el rol `UserRole.CITIZEN`.
  - Persiste el usuario mediante `userRepository.save(...)` bajo una transacción `@Transactional`.
  - Retorna `RegisterResponse("Usuario registrado exitosamente", savedUser.getEmail())`.
- **Método `login(LoginRequest request)`**:
  - Valida que la solicitud no sea nula.
  - Normaliza el correo electrónico.
  - Busca el usuario registrado mediante `userRepository.findByEmail(...)`. Si no se encuentra, lanza `InvalidCredentialsException("Credenciales inválidas.")`.
  - Comprueba la contraseña en texto plano contra el hash BCrypt con `passwordEncoder.matches(...)`. Si no coincide, lanza `InvalidCredentialsException("Credenciales inválidas.")`.
  - Genera el token JWT mediante `jwtService.generateToken(user)`.
  - Retorna `AuthResponse` conteniendo el token, esquema `Bearer`, tiempo de expiración (86.400 segundos / 24 horas), rol (`CITIZEN`) y nombre completo del usuario.
- Javadoc exhaustivo en clase, constructor y métodos (`@param`, `@return`, `@throws`).

### 2.2 Controlador REST (`Backend/src/main/java/org/bulbaseros/controller/AuthController.java`)
- Anotado con `@RestController` y mapeado en la ruta base `@RequestMapping("/api/auth")`.
- **Endpoint `POST /api/auth/register`**:
  - Recibe `@Valid @RequestBody RegisterRequest request`.
  - Invoca `authService.register(request)`.
  - Responde con estado HTTP 201 Created y payload `RegisterResponse`.
- **Endpoint `POST /api/auth/login`**:
  - Recibe `@Valid @RequestBody LoginRequest request`.
  - Invoca `authService.login(request)`.
  - Responde con estado HTTP 200 OK y payload `AuthResponse`.
- Javadoc riguroso detallando comportamiento, parámetros y códigos de retorno.

### 2.3 Pruebas Unitarias y de Integración Web
- **`AuthServiceTest` (`Backend/src/test/java/org/bulbaseros/service/AuthServiceTest.java`)**:
  - Pruebas aisladas con Mockito (`@ExtendWith(MockitoExtension.class)`).
  - 8 casos de prueba cubriendo:
    - Registro exitoso con captura de entidad y comprobación de rol `CITIZEN`.
    - Normalización de correo en minúsculas y sin espacios al registrar.
    - Lanzamiento de `EmailAlreadyExistsException` ante email duplicado.
    - Autenticación exitosa y generación de `AuthResponse`.
    - Normalización de correo al iniciar sesión.
    - Lanzamiento de `InvalidCredentialsException` ante usuario inexistente.
    - Lanzamiento de `InvalidCredentialsException` ante contraseña incorrecta.
    - Rechazo de solicitudes nulas con `IllegalArgumentException`.
- **`AuthControllerTest` (`Backend/src/test/java/org/bulbaseros/controller/AuthControllerTest.java`)**:
  - Pruebas de integración con `@WebMvcTest(AuthController.class)` y `MockMvc`.
  - 5 casos de prueba verificando códigos de estado HTTP y estructura JSON:
    - `POST /api/auth/register` con payload válido -> HTTP 201 Created.
    - `POST /api/auth/register` con email duplicado -> HTTP 409 Conflict.
    - `POST /api/auth/register` con campos inválidos -> HTTP 400 Bad Request.
    - `POST /api/auth/login` con credenciales válidas -> HTTP 200 OK.
    - `POST /api/auth/login` con credenciales erróneas -> HTTP 401 Unauthorized.

---

## 3. Archivos creados / modificados

| Archivo | Tipo de cambio | Descripción |
|---|---|---|
| `Backend/src/main/java/org/bulbaseros/service/AuthService.java` | Creado | Lógica de negocio de registro con BCrypt y login con JWT |
| `Backend/src/main/java/org/bulbaseros/controller/AuthController.java` | Creado | Controlador REST exponiendo endpoints `/register` y `/login` |
| `Backend/src/test/java/org/bulbaseros/service/AuthServiceTest.java` | Creado | Pruebas unitarias para el servicio de autenticación con Mockito |
| `Backend/src/test/java/org/bulbaseros/controller/AuthControllerTest.java` | Creado | Pruebas de integración web para endpoints REST con MockMvc |
| `features/001-registro-e-inicio-de-sesion-de-ciudadanos/tasks.md` | Modificado | Marcado de Tarea 5 y subtareas 5.1, 5.2 y 5.3 como completadas `[x]` |
| `docs/001-registro-e-inicio-de-sesion-de-ciudadanos/05-tarea.md` | Creado | Documentación técnica de la tarea realizada |

---

## 4. Resultado de la Verificación y Tests

Ejecución de `cd Backend && mvn clean test`:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.bulbaseros.AppTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.config.SecurityConfigTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.controller.AuthControllerTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.dto.ValidationDtoTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.exception.GlobalExceptionHandlerTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.model.UserTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.repository.UserRepositoryTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.security.JwtServiceTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.service.AuthServiceTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 55, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  7.061 s
[INFO] Finished at: 2026-10-20T21:09:21+02:00
[INFO] ------------------------------------------------------------------------
```

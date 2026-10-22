# 001 — Registro e Inicio de Sesión de Ciudadanos — Plan

_Cómo se implementa lo descrito en `spec.md`. Respeta la `constitution/`._

## Enfoque

Implementar un flujo de autenticación desacoplado y sin estado (stateless) basado en tokens JWT y encriptación BCrypt para el rol `CITIZEN`. En el backend (Spring Boot), se exponen endpoints REST seguros validados con Bean Validation y Spring Security, incluyendo soporte CORS para el entorno de desarrollo. En el frontend (React 19 + Vite + React Router), se desarrollan las vistas `/login` y `/register` con validaciones previas en cliente, gestionando la persistencia del token en `localStorage` y su propagación y revocación mediante interceptores de Axios (inyección de cabecera y captura de 401).

## Implementación

_Pasos técnicos concretos en orden y módulos afectados:_

### 1. Configuración de dependencias y seguridad backend
- **Archivo:** `Backend/pom.xml`
  - Incorporar starters de Spring Boot (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `postgresql`, `jjwt-api`, `jjwt-impl`, `jjwt-jackson`, `spring-boot-starter-test`, `spring-security-test`).
- **Archivo:** `Backend/src/main/resources/application.properties`
  - Configuración JDBC hacia `urban_pulse_db`, propiedades JPA/Hibernate y parámetros JWT (`jwt.secret`, `jwt.expiration=86400000` en ms para 24 horas).
- **Archivo:** `Backend/src/main/java/org/bulbaseros/config/SecurityConfig.java`
  - Configuración de filter chain: CSRF deshabilitado, endpoints `/api/auth/**` públicos con `permitAll()`, bean `BCryptPasswordEncoder` y bean `CorsConfigurationSource` habilitando orígenes de desarrollo (`http://localhost:5173`) con métodos GET, POST, PUT, DELETE, OPTIONS y cabeceras autorizadas.

### 2. Capa de dominio y persistencia
- **Archivo:** `Backend/src/main/java/org/bulbaseros/model/UserRole.java`
  - Enum mapeando los roles del sistema (`CITIZEN`, `OPERATOR`, `TECHNICIAN`, `ADMIN`, `ANALYST`).
- **Archivo:** `Backend/src/main/java/org/bulbaseros/model/User.java`
  - Entidad JPA asociada a la tabla `users` con anotaciones `@Column(name = "password_hash")`, `@Enumerated(EnumType.STRING)` y validación de longitud máxima de 150 caracteres para `fullName` y `email`.
- **Archivo:** `Backend/src/main/java/org/bulbaseros/repository/UserRepository.java`
  - Interfaz JPA con métodos `existsByEmail(String email)` y `findByEmail(String email)`.

### 3. Servicio de generación de tokens JWT
- **Archivo:** `Backend/src/main/java/org/bulbaseros/security/JwtService.java`
  - Métodos `generateToken(User user)` con claims `sub` (email/id) y `role`, algoritmo HMAC-SHA256 y expiración a 24 horas (86.400.000 ms).
  - Métodos de extracción de claims y verificación de firma y expiración.

### 4. DTOs y validación de entrada
- **Archivo:** `Backend/src/main/java/org/bulbaseros/dto/RegisterRequest.java`
  - Validaciones: `@NotBlank`, `@Email`, `@Size(max = 150)` para `fullName` y `email`; `@Pattern` para `password` (mínimo 8 caracteres, al menos una letra y un dígito).
- **Archivo:** `Backend/src/main/java/org/bulbaseros/dto/RegisterResponse.java`
  - Objeto con `message` ("Usuario registrado exitosamente") y `email`.
- **Archivo:** `Backend/src/main/java/org/bulbaseros/dto/LoginRequest.java`
  - Anotaciones `@NotBlank`, `@Email` para `email`, y `@NotBlank` para `password`.
- **Archivo:** `Backend/src/main/java/org/bulbaseros/dto/AuthResponse.java`
  - Objeto con `token`, `tokenType` ("Bearer"), `expiresIn` (86400 segundos), `role` ("CITIZEN") y `fullName`.

### 5. Lógica de negocio y controladores REST
- **Archivo:** `Backend/src/main/java/org/bulbaseros/service/AuthService.java`
  - Método `register(...)`: verifica unicidad de email (lanza excepción si existe), encripta la contraseña con BCrypt y guarda con rol `CITIZEN` devolviendo `RegisterResponse` (RF-1, RF-2).
  - Método `login(...)`: busca usuario por email y verifica contraseña con `passwordEncoder.matches`; ante discrepancia lanza excepción genérica de credenciales; genera y retorna token JWT con `AuthResponse` (RF-6, RF-7).
- **Archivo:** `Backend/src/main/java/org/bulbaseros/controller/AuthController.java`
  - `POST /api/auth/register` (devuelve HTTP 201 con `RegisterResponse`).
  - `POST /api/auth/login` (devuelve HTTP 200 con `AuthResponse`).
- **Archivo:** `Backend/src/main/java/org/bulbaseros/exception/GlobalExceptionHandler.java`
  - Mapeo de errores: `MethodArgumentNotValidException` → HTTP 400, `EmailAlreadyExistsException` → HTTP 409, `BadCredentialsException` → HTTP 401.

### 6. Configuración de cliente HTTP e interceptores en Frontend
- **Archivo:** `frontend/package.json`
  - Dependencias añadidas y documentadas: `axios`, `react-router-dom`.
- **Archivo:** `frontend/src/services/api.js`
  - Creación de cliente Axios con base URL.
  - Interceptor de peticiones: inyecta `Authorization: Bearer <token>` si hay token en `localStorage` (RF-9).
  - Interceptor de respuestas: ante respuesta con status 401, elimina token de `localStorage` y redirige a `/login` (RF-10).
- **Archivo:** `frontend/src/services/authService.js`
  - Métodos `login(email, password)`, `register(fullName, email, password)`, `logout()` y `isAuthenticated()`.

### 7. Vistas e interfaz de usuario en React
- **Archivo:** `frontend/src/pages/RegisterPage.jsx` (`/register`)
  - Formulario con validación en cliente (campos obligatorios, formato email, máximo 150 caracteres, contraseña de 8 caracteres con letra y dígito).
  - Tras HTTP 201, feedback de confirmación y redirección automática a `/login` (RF-4, RF-5).
  - Redirección a `/` si el usuario ya está autenticado (RF-11).
- **Archivo:** `frontend/src/pages/LoginPage.jsx` (`/login`)
  - Formulario con campos de Email y Password y validación en cliente.
  - Guarda token en `localStorage` y redirige a `/` tras éxito (RF-8).
  - Feedback visual ante error HTTP 401.
  - Redirección a `/` si el usuario ya está autenticado (RF-11).
- **Archivo:** `frontend/src/App.jsx`
  - Configuración de React Router con rutas `/`, `/login`, `/register`.

## Decisiones

- **Autenticación Stateless con JWT frente a Sesiones Stateful:**
  - _Justificación:_ Permite desacoplar totalmente el cliente React del servidor Spring Boot, facilitando la escalabilidad cloud horizontal.
- **BCrypt para hashing de contraseñas:**
  - _Justificación:_ Estándar de la industria soportado de forma nativa por Spring Security con factor de trabajo adaptable.
- **Unidad de tiempo de expiración estandarizada:**
  - _Justificación:_ Se configura en el backend internamente a 86.400.000 ms (24h) y se expone al cliente en segundos (`86400`) siguiendo el estándar RFC 6749 / OAuth2.
- **Interceptor bidireccional en Axios:**
  - _Justificación:_ Centraliza tanto la autorización saliente como la detección reactiva de sesiones expiradas o inválidas (401), purgando `localStorage` sin dispersar lógica en componentes.

## Riesgos

- **Inconsistencia de tipos ENUM de PostgreSQL:**
  - _Riesgo:_ La columna `role` es un tipo ENUM nativo (`user_role`).
  - _Mitigación:_ Configurar la anotación `@Enumerated(EnumType.STRING)` y asegurar coincidencia exacta de strings con el script SQL.
- **Bloqueo por CORS:**
  - _Riesgo:_ Peticiones del frontend (`http://localhost:5173`) bloqueadas por el backend (`http://localhost:8080`).
  - _Mitigación:_ Configurar explícitamente `CorsConfigurationSource` en Spring Security autorizando el origen de Vite.
- **Desincronización de expiración:**
  - _Riesgo:_ Que el frontend asuma que el token sigue vivo mientras el servidor ya lo rechazó.
  - _Mitigación:_ El servidor es la fuente única de verdad; el interceptor de respuesta en frontend captura el 401 y purga la sesión de inmediato.

## Estrategia de tests

- **Backend Unitarios & Integración (Spring Boot Test):**
  - `AuthServiceTest`: Test de registro exitoso (retorno 201 y BCrypt), fallo por email duplicado (conflicto 409), login exitoso y fallo por credenciales incorrectas (401).
  - `AuthControllerTest` (`@WebMvcTest`): Verificación de contratos HTTP (201, 400 por payload inválido o exceso de 150 caracteres, 401 genérico y 409).
  - `JwtServiceTest`: Validación de generación, expiración a 24h y extracción de claims (`sub`, `role`).
- **Frontend / Integración:**
  - Pruebas de validación en cliente para longitudes máximas, formato de correo y requisitos de contraseña.
  - Prueba de interceptor de Axios para verificar inyección de `Authorization: Bearer` y manejo de respuesta 401 (limpieza de `localStorage` y redirección a `/login`).

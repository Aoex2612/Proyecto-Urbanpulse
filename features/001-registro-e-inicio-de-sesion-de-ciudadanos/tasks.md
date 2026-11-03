# 001 — Registro e Inicio de Sesión de Ciudadanos — Tareas

_Checklist accionable derivada del `plan.md`. Tareas principales y subtareas pequeñas y concretas; marca `[x]` al completarlas._

## [x] Tarea 1: Configurar dependencias, seguridad y CORS en Spring Boot - RF: RF-1, RF-6, RF-7
- [x] 1.1 Configurar starters en `Backend/pom.xml` (`spring-boot-starter-web`, `data-jpa`, `security`, `validation`, `postgresql`, `jjwt`).
- [x] 1.2 Configurar `application.properties` con conexión a `urban_pulse_db` y secreto/expiración de JWT (`jwt.expiration=86400000`).
- [x] 1.3 Implementar `SecurityConfig` configurando CSRF deshabilitado, endpoints `/api/auth/**` públicos y bean `BCryptPasswordEncoder`.
- [x] 1.4 Configurar `CorsConfigurationSource` en Spring Security autorizando el origen de desarrollo frontend `http://localhost:5173`.

## [x] Tarea 2: Entidad de Dominio y Repositorio de Usuario - Depende de Tarea 1 - RF: RF-1, RF-2
- [x] 2.1 Crear enumerado `UserRole` (`CITIZEN`, `OPERATOR`, `TECHNICIAN`, `ADMIN`, `ANALYST`).
- [x] 2.2 Crear entidad `User` vinculada a tabla `users` con `@Column(name = "password_hash")`, validaciones de longitud (máx 150 caracteres para `fullName` y `email`) y rol `CITIZEN`.
- [x] 2.3 Crear repositorio `UserRepository` con métodos `existsByEmail` y `findByEmail`.

## [x] Tarea 3: Proveedor y Servicio de Tokens JWT - Depende de Tarea 1 - RF: RF-6, RF-9
- [x] 3.1 Implementar `JwtService` para generar token con claims `sub` y `role`, algoritmo HMAC-SHA256 y expiración a 24 horas (86.400 segundos).
- [x] 3.2 Implementar métodos de extracción de claims y validación de expiración y firma.

## [x] Tarea 4: DTOs, Validaciones y Manejador Global de Excepciones - Depende de Tarea 2 - RF: RF-1, RF-2, RF-3, RF-7
- [x] 4.1 Crear `RegisterRequest` (`@NotBlank`, `@Email`, `@Size(max = 150)`, `@Pattern` para contraseña de mín 8 caracteres con letra y dígito) y `RegisterResponse`.
- [x] 4.2 Crear `LoginRequest` con validaciones y `AuthResponse` con `token`, `tokenType`, `expiresIn: 86400`, `role` y `fullName`.
- [x] 4.3 Crear `GlobalExceptionHandler` mapeando errores de validación (400), conflicto de email duplicado (409) y credenciales inválidas (401).

## [x] Tarea 5: Lógica de Autenticación y Controladores REST - Depende de Tareas 2, 3, 4 - RF: RF-1, RF-2, RF-6, RF-7
- [x] 5.1 Implementar `AuthService.register` verificando unicidad de correo, encriptando con BCrypt y asignando rol `CITIZEN`.
- [x] 5.2 Implementar `AuthService.login` verificando credenciales, controlando errores genéricos (401) y emitiendo JWT.
- [x] 5.3 Implementar `AuthController` exponiendo `POST /api/auth/register` (HTTP 201) y `POST /api/auth/login` (HTTP 200).

## [x] Tarea 6: Pruebas Unitarias y de Integración Backend - Depende de Tareas 1 y 5 - RF: RF-1, RF-2, RF-3, RF-6, RF-7
- [x] 6.1 Crear pruebas unitarias para `AuthService` (registro exitoso, conflicto por email duplicado, login exitoso y fallo 401).
- [x] 6.2 Crear pruebas de controlador con `@WebMvcTest` para validar códigos de respuesta HTTP (201, 400 por payload inválido o exceso de 150 caracteres, 401, 409).
- [x] 6.3 Crear prueba unitaria para `JwtService` validando expiración en 24h y claims.

## [x] Tarea 7: Dependencias Frontend y Configuración de Cliente Axios - RF: RF-9, RF-10
- [x] 7.1 Instalar dependencias `axios` y `react-router-dom` en `frontend/package.json`.
- [x] 7.2 Crear `src/services/api.js` con interceptor de peticiones para inyectar `Authorization: Bearer <token>` desde `localStorage`.
- [x] 7.3 Configurar en `src/services/api.js` interceptor de respuestas para capturar error 401, limpiar `localStorage` y redirigir a `/login`.
- [x] 7.4 Crear `src/services/authService.js` con funciones de comunicación con `/api/auth/register`, `/api/auth/login` y `logout`.

## [x] Tarea 8: Pantalla de Registro de Ciudadanos (`/register`) - Depende de Tarea 7 - RF: RF-3, RF-4, RF-5, RF-11
- [x] 8.1 Desarrollar componente de vista `RegisterPage.jsx` con formulario de Nombre completo, Correo y Contraseña.
- [x] 8.2 Añadir validaciones reactivas en cliente (campos no vacíos, formato email, máximo 150 caracteres y contraseña de 8 caracteres con letra y dígito).
- [x] 8.3 Conectar con `authService.register`, mostrando mensaje de éxito y redirigiendo a `/login` tras código 201.
- [x] 8.4 Redirigir a `/` si el usuario ya se encuentra autenticado.

## [x] Tarea 9: Pantalla de Inicio de Sesión (`/login`) - Depende de Tarea 7 - RF: RF-7, RF-8, RF-11
- [x] 9.1 Desarrollar componente de vista `LoginPage.jsx` con campos de Correo y Contraseña.
- [x] 9.2 Conectar con `authService.login`, almacenar token en `localStorage` ante éxito y redirigir a `/`.
- [x] 9.3 Manejar errores genéricos HTTP 401 mostrando feedback visual al usuario.
- [x] 9.4 Redirigir a `/` si el usuario ya se encuentra autenticado.

## [x] Tarea 10: Enrutamiento y Verificación Integral - Depende de Tareas 6, 8, 9 - RF: Todos
- [x] 10.1 Configurar rutas (`/`, `/login`, `/register`) en `src/App.jsx` con React Router.
- [x] 10.2 Validar contra todos los criterios de aceptación de `spec.md`.
- [x] 10.3 Mover la feature a "Hecho" en `constitution/roadmap.md`.

---
## Mantenimiento (checklist recurrente)

- [x] Verificar que `mvn test` y `npm run lint` pasen limpios antes de cada commit.

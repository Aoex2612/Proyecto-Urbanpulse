# QA Note — Validación de Feature 001: Registro e Inicio de Sesión de Ciudadanos

**Fecha de validación:** 2026-10-21  
**Rol evaluador:** QA Engineer  
**Feature:** `001-registro-e-inicio-de-sesion-de-ciudadanos`  
**Rama:** `feature/HU01`  
**Veredicto final:** ✅ **APROBADA (Cumplimiento al 100%)**

---

## 1. Trazabilidad de Requisitos Funcionales (EARS) y Cobertura de Tests

| ID Requisito | Descripción EARS | Tests / Mecanismo de Validación | Resultado |
|---|---|---|---|
| **RF-1** | CUANDO registro con datos válidos y email no existente, EL SISTEMA crea usuario `CITIZEN` con BCrypt y responde HTTP 201 con mensaje JSON | `AuthServiceTest.register_Success_ShouldCreateCitizenUserAndReturnResponse`<br>`AuthControllerTest.register_ValidRequest_ShouldReturn201Created`<br>`SecurityConfigTest.passwordEncoder_ShouldHashAndMatchPasswords` | ✅ PASA |
| **RF-2** | SI email ya registrado, ENTONCES EL SISTEMA deniega y responde HTTP 409 | `AuthServiceTest.register_EmailAlreadyExists_ShouldThrowConflictException`<br>`AuthControllerTest.register_EmailAlreadyExists_ShouldReturn409Conflict`<br>`GlobalExceptionHandlerTest.handleEmailAlreadyExists_ShouldReturn409Conflict` | ✅ PASA |
| **RF-3** | SI campos ausentes, longitud > 150, email inválido o password < 8 caracteres sin letra y dígito, ENTONCES EL SISTEMA responde HTTP 400 | `ValidationDtoTest` (5 tests de validación)<br>`AuthControllerTest.register_InvalidRequest_ShouldReturn400BadRequest`<br>`AuthControllerTest.register_FullNameExceeds150Chars_ShouldReturn400BadRequest`<br>`AuthControllerTest.register_EmailExceeds150Chars_ShouldReturn400BadRequest`<br>`GlobalExceptionHandlerTest.handleValidationExceptions_ShouldReturn400BadRequest` | ✅ PASA |
| **RF-4** | CUANDO interactúa con `/register`, EL SISTEMA valida en cliente campos, longitud máxima, formato y contraseña antes de enviar | `RegisterPage.jsx` (`validateForm` con regex de email y contraseña)<br>Validación estática con `npm run lint` (0 errores) | ✅ PASA |
| **RF-5** | CUANDO registro exitoso (HTTP 201), EL SISTEMA muestra confirmación y redirige a `/login` | `RegisterPage.jsx` (gestión de estado `successMessage` y `navigate('/login')`) | ✅ PASA |
| **RF-6** | CUANDO login con credenciales válidas, EL SISTEMA valida hash y entrega JWT (24h) con claims `sub` y `role` | `AuthServiceTest.login_Success_ShouldReturnAuthResponse`<br>`AuthControllerTest.login_ValidCredentials_ShouldReturn200Ok`<br>`JwtServiceTest.generateToken_ShouldContainStandardClaimsSubAndRole`<br>`JwtServiceTest.generateToken_ShouldExpireIn24Hours` | ✅ PASA |
| **RF-7** | SI usuario no registrado o contraseña errónea, ENTONCES EL SISTEMA devuelve HTTP 401 genérico | `AuthServiceTest.login_UserNotFound_ShouldThrowInvalidCredentialsException`<br>`AuthServiceTest.login_WrongPassword_ShouldThrowInvalidCredentialsException`<br>`AuthControllerTest.login_InvalidCredentials_ShouldReturn401Unauthorized`<br>`GlobalExceptionHandlerTest.handleInvalidCredentials_ShouldReturn401Unauthorized` | ✅ PASA |
| **RF-8** | CUANDO login exitoso en `/login`, EL SISTEMA guarda token en `localStorage` y redirige a `/` | `authService.js` (`localStorage.setItem('token', ...)`)<br>`LoginPage.jsx` (`navigate('/', { replace: true })`) | ✅ PASA |
| **RF-9** | EL SISTEMA inyecta automáticamente cabecera `Authorization: Bearer <token>` mediante interceptor Axios | `api.js` (`api.interceptors.request.use` inyectando header) | ✅ PASA |
| **RF-10** | SI petición saliente recibe HTTP 401, ENTONCES EL SISTEMA elimina token de `localStorage` y redirige a `/login` | `api.js` (`api.interceptors.response.use` capturando 401 y purgando sesión) | ✅ PASA |
| **RF-11** | MIENTRAS sesión autenticada con token válido, SI intenta acceder a `/login` o `/register`, ENTONCES redirige a `/` | `LoginPage.jsx` y `RegisterPage.jsx` (`useEffect` con comprobación de sesión)<br>`ProtectedRoute.jsx` protegiendo la ruta raíz | ✅ PASA |

---

## 2. Evaluación de Requisitos No Funcionales (RNF)

1. **Seguridad y Criptografía:**
   - Hashing seguro con `BCryptPasswordEncoder` verificado en tests unitarios.
   - Las contraseñas no se modifican con trim, respetando el secreto exacto introducido por el usuario.
   - La entidad `User` excluye explícitamente el hash de la contraseña en su método `toString()` para evitar fugas en logs (`UserTest.toString_ShouldNotIncludePasswordHash()`).
   - Secreto JWT de 512 bits y algoritmos estándar HMAC-SHA256 (RFC 7519).
   - *Resultado: CUMPLIDO.*

2. **Formato y Expiración del Token:**
   - Expiración a 24 horas (86.400.000 ms) verificada por `JwtServiceTest.generateToken_ShouldExpireIn24Hours()`.
   - *Resultado: CUMPLIDO.*

3. **Validación y UX:**
   - Validaciones reactivas en cliente en ambas páginas (`/register` y `/login`).
   - Gestión adecuada de estados de carga ("Creando cuenta...", "Iniciando sesión...").
   - *Resultado: CUMPLIDO.*

---

## 3. Revisión de Calidad de Código y Estándares

- **Backend (Java / Spring Boot):**
  - Todas las clases, interfaces, métodos y atributos cuentan con documentación **Javadoc** exhaustiva (`@param`, `@return`, `@throws`).
  - Suite de pruebas de Maven ejecutada: **59 tests ejecutados, 0 fallos, 0 errores, 0 omitidos (`BUILD SUCCESS`)**.
- **Frontend (React 19 / Vite):**
  - Documentación con **JSDoc** en componentes y utilidades.
  - Validación con ESLint: **0 errores, 0 advertencias**.
  - Compilación de producción con Vite: **`vite build` exitoso en 258ms**.

---

## 4. Comprobación de Criterios de Aceptación de `spec.md`

- [x] Endpoint `POST /api/auth/register` operativo con HTTP 201 y mensaje JSON.
- [x] Validación de email duplicado devolviendo HTTP 409 sin redundancias.
- [x] Validación de campos vacíos, longitud >150 chars y contraseña con HTTP 400.
- [x] Endpoint `POST /api/auth/login` operativo con token JWT (24h) y claims `sub`/`role`.
- [x] Error HTTP 401 genérico ante credenciales erróneas o usuario inexistente.
- [x] Pantallas `/login` y `/register` funcionales con validaciones en cliente.
- [x] Redirección automática tras registro (a `/login`) y login (a `/`).
- [x] Redirección a `/` si un usuario autenticado visita `/login` o `/register`.
- [x] Token en `localStorage` e interceptor de Axios operativo (inyección y captura 401).
- [x] Suite de pruebas automatizadas al 100% en verde.

---

## 5. Guía de Pruebas Manuales Paso a Paso (Para Humanos)

Esta guía permite a cualquier persona comprobar manualmente el flujo de registro, inicio de sesión y gestión de sesión en el navegador.

### Paso 0: Levantar el entorno local
1. Iniciar el backend:
   ```bash
   cd Backend && mvn spring-boot:run
   ```
2. Iniciar el frontend:
   ```bash
   cd frontend && npm run dev
   ```
3. Abre tu navegador en `http://localhost:5173`.

---

### Prueba 1: Registro de un nuevo ciudadano
1. Ve a `http://localhost:5173/register`.
2. Introduce los siguientes datos:
   - **Nombre completo:** `Ana García`
   - **Correo electrónico:** `ana.garcia@urbanpulse.es`
   - **Contraseña:** `Segura1234` (al menos 8 caracteres, con letra y número).
3. Haz clic en el botón **"Crear cuenta"**.
4. **Resultado esperado:**
   - Aparece un mensaje verde de confirmación de registro.
   - En un instante te redirige automáticamente a la pantalla de login (`/login`).

---

### Prueba 2: Validaciones de error en registro
1. En `http://localhost:5173/register`:
   - Intenta registrarte de nuevo con el mismo email `ana.garcia@urbanpulse.es`.
   - *Resultado esperado:* Aparece un aviso de error indicando que el correo electrónico ya está registrado (HTTP 409).
2. Borra los campos o introduce una contraseña simple como `123`.
   - *Resultado esperado:* El formulario no se envía y muestra un mensaje en rojo indicando el formato exigido.

---

### Prueba 3: Inicio de sesión correcto y fallido
1. Ve a `http://localhost:5173/login`.
2. **Prueba de fallo:** Escribe `ana.garcia@urbanpulse.es` con contraseña incorrecta `claveErronea1` y pulsa "Iniciar sesión".
   - *Resultado esperado:* Mensaje de error `"Credenciales inválidas"` (HTTP 401) y no te deja entrar.
3. **Prueba correcta:** Escribe `ana.garcia@urbanpulse.es` con la contraseña correcta `Segura1234` y pulsa "Iniciar sesión".
   - *Resultado esperado:*
     - Te redirige a la página principal (`/`).
     - Se guarda el token JWT en el almacenamiento del navegador (`localStorage`).
     - Ves el saludo de bienvenida con tu nombre y el botón de **"Cerrar sesión"**.

---

### Prueba 4: Protección de rutas y cierre de sesión
1. Con la sesión iniciada, intenta navegar a `http://localhost:5173/login` o `http://localhost:5173/register`.
   - *Resultado esperado:* Te redirige automáticamente a `/` para evitar que vuelvas a autenticarte estando ya conectado.
2. Haz clic en **"Cerrar sesión"**.
   - *Resultado esperado:* Se elimina el token de `localStorage` y te redirige a `/login`. Si intentas entrar a `/`, te bloqueará y te pedirá iniciar sesión.

---

## 6. Veredicto del QA

> **¿La especificación está cumplida?**  
> **SÍ.** Todos los requisitos funcionales, no funcionales y criterios de aceptación se cumplen estrictamente sin desviaciones ni errores. No se requiere archivo auxiliar de reparaciones (`repare.md`). La feature está lista para ser integrada a la rama principal mediante Pull Request.

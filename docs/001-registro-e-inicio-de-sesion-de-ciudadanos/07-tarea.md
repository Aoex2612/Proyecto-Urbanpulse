# Tarea 7: Dependencias Frontend y Configuración de Cliente Axios

**Feature:** `001-registro-e-inicio-de-sesion-de-ciudadanos`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-9, RF-10  

---

## 1. Resumen de la Tarea

Se ha configurado la infraestructura de cliente HTTP en el frontend React/Vite para permitir la comunicación segura y desacoplada con la API REST de UrbanPulse. Se instalaron las dependencias esenciales (`axios` y `react-router-dom`), se configuró la instancia centralizada de Axios en `frontend/src/services/api.js` con interceptores bidireccionales (inyección automática de cabecera `Authorization: Bearer <token>` y captura del error HTTP 401 con purga de sesión y redirección a `/login`), y se implementó el servicio de autenticación en `frontend/src/services/authService.js` con métodos para registro, login, logout y verificación de sesión, documentados rigurosamente con JSDoc.

---

## 2. Subtareas implementadas

### 2.1 Dependencias en `frontend/package.json`
- Se añadieron e instalaron las librerías:
  - `axios` (`^1.20.0`): Cliente HTTP basado en promesas para consumir la API REST del backend.
  - `react-router-dom` (`^7.18.4`): Enrutamiento declarativo para la navegación cliente (pantallas `/login`, `/register`, `/`).
- Se verificó la integridad del archivo `frontend/package-lock.json`.

### 2.2 Cliente e Interceptor de peticiones en `frontend/src/services/api.js`
- Creación de la instancia Axios con `baseURL` configurable mediante variable de entorno `VITE_API_URL` (con fallback a `http://localhost:8080`) y cabecera `Content-Type: application/json`.
- Configuración de interceptor de peticiones (`api.interceptors.request.use`):
  - Consulta `localStorage.getItem('token')`.
  - Si el token está presente, inyecta la cabecera `Authorization: Bearer <token>` (cumpliendo **RF-9**).

### 2.3 Interceptor de respuestas en `frontend/src/services/api.js`
- Configuración de interceptor de respuesta (`api.interceptors.response.use`):
  - Captura respuestas de error con estado HTTP 401 (`error.response.status === 401`).
  - Purga las credenciales del cliente eliminando `token` y `user` de `localStorage`.
  - Redirige al usuario a la ruta `/login` previniendo bucles si ya se encuentra en dicha página (cumpliendo **RF-10**).
  - Propaga el error mediante `Promise.reject(error)` para que los componentes puedan reaccionar.

### 2.4 Servicio de Autenticación en `frontend/src/services/authService.js`
- Se crearon y exportaron las funciones del servicio:
  - `register(fullName, email, password)`: Realiza una petición `POST /api/auth/register` enviando los datos del ciudadano y retorna los datos de confirmación devueltos por el backend.
  - `login(email, password)`: Realiza una petición `POST /api/auth/login`, almacena el token JWT recibido en `localStorage.setItem('token', token)`, guarda el perfil básico (`fullName`, `role`, `email`) en `localStorage.setItem('user', ...)` y retorna la data.
  - `logout()`: Elimina `token` y `user` de `localStorage`.
  - `getToken()`: Retorna el token JWT actual de `localStorage`.
  - `isAuthenticated()`: Retorna un booleano indicando si existe un token persistido.
  - `getUser()`: Retorna la información parseada del usuario actual o `null`.

### 2.5 Documentación JSDoc y Linting
- Todas las funciones, parámetros (`@param`), tipos y valores de retorno (`@returns`) fueron documentados siguiendo el estándar JSDoc estricto según la directriz de `AGENTS.MD`.
- Se validó el código mediante `npm run lint` en `frontend/` sin ninguna advertencia o error de ESLint.
- Se comprobó la compilación mediante `npm run build` sin errores.

---

## 3. Archivos creados y modificados

| Archivo | Acción | Descripción |
| :--- | :--- | :--- |
| `frontend/package.json` | Modificado | Incorporadas dependencias `axios` y `react-router-dom`. |
| `frontend/package-lock.json` | Modificado | Árbol de dependencias actualizado. |
| `frontend/src/services/api.js` | Creado | Cliente Axios con interceptor de autorización Bearer e interceptor de error 401. |
| `frontend/src/services/authService.js` | Creado | Métodos de consumo para endpoints de autenticación y manejo de sesión local. |
| `features/001-registro-e-inicio-de-sesion-de-ciudadanos/tasks.md` | Modificado | Marcadas la Tarea 7 y subtareas 7.1 a 7.4 como completadas (`[x]`). |
| `docs/001-registro-e-inicio-de-sesion-de-ciudadanos/07-tarea.md` | Creado | Documentación técnica detallada de la Tarea 7. |

---

## 4. Verificación y Calidad

- **ESLint:** Ejecutado `npm run lint` en `frontend/`:
  ```bash
  npm run lint
  > frontend@0.0.0 lint
  > eslint .
  ```
  Resultado: 0 errores, 0 advertencias.
- **Vite Build:** Ejecutado `npm run build` en `frontend/`:
  Resultado: Build exitoso en 91ms sin advertencias.

---

## 5. Siguiente paso

- Proceder con la **Tarea 8: Pantalla de Registro de Ciudadanos (`/register`)**, desarrollando `RegisterPage.jsx` con validaciones reactivas en cliente conectadas a `authService.register` y redirección condicional.

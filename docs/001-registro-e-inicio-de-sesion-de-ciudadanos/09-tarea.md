# Tarea 9: Pantalla de Inicio de Sesión (`/login`)

**Feature:** `001-registro-e-inicio-de-sesion-de-ciudadanos`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-7, RF-8, RF-11  

---

## 1. Resumen de la Tarea

Se ha implementado el componente de interfaz de usuario `LoginPage.jsx` en `frontend/src/pages/` para permitir el acceso autenticado de ciudadanos en UrbanPulse. La vista reutiliza los estilos de autenticación (`Auth.css`), incluye validaciones reactivas en cliente, integración con `authService.login`, persistencia del token JWT y sesión en `localStorage`, control estricto de mensajes genéricos ante credenciales incorrectas (error HTTP 401 sin desvelar detalles de usuario o contraseña), redirección automática a la raíz (`/`) tras inicio de sesión exitoso o si el usuario ya se encuentra autenticado, y soporte para recepción de mensajes informativos y precarga de correo al provenir de la vista de registro.

---

## 2. Subtareas implementadas

### 2.1 Componente de vista en `frontend/src/pages/LoginPage.jsx`
- Formulario de inicio de sesión centrado con campos:
  - Correo electrónico (`email`).
  - Contraseña (`password`).
  - Botón de envío (`Iniciar Sesión`) con estado reactivo de carga ("Iniciando sesión...") que previene múltiples llamadas concurrentes.
  - Enlace inferior navegable hacia `/register` mediante `<Link to="/register">`.
  - Detección de `location.state` para mostrar mensajes de confirmación provenientes del registro (e.g., "Registro exitoso. Inicia sesión con tus credenciales.") y precarga del correo registrado.

### 2.2 Integración y persistencia de sesión (RF-8)
- Invocación a `authService.login(email, password)`.
- El servicio almacena el token JWT recibido en `localStorage.getItem('token')` y los datos del usuario en `localStorage.getItem('user')`.
- Ante éxito, redirige de forma inmediata al usuario a la ruta principal `/` con `{ replace: true }`.

### 2.3 Manejo de errores genéricos (RF-7)
- Si la petición falla con error HTTP 401 (`BadCredentialsException` o usuario inexistente en backend), se captura y despliega un mensaje estrictamente genérico: `"Correo electrónico o contraseña incorrectos."`.
- No se expone información sensible que permita a un atacante inferir si el fallo fue debido al correo o a la contraseña.
- Manejo de contingencia para errores generales de red o servidor con mensajes claros.

### 2.4 Redirección de usuarios autenticados (RF-11)
- Efecto con hook `useEffect` al montar la vista: si `authService.isAuthenticated()` es `true`, redirige inmediatamente a la ruta raíz `/` con `{ replace: true }`.

### 2.5 Validaciones en cliente
- Función `validateForm()`:
  - Verificación de campo obligatorio y formato válido mediante regex para el correo electrónico.
  - Verificación de campo obligatorio para la contraseña.
  - Mensajes de error contextuales bajo los campos afectados.

### 2.6 Documentación JSDoc y Calidad
- Métodos, componentes y funciones auxiliares documentadas con JSDoc.
- Verificación limpia con `npm run lint` y `npm run build`.

---

## 3. Archivos creados y modificados

| Archivo | Acción | Descripción |
| :--- | :--- | :--- |
| `frontend/src/pages/LoginPage.jsx` | Creado | Componente de la página de inicio de sesión con autenticación JWT y feedback de error. |
| `features/001-registro-e-inicio-de-sesion-de-ciudadanos/tasks.md` | Modificado | Marcada la Tarea 9 y subtareas 9.1 a 9.4 como completadas (`[x]`). |
| `docs/001-registro-e-inicio-de-sesion-de-ciudadanos/09-tarea.md` | Creado | Documentación técnica detallada de la Tarea 9. |

---

## 4. Verificación y Calidad

- **ESLint:**
  ```bash
  npm run lint
  > frontend@0.0.0 lint
  > eslint .
  ```
  Resultado: 0 errores, 0 advertencias.
- **Build Vite:**
  ```bash
  npm run build
  ```
  Resultado: Compilación exitosa en 95ms.

---

## 5. Siguiente paso

- Proceder con la **Tarea 10: Enrutamiento y Verificación Integral**, configurando las rutas en `frontend/src/App.jsx` (`/`, `/login`, `/register`) con React Router y verificando la integración end-to-end contra todos los criterios de aceptación.

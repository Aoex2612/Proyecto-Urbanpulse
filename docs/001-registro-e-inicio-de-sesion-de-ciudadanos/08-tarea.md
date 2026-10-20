# Tarea 8: Pantalla de Registro de Ciudadanos (`/register`)

**Feature:** `001-registro-e-inicio-de-sesion-de-ciudadanos`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-3, RF-4, RF-5, RF-11  

---

## 1. Resumen de la Tarea

Se ha implementado el componente de interfaz de usuario `RegisterPage.jsx` en `frontend/src/pages/` para dar soporte a la creación de cuentas de ciudadanos en UrbanPulse. La vista incorpora un diseño usable y accesible (`Auth.css`), validación reactiva de campos obligatorios, restricciones de longitud y complejidad de contraseñas según las reglas de negocio, integración con el servicio `authService.register`, feedback visual de estados (carga, alertas de éxito y error con captura de conflictos 409 o errores 400), redirección a `/login` tras registro exitoso y protección de ruta con redirección a `/` si el usuario ya posee una sesión activa.

---

## 2. Subtareas implementadas

### 2.1 Componente de vista en `frontend/src/pages/RegisterPage.jsx`
- Formulario estructurado y semántico con campos:
  - Nombre completo (`fullName`).
  - Correo electrónico (`email`).
  - Contraseña (`password`).
  - Botón de envío (`Registrarse`) con estado de carga ("Creando cuenta...") y bloqueo para prevenir envíos duplicados.
  - Enlace inferior navegable hacia `/login` con `react-router-dom` (`<Link to="/login">`).

### 2.2 Validaciones reactivas en cliente (RF-3, RF-4)
- Función `validateForm()` ejecutada antes del envío HTTP:
  - **Nombre completo:** Comprobación de campo no vacío tras trimado y límite máximo de 150 caracteres.
  - **Correo electrónico:** Comprobación de campo no vacío tras trimado, límite máximo de 150 caracteres y cumplimiento de formato estándar de correo electrónico mediante expresión regular (`isValidEmail`). Normalización a minúsculas para persistencia.
  - **Contraseña:** Obligatoria, longitud mínima de 8 caracteres y validación de complejidad que requiere al menos una letra y un dígito (`isValidPassword`), respetando la cadena original sin alterarla.
- Resaltado de campos con borde de advertencia (`input-error`) y mensajes específicos debajo de cada control en caso de discrepancia.

### 2.3 Integración con `authService.register` y feedback (RF-5)
- Invocación asíncrona a `authService.register(fullName, email, password)`.
- **Éxito (201):**
  - Muestra alerta verde de confirmación (`¡Registro completado con éxito! Redirigiendo a iniciar sesión...`).
  - Tras un temporizador de 1.5s, redirige automáticamente a `/login` pasando el correo registrado y el mensaje en el `location.state` para facilitar la experiencia de inicio de sesión.
- **Error (409 o 400):**
  - Captura el mensaje específico devuelto por la API REST (`error.response.data.message`), permitiendo notificar adecuadamente al usuario en caso de correo duplicado ("El correo ya está en uso") o fallos de validación del servidor.

### 2.4 Redirección de usuarios autenticados (RF-11)
- Efecto con hook `useEffect` al montar el componente: si `authService.isAuthenticated()` retorna `true`, redirige de inmediato a la ruta raíz `/` con opción `{ replace: true }`.

### 2.5 Estilos centralizados en `frontend/src/pages/Auth.css`
- Tarjeta centrada responsive, tipografía del sistema, estados `:focus`, `:disabled`, `:hover`, y bloques de alerta accesibles (`role="alert"` y `role="status"`).

### 2.6 Documentación JSDoc y Calidad
- Módulo y funciones documentados exhaustivamente con JSDoc.
- Validado con `npm run lint` sin advertencias ni errores.

---

## 3. Archivos creados y modificados

| Archivo | Acción | Descripción |
| :--- | :--- | :--- |
| `frontend/src/pages/RegisterPage.jsx` | Creado | Componente de la página de registro de ciudadanos con validación y navegación. |
| `frontend/src/pages/Auth.css` | Creado | Hoja de estilos compartida para las páginas de autenticación. |
| `features/001-registro-e-inicio-de-sesion-de-ciudadanos/tasks.md` | Modificado | Marcada la Tarea 8 y subtareas 8.1 a 8.4 como completadas (`[x]`). |
| `docs/001-registro-e-inicio-de-sesion-de-ciudadanos/08-tarea.md` | Creado | Documentación técnica detallada de la Tarea 8. |

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
  Resultado: Compilación limpia sin incidencias.

---

## 5. Siguiente paso

- Proceder con la **Tarea 9: Pantalla de Inicio de Sesión (`/login`)**, desarrollando `LoginPage.jsx` con persistencia de token JWT, control de error 401 y redirección a `/`.

# Tarea 5: Componente `RoleGuard`, Extensión de `ProtectedRoute` y Manejo Axios en Frontend

**Feature:** `002-autenticacion-y-control-de-acceso-rbac`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-7, RF-8, RF-10, RF-12  

---

## 1. Resumen de la Tarea

En esta tarea se ha establecido la arquitectura y componentes base para el control de acceso basado en roles (RBAC) en el frontend de UrbanPulse. Se desarrollaron funciones de utilidad para la consulta e inspección normalizada de roles en `authService.js`, se ajustó el interceptor de Axios en `api.js` para asegurar que las respuestas HTTP 403 (Forbidden) no destruyan la sesión del usuario, se implementó el componente `RoleGuard.jsx` con redirección controlada y advertencia visual vía estado de navegación (`location.state`), y se extendió el componente `ProtectedRoute.jsx` para admitir la propiedad opcional `allowedRoles` componiendo internamente `RoleGuard` cuando el usuario se encuentra autenticado.

---

## 2. Subtareas Implementadas

### 2.1 Funciones helper en `frontend/src/services/authService.js`
- **`getUserRole()`**:
  - Consulta de forma segura el usuario autenticado almacenado en `localStorage`.
  - Normaliza la propiedad `role` convirtiéndola a cadena sin espacios en blanco y en mayúsculas (`trim().toUpperCase()`), garantizando coherencia ante roles como `'CITIZEN'`, `'OPERATOR'` o `'ADMIN'`.
  - Retorna `null` si el usuario no ha iniciado sesión o no cuenta con la propiedad `role`.
  - Documentado con JSDoc (`@returns {string|null}`).
- **`hasAnyRole(allowedRoles)`**:
  - Recibe un array o una cadena individual de roles permitidos.
  - Valida si el rol activo del usuario (`getUserRole()`) se encuentra incluido en los roles autorizados (normalizando los valores comparados a mayúsculas).
  - Retorna `false` inmediatamente si el usuario no tiene rol o la lista de roles es nula/vacía.
  - Documentado con JSDoc (`@param {string[]|string} allowedRoles`, `@returns {boolean}`).
- Ambas funciones se exportan de forma individual y se integran en el objeto exportado por defecto `authService`.

### 2.2 Ajuste del interceptor de respuestas en `frontend/src/services/api.js`
- Se ajustó el interceptor de errores en Axios para que la purga de credenciales (`localStorage.removeItem('token')`, `localStorage.removeItem('user')`) y la redirección forzada a `/login` ocurra única y exclusivamente ante códigos de error HTTP 401 (Unauthorized).
- Se documentó explícitamente mediante comentarios y JSDoc que los errores HTTP 403 (Forbidden) no deben ejecutar logout ni eliminar credenciales, preservando la sesión activa del usuario para permitir que la interfaz de usuario gestione alertas y advertencias de permisos insuficientes sin degradar la experiencia de navegación (cumpliendo RF-10).

### 2.3 Componente `frontend/src/components/RoleGuard.jsx`
- Componente de React implementado bajo convención PascalCase.
- Recibe las propiedades `allowedRoles`, `children` y opcionalmente `message`.
- Evalúa los permisos mediante `authService.hasAnyRole(allowedRoles)`.
- Si el usuario cuenta con un rol admitido, renderiza los elementos hijos `children`.
- Si el usuario carece del rol necesario:
  - Redirige hacia la ruta principal (`/`) mediante `<Navigate to="/" replace state={{ accessDenied: true, message: ... }} />`.
  - Emite el mensaje estándar: `'Acceso denegado: no dispones de permisos de operador o administrador'` (o el mensaje personalizado provisto).
  - No invoca `logout()` ni altera `localStorage`, salvaguardando la sesión del usuario intacta.
- Documentado con JSDoc exhaustivo describiendo propiedades, comportamiento y tipos.

### 2.4 Extensión y composición en `frontend/src/components/ProtectedRoute.jsx`
- Actualizado para admitir la propiedad opcional `allowedRoles` (`string[]` o `string`).
- **Comportamiento modular en dos fases**:
  1. **Autenticación (RF-7)**: Si `!authService.isAuthenticated()`, redirige a `/login` con `replace`.
  2. **Autorización (RF-12)**: Si el usuario está autenticado y se proporciona `allowedRoles`, compone y retorna `<RoleGuard allowedRoles={allowedRoles}>{children}</RoleGuard>`.
  3. **Paso directo**: Si el usuario está autenticado y no se definen roles específicos, retorna directamente `children`.
- Documentado rigurosamente con JSDoc (`@param`, `@returns`, `@fileoverview`).

---

## 3. Verificación de Criterios de Aceptación (EARS)

- **RF-7:** Usuario anónimo que accede a ruta con `ProtectedRoute` es redirigido a `/login`.
- **RF-8:** Usuario autenticado con rol insuficiente (ej. `CITIZEN`) accediendo a ruta con `allowedRoles={['OPERATOR', 'ADMIN']}` es redirigido a `/` con `{ accessDenied: true, message: 'Acceso denegado: no dispones de permisos de operador o administrador' }`.
- **RF-10:** Durante la denegación de acceso en frontend (o error 403 en backend), las credenciales en `localStorage` se mantienen intactas sin cerrar la sesión del usuario.
- **RF-12:** Separación de responsabilidades clara y desacoplada mediante la composición `ProtectedRoute` -> `RoleGuard`.

---

## 4. Validación y Calidad de Código

Se ejecutaron las herramientas de análisis estático y empaquetado del cliente en la raíz del proyecto `frontend`:
- `npm run lint`: **0 errores**, sin incidencias de ESLint ni advertencias en los módulos modificados y creados.
- `npm run build`: Compilación exitosa con Vite sin advertencias de tipos ni resolución de módulos.

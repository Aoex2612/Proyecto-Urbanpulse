# Tarea 6: Vistas Frontend del Panel de Operador y Gestión de Acceso Denegado

**Feature:** `002-autenticacion-y-control-de-acceso-rbac`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-8, RF-9, RF-10, RF-11  

---

## 1. Resumen de la Tarea

En esta tarea se han desarrollado e integrado las interfaces de usuario correspondientes al panel operativo y la gestión de acceso denegado en el cliente web de UrbanPulse. Se creó la vista `OperatorPanelPage.jsx` con estilos dedicados en `OperatorPanelPage.css`, proveyendo a los roles `OPERATOR` y `ADMIN` una consola visual para el triaje, clasificación y cambio de estado de incidencias. Asimismo, se actualizó la vista `HomePage.jsx` para capturar la bandera de navegación `accessDenied` procedente de `RoleGuard`, desplegando un banner de advertencia no bloqueante con opción de descarte y limpieza del historial, e incorporando enlaces y accesos directos condicionados al rol del usuario mediante `authService.hasAnyRole(['OPERATOR', 'ADMIN'])`. Por último, se registró la ruta protegida `/operator-panel` en `App.jsx` bajo `<ProtectedRoute allowedRoles={['OPERATOR', 'ADMIN']}>` y se validó la ausencia total de errores o advertencias en `npm run lint` y `npm run build`.

---

## 2. Subtareas Implementadas

### 2.1 Creación del componente `frontend/src/pages/OperatorPanelPage.jsx` y `OperatorPanelPage.css`
- **Componente `OperatorPanelPage.jsx`**:
  - Implementado en PascalCase y documentado exhaustivamente con JSDoc en cabecera de módulo y funciones.
  - Diseñado exclusivamente para roles operativos (`OPERATOR` y `ADMIN`).
  - Cabecera con marca UrbanPulse, distintivo "Panel de Operador", enlace de retorno al "Portal Ciudadano" (`/`) y botón de "Cerrar sesión" seguro mediante `authService.logout()`.
  - Tarjeta de resumen operativo con presentación del usuario autenticado (`authService.getUser()`), su rol normalizado y métricas de triaje (incidencias pendientes, en tramitación, resueltas y estado de protección RBAC).
  - Interfaz de triaje interactiva con tabla de incidencias mock (`INITIAL_INCIDENTS`), etiquetas de prioridad semánticas (Alta, Media, Baja), categorías y selector de cambio de estado (`handleStatusChange`), proporcionando retroalimentación visual al operador.
  - Cuadro informativo de seguridad sobre la protección perimetral del endpoint `PATCH /api/incidents/:id/status`.
- **Hoja de estilos `frontend/src/pages/OperatorPanelPage.css`**:
  - Totalmente adaptada a las variables de diseño del proyecto (`--bg`, `--border`, `--text`, `--text-h`, `--code-bg`, etc.).
  - Soporte responsivo para visualización móvil y de escritorio con tablas con scroll horizontal adaptado (`overflow-x: auto`).

### 2.2 Detección y banner de acceso denegado en `frontend/src/pages/HomePage.jsx`
- Lectura reactiva del estado de navegación `location.state?.accessDenied` y del mensaje descriptivo transmitido por `RoleGuard` (cumpliendo RF-8 y RF-9).
- Implementación de estado local `dismissedLocationKey` sincronizado con la clave de navegación `location.key`, evitando efectos secundarios cascada y respetando estrictamente las directrices de `eslint-plugin-react-hooks`.
- Despliegue de un banner visual de advertencia (`.access-denied-alert`) con rol ARIA `alert`, icono de advertencia, título destacado y botón de descarte (`×`).
- Manejador `handleDismissAccessDenied`:
  - Oculta el banner inmediatamente.
  - Limpia el historial de navegación mediante `navigate(location.pathname, { replace: true, state: {} })` para garantizar que recargas de página o navegaciones posteriores no mantengan el aviso activo si ya fue descartado.
- Manejador `handleNavClick` en enlaces internos para purgar el estado de navegación residual mediante `window.history.replaceState`.

### 2.3 Acceso directo al Panel de Operador en `frontend/src/pages/HomePage.jsx`
- Verificación de permisos del usuario conectado con `authService.hasAnyRole(['OPERATOR', 'ADMIN'])`.
- Si el usuario cuenta con rol `OPERATOR` o `ADMIN`:
  1. En la cabecera: enlace directo visible (`.operator-nav-link`) hacia `/operator-panel` junto al botón de cerrar sesión.
  2. En el cuerpo principal (`.features-preview`): tarjeta destacada (`.operator-feature-card`) informando al operador sobre sus permisos y proporcionando un botón de acceso directo a la consola de triaje.
- Si el usuario cuenta únicamente con rol `CITIZEN` (o es anónimo), los accesos directos al panel permanecen invisibles.

### 2.4 Configuración de la ruta protegida `/operator-panel` en `frontend/src/App.jsx`
- Importación del nuevo componente `OperatorPanelPage`.
- Configuración de la ruta `/operator-panel`:
  ```jsx
  <Route
    path="/operator-panel"
    element={
      <ProtectedRoute allowedRoles={['OPERATOR', 'ADMIN']}>
        <OperatorPanelPage />
      </ProtectedRoute>
    }
  />
  ```
- Al navegar un usuario anónimo hacia `/operator-panel`, `ProtectedRoute` comprueba `!isAuthenticated()` y redirige hacia `/login` (RF-7).
- Al navegar un ciudadano (`CITIZEN`) hacia `/operator-panel`, `RoleGuard` comprueba `!hasAnyRole(['OPERATOR', 'ADMIN'])` y redirige hacia `/` con `state: { accessDenied: true, message: ... }` preservando el token y las credenciales en `localStorage` (RF-8, RF-10).
- Al navegar un operador (`OPERATOR`) o administrador (`ADMIN`), `RoleGuard` concede el paso y se renderiza el panel de triaje (RF-11).

### 2.5 Validación estática y compilación de producción en Frontend
- Ejecución de `npm run lint`: **0 errores, 0 advertencias**.
- Ejecución de `npm run build`: Compilación con Vite de los 90 módulos exitosa sin advertencias ni roturas de tipos.

---

## 3. Verificación de Criterios de Aceptación (EARS)

- **RF-8:** Al intentar acceder un usuario con rol insuficiente (ej. `CITIZEN`) a `/operator-panel`, es redirigido a `/` con `location.state = { accessDenied: true, message: "Acceso denegado: no dispones de permisos de operador o administrador" }`.
- **RF-9:** La vista `HomePage` detecta `location.state.accessDenied` y renderiza el banner de alerta visual destacando el mensaje descriptivo con control de cierre.
- **RF-10:** Durante la denegación de acceso y la visualización de la alerta, las credenciales en `localStorage` se mantienen activas sin cerrar sesión ni ejecutar `logout()`.
- **RF-11:** Al acceder un usuario autenticado con rol `OPERATOR` o `ADMIN` a `/operator-panel`, se muestra de manera completa la interfaz de gestión operativa y triaje.

---

## 4. Validación y Calidad de Código

```bash
$ npm run lint
> frontend@0.0.0 lint
> eslint .

# 0 problemas encontrados

$ npm run build
> frontend@0.0.0 build
> vite build

vite v8.3.3 building client environment for production...
✓ 90 modules transformed.
rendering chunks (1)...computing gzip size...
dist/index.html                   0.45 kB │ gzip:   0.29 kB
dist/assets/index-BHepUxkf.css   12.41 kB │ gzip:   2.82 kB
dist/assets/index-CFoHGxKx.js   328.37 kB │ gzip: 105.49 kB
✓ built in 131ms
```

# Tarea 6: Vista de Seguimiento "Mis Incidencias" y Navegación en Frontend

**Feature:** `004-reporte-y-seguimiento-de-incidencias`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-11, RF-12, RF-13, RF-14, RF-15, RF-16, RF-17  
**Requisitos no funcionales:** Accesibilidad (a11y), diseño responsive, usabilidad, robustez en paginación y degradación controlada  

---

## 1. Resumen de la Tarea

En esta tarea se ha diseñado, implementado y probado exhaustivamente la vista de seguimiento ciudadano **"Mis Incidencias"** (`MyIncidentsPage`), su integración en la barra de navegación global y la interacción con el modal de detalle de incidencias (`IncidentDetailModal`), cubriendo las siguientes capacidades:

1. **Componente de Seguimiento Ciudadano (`frontend/src/pages/MyIncidentsPage.jsx`):**
   - **Paginación y Tamaño de Página:** Carga por defecto 10 elementos por página (`PAGE_SIZE = 10`) consultando de forma asíncrona `incidentService.getMyIncidents({ page, size: 10, status })`.
   - **Filtro Desplegable por Estado:** Selector con opción predeterminada `"TODOS"` (muestra la totalidad de incidencias del usuario) y los 8 estados oficiales del ciclo de vida (`REPORTED`, `VALIDATED`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `REOPENED`, `CLOSED`, `REJECTED`). Al cambiar de estado, reinicia automáticamente la paginación a la página 0.
   - **Tabla Responsive y Datos Clave:**
     - Título y categoría temático con chip distintivo.
     - Estado actual con badge coloreado correspondiente a su fase (`status-reported`, `status-validated`, `status-assigned`, `status-in_progress`, `status-resolved`, `status-reopened`, `status-closed`, `status-rejected`).
     - Dirección postal y Distrito municipal resueltos, con fallback informativo `"Sin asignar"` en caso de valores nulos o vacíos.
     - Fechas clave formateadas con `formatDate` (`createdAt` como fecha de reporte y `updatedAt` como última actualización).
   - **Controles de Paginación Accesibles:**
     - Botón "Página anterior" deshabilitado en página 0 o durante estados de carga.
     - Indicador textual de página con soporte accesible `aria-live="polite"` ("Página X de Y", o "Página 1 de 1" ante listados vacíos).
     - Botón "Página siguiente" deshabilitado en la última página o cuando el contenido está vacío.
   - **Estados de Carga y Manejo de Errores:**
     - Indicador de carga accesible (`role="status"`, `aria-live="polite"`).
     - Mensaje de error (`role="alert"`) con botón "Reintentar" para reejecutar la consulta sin recargar la página.
   - **Estado Vacío Amigable:**
     - Si el ciudadano no tiene avisos (`totalElements === 0` sin filtros): mensaje motivador ("No tienes incidencias registradas todavía") y botón de acción directa "Reportar incidencia en el mapa" (`/map`).
     - Si no hay coincidencias debido a un filtro por estado: mensaje explicativo ("No se encontraron incidencias con el estado seleccionado") y botón para restaurar el filtro a `"TODOS"`.

2. **Integración del Detalle de Incidencias (`IncidentDetailModal`):**
   - En cada fila se incorpora el botón accesible "Ver detalle", que despliega `IncidentDetailModal` pasando la incidencia seleccionada.
   - `IncidentDetailModal` se actualizó para renderizar `Última Actualización`, `Dirección` y `Distrito` además de los metadatos existentes.

3. **Enrutamiento y Navegación Global:**
   - Registro de la ruta protegida en `frontend/src/App.jsx`:
     `<Route path="/my-incidents" element={<ProtectedRoute><MyIncidentsPage /></ProtectedRoute>} />`
   - Incorporación del enlace "Mis Incidencias" en las cabeceras de navegación de `HomePage`, `MapPage` y `OperatorPanelPage`.
   - Botón directo en la tarjeta destacada "Mis Reportes" de `HomePage`.

4. **Utilidades de Fecha y Constantes:**
   - Incorporación de la función `formatDate(dateValue, includeTime = true)` en `frontend/src/utils/dateUtils.js`.
   - Exportación de la constante `INCIDENT_STATUSES` en `frontend/src/components/Map/mapConstants.js`.

5. **Pruebas Automatizadas Unitarias y de Integración con Vitest:**
   - Creación de `frontend/src/pages/MyIncidentsPage.test.jsx` con 8 casos de prueba cubriendo:
     - Renderizado de listado con datos, direcciones, distritos y badges.
     - Navegación y deshabilitación de bordes en paginación.
     - Filtrado por estado con reset de página.
     - Estado vacío general con enlace a `/map`.
     - Estado vacío por filtro con botón para limpiar selección.
     - Apertura y cierre del modal `IncidentDetailModal` al pulsar "Ver detalle".
     - Manejo de error con botón "Reintentar".
     - Cierre de sesión desde la cabecera.
   - Ampliación de `frontend/src/utils/dateUtils.test.js` para validar `formatDate`.
   - Resultado: **6 archivos de prueba, 43 tests ejecutados, 100% verde (0 fallos), 0 warnings en linter (`npm run lint`)**.

---

## 2. Archivos Modificados / Creados

- `frontend/src/pages/MyIncidentsPage.jsx`: Vista principal de seguimiento ciudadano de incidencias reportadas.
- `frontend/src/pages/MyIncidentsPage.css`: Hoja de estilos responsive para la vista de seguimiento, badges y tabla.
- `frontend/src/pages/MyIncidentsPage.test.jsx`: Batería de pruebas unitarias para `MyIncidentsPage`.
- `frontend/src/App.jsx`: Registro de la ruta `/my-incidents` con `ProtectedRoute`.
- `frontend/src/components/Map/IncidentDetailModal.jsx`: Inclusión de fechas clave (`updatedAt`), dirección y distrito.
- `frontend/src/components/Map/mapConstants.js`: Exportación del catálogo `INCIDENT_STATUSES`.
- `frontend/src/pages/HomePage.jsx` y `HomePage.css`: Enlace "Mis Incidencias" en cabecera y botón en tarjeta "Mis Reportes".
- `frontend/src/pages/MapPage.jsx` y `MapPage.css`: Enlace "Mis Incidencias" en cabecera superior del mapa.
- `frontend/src/pages/OperatorPanelPage.jsx` y `OperatorPanelPage.css`: Enlace "Mis Incidencias" en cabecera de operador.
- `frontend/src/utils/dateUtils.js` y `dateUtils.test.js`: Función `formatDate` con tests asociados.
- `features/004-reporte-y-seguimiento-de-incidencias/tasks.md`: Marcado de la Tarea 6 como completada `[x]`.
- `docs/004-reporte-y-seguimiento-de-incidencias/06-tarea.md`: Documentación técnica de la tarea.

# Tarea 7: Vista `/map`, Enrutamiento y Navegación en Frontend

**Feature:** `003-mapa-incidencias-y-auditoria-transversal`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-1, RF-2, RF-3, RF-6, RF-7, RF-8, RF-9, RF-11, RF-18, RF-19  

---

## 1. Resumen de la Tarea

En esta tarea se integró la experiencia cartográfica completa en la aplicación cliente web de UrbanPulse, creando la vista de página `/map`, registrando su ruta protegida en el enrutador central y desplegando enlaces y accesos destacados de navegación desde el portal ciudadano y el panel operativo:

1. **Página de Mapa Cartográfico (`MapPage.jsx` y `MapPage.css`):**
   - Vista a pantalla completa adaptada a escritorio y dispositivos móviles (`min-height: 600px`, layout flex vertical con márgenes y bordes normalizados).
   - Cabecera superior fija con controles y resumen:
     - Título descriptivo ("Mapa de Incidencias Urbanas") con badge geográfico ("Málaga").
     - Enlace de retorno contextual hacia el Portal Ciudadano (`/`).
     - Chip contador reactivo que computa y muestra el número de incidencias activas visibles (`incidents.filter(i => i.status !== 'CLOSED' && i.status !== 'REJECTED').length`).
     - Botón toggle de creación asistida (`RF-6` y `RF-7`):
       - Estado inactivo: estilo de acción primaria (`#aa3bff`) con etiqueta "➕ Crear incidencia".
       - Estado activo: estilo destacado de cancelación en rojo (`#ef4444`) con halo animado de pulsación (`pulseSelection`), icono "✖" y texto "Cancelar selección".
     - Botón de refresco manual (`🔄 Actualizar`) con animación de giro (`spin`) durante la recarga asíncrona.
     - Acceso directo condicional al "Panel de Operador" para usuarios autorizados con roles `OPERATOR` o `ADMIN`.
   - Gestión integral de estados:
     - `incidents`: carga inicial asíncrona con `incidentService.getAllIncidents()` con manejo de desmontaje limpio.
     - `loading` y `refreshing`: indicador de carga con spinner accesible centrado (`.map-loading-overlay`).
     - `error`: banner de alerta semántica con botón de reintento interactivo.
     - `isSelectingLocation`: estado booleano sincronizado con el botón toggle y el componente `IncidentsMap`.
     - `selectedLocation`: captura de coordenadas `{ latitude, longitude }` mediante clic sobre el mapa.
     - `isCreateModalOpen`: apertura asistida de `CreateIncidentModal` al pulsar sobre el lienzo en modo selección.
     - `selectedIncident`: apertura de `IncidentDetailModal` al accionar "Ver detalle completo" desde el popup de cualquier marcador.
     - `toastMessage`: banner flotante accesible (`role="status"`, `aria-live="polite"`) que confirma la creación exitosa e incorporación inmediata del nuevo aviso al mapa.

2. **Flujos Integrados de Alta y Detalle:**
   - **Alta asistida (`RF-8`, `RF-9`, `RF-11`):**
     - El usuario pulsa "Crear incidencia" &rarr; el mapa activa la escucha de clics (`useMapEvents`) y muestra el banner flotante indicativo.
     - Al hacer clic sobre el mapa &rarr; se captura la ubicación, se desactiva el modo de selección y se abre `CreateIncidentModal` con las coordenadas fijadas.
     - Si el usuario cancela o cierra el modal &rarr; se descartan las coordenadas y se limpia el estado sin dejar residuos (`RF-9`).
     - Si el usuario completa el registro exitosamente (`onCreated`) &rarr; la incidencia retornada por el servidor (HTTP 201) se antepone inmediatamente a la lista `incidents` (`RF-11`), actualizando los marcadores cartográficos sin necesidad de recargar la página.
   - **Inspección de detalle y auditoría (`RF-2`, `RF-3`, `RF-18`):**
     - Al pulsar "Ver detalle completo" desde un marcador &rarr; se abre `IncidentDetailModal` pasando la incidencia y el usuario autenticado (`authService.getUser()`), desplegando de forma condicional el componente `StatusTimeline` si el usuario dispone del rol `ADMIN`.

3. **Enrutamiento y Control de Acceso (`App.jsx`):**
   - Registro de la ruta protegida `/map` envuelta con `<ProtectedRoute>` para requerir autenticación JWT activa.

4. **Navegación Intuitiva en el Cliente (`HomePage.jsx`, `OperatorPanelPage.jsx`):**
   - Enlace directo "Ver Mapa de Incidencias" en la cabecera del portal ciudadano.
   - Tarjeta destacada interactiva ("Mapa de Incidencias Urbanas") en la sección de funcionalidades del dashboard ciudadano.
   - Enlace de acceso directo hacia `/map` en la barra superior de acciones del panel de operador (`OperatorPanelPage.jsx`).

5. **Calidad y Estilo:**
   - Documentación exhaustiva con JSDoc en todos los componentes creados y actualizados.
   - Estricto cumplimiento de ESLint con 0 errores y 0 advertencias.
   - Compilación completa de producción en Vite (`npm run build`).

---

## 2. Subtareas Implementadas

### 2.1 Vista `MapPage.jsx` y Hoja de Estilos `MapPage.css` (Subtarea 7.1)
- **`frontend/src/pages/MapPage.jsx`:**
  - Componente funcional con hooks `useState`, `useEffect`, `useCallback` y `useMemo`.
  - Orquestación desacoplada de `IncidentsMap`, `CreateIncidentModal` e `IncidentDetailModal`.
  - Cabecera con contador de incidencias no terminadas, toggle de captura, refresco manual y navegación.
  - Alerta de error con reintento y banner toast de confirmación tras creación de incidencias.
- **`frontend/src/pages/MapPage.css`:**
  - Layout flexbox adaptado a pantalla completa y dispositivos móviles.
  - Estilos específicos para `.map-btn-toggle-create.mode-active` con animación `pulseSelection`.
  - Estilos de feedback con `.map-toast-banner` y soporte para modo oscuro (`@media (prefers-color-scheme: dark)`).

### 2.2 Enrutamiento Protegido en `App.jsx` (Subtarea 7.2)
- Importación de `MapPage` y registro de la ruta `/map` dentro de `<ProtectedRoute>`.
- Redirección automática al `/login` si un usuario no autenticado intenta acceder directamente a `/map`.

### 2.3 Enlaces de Navegación en `HomePage.jsx` y `OperatorPanelPage.jsx` (Subtarea 7.3)
- Incorporación de botón/enlace destacado "Ver Mapa de Incidencias" en la cabecera de `HomePage.jsx`.
- Nueva tarjeta destacada "Mapa de Incidencias Urbanas" en el catálogo de servicios ciudadanos (`.features-preview`).
- Enlace al mapa desde la barra de herramientas del panel operativo en `OperatorPanelPage.jsx`.

### 2.4 Verificación de Linter y Compilación (Subtarea 7.4)
- Ejecución de `npm run lint` en `frontend/`: 0 errores y 0 advertencias.
- Ejecución de `npm run build` en `frontend/`: compilación correcta sin fallos.
- Ejecución de `mvn -f Backend/pom.xml test`: 166 pruebas unitarias y de integración pasando al 100%.

---

## 3. Archivos Modificados y Creados

| Archivo | Tipo de Cambio | Descripción |
|---------|----------------|-------------|
| `frontend/src/pages/MapPage.jsx` | Creado | Vista principal de la página del mapa interactivo con cabecera de mandos, orquestación de mapa y modales. |
| `frontend/src/pages/MapPage.css` | Creado | Hoja de estilos de la página del mapa con soporte responsivo y modo oscuro. |
| `frontend/src/App.jsx` | Modificado | Registro de la ruta protegida `/map` con `<ProtectedRoute><MapPage /></ProtectedRoute>`. |
| `frontend/src/pages/HomePage.jsx` | Modificado | Enlace en cabecera y tarjeta destacada con llamada a la acción para acceder a `/map`. |
| `frontend/src/pages/HomePage.css` | Modificado | Estilos para enlaces de navegación hacia el mapa y tarjeta de funcionalidad cartográfica. |
| `frontend/src/pages/OperatorPanelPage.jsx` | Modificado | Enlace directo "Ver Mapa de Incidencias" en la barra de acciones superior. |
| `frontend/src/pages/OperatorPanelPage.css` | Modificado | Estilos para el botón de navegación hacia el mapa en el panel de operador. |
| `features/003-mapa-incidencias-y-auditoria-transversal/tasks.md` | Modificado | Marcada la Tarea 7 y subtareas 7.1 a 7.4 como completadas `[x]`. |
| `docs/003-mapa-incidencias-y-auditoria-transversal/07-tarea.md` | Creado | Documentación técnica detallada de la Tarea 7. |

---

## 4. Validación y Resultados de Pruebas

### 4.1 Linter de Frontend (`npm run lint`)

```bash
npm --prefix frontend run lint
```

**Resultado:**
```
> frontend@0.0.0 lint
> eslint .

Exit Code: 0 (0 errores, 0 advertencias)
```

### 4.2 Compilación de Producción de Frontend (`npm run build`)

```bash
npm --prefix frontend run build
```

**Resultado:**
```
> frontend@0.0.0 build
> vite build

vite v8.3.3 building client environment for production...
transforming...
✓ 148 modules transformed.
rendering chunks...
computing gzip size...
dist/index.html                   0.47 kB │ gzip:   0.30 kB
dist/assets/index-BwOooeHY.css   49.75 kB │ gzip:  12.67 kB
dist/assets/index-EiMCCNmE.js   511.29 kB │ gzip: 161.83 kB
✓ built in 391ms

Exit Code: 0
```

### 4.3 Pruebas de Backend (`mvn test`)

```bash
mvn -f Backend/pom.xml --batch-mode test
```

**Resultado:**
```
[INFO] Results:
[INFO] 
[INFO] Tests run: 166, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  19.367 s
```

---

## 5. Siguiente Paso

- **Tarea 8:** Verificación Integral de Criterios de Aceptación y Pruebas (`features/003-mapa-incidencias-y-auditoria-transversal/tasks.md`).

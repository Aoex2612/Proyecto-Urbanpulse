# Tarea 6: Modal de Detalle de Incidencia y Componente Modular `Timeline` de Auditoría

**Feature:** `003-mapa-incidencias-y-auditoria-transversal`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-2, RF-3, RF-15, RF-18, RF-19, RF-20  

---

## 1. Resumen de la Tarea

En esta tarea se desarrollaron los componentes front-end para la inspección detallada de incidencias y la visualización de su trazabilidad histórica de auditoría en UrbanPulse:

1. **Componente Modular `StatusTimeline` (`StatusTimeline.jsx`, `StatusTimeline.css`, `index.js`):**
   - Diseñado como un componente desacoplado y reutilizable, aplicable en modales, paneles de operador o vistas de detalle completas.
   - Recibe las propiedades `history` (array de transiciones históricas), `loading` (indicador booleano de consulta asíncrona) y `error` (mensaje textual de error opcional).
   - Renderiza una línea temporal vertical estilizada con conector degradado y nodos circulares (`.timeline-marker`).
   - Muestra cada transición histórica con:
     - Transición visual de estado (`fromStatus` &rarr; `toStatus`) mediante badges con colores semánticos normalizados.
     - Marca temporal localizada y formateada en español (`dd/MM/yyyy, HH:mm`).
     - Usuario actor responsable del cambio (`changedByUserName` o `changedByUserEmail`).
     - Motivo o justificación documentada de la transición.
   - Manejo de estados de carga (`spinner` accesible), error con alerta semántica y estado vacío informativo indicando que la incidencia permanece en su estado original (`REPORTED`) sin transiciones posteriores.

2. **Modal de Detalle Exhaustivo (`IncidentDetailModal.jsx`):**
   - Componente modal accesible (`role="dialog"`, `aria-modal="true"`, `aria-labelledby`) que recibe `incident`, `isOpen`, `onClose` y `currentUser`.
   - Muestra la ficha completa de la incidencia seleccionada:
     - Identificador numérico e insignia contextual.
     - Título del problema y badges agrupados de estado (`status`), prioridad (`priority`: Baja, Media, Alta, Crítica) y categoría de catálogo.
     - Rejilla estructurada de metadatos: fecha de creación, ciudadano emisor y coordenadas geoespaciales formateadas a 6 decimales.
     - Sección de descripción detallada con preservación de formato de texto.
   - Accesibilidad completa: navegación mediante teclado, cierre accesible con la tecla `Escape`, descarte al hacer clic en el backdrop exterior y botón de cierre ("Cerrar") con autofoco.

3. **Integración Condicional de Auditoría por Roles (RBAC Frontend):**
   - Evaluación del rol del usuario en sesión (`currentUser?.role` o fallback a `authService.getUser()`).
   - Control de visualización mediante la constante extensible `CAN_VIEW_AUDIT_ROLES = ['ADMIN']` y la función helper `canViewAuditHistory(role)`.
   - Si el usuario cuenta con el rol autorizado (`ADMIN`): ejecuta la consulta asíncrona a `incidentService.getIncidentHistory(incident.id)` al abrir el modal y renderiza la sección de auditoría integrando el componente `StatusTimeline`.
   - Si el usuario no tiene privilegios de auditoría (`CITIZEN`, `OPERATOR`, `TECHNICIAN`, `ANALYST`): no realiza ninguna petición HTTP al endpoint protegido (evitando errores 403 innecesarios) y omite por completo la sección de auditoría del árbol DOM.
   - Estructura abierta que permite habilitar el rol `OPERATOR` en futuras iteraciones simplemente añadiéndolo al array `CAN_VIEW_AUDIT_ROLES` sin alterar la lógica interna ni los componentes visuales.

4. **Utilidades Compartidas y Estilos:**
   - Creación de `frontend/src/utils/dateUtils.js` con la función `formatDateTime` para estandarizar el formateo de fechas en español con la API nativa `Intl.DateTimeFormat`.
   - Incorporación de estilos en `Map.css` y `StatusTimeline.css` para badges de prioridad, rejillas de datos y compatibilidad nativa con modo oscuro (`prefers-color-scheme: dark`).

---

## 2. Subtareas Implementadas

### 2.1 Componente Modular `StatusTimeline` (Subtarea 6.1)
- **`frontend/src/components/Timeline/StatusTimeline.jsx`:**
  - Componente de presentación pura desacoplado del contexto cartográfico.
  - Renderizado de lista ordenada `<ol>` con accesibilidad semántica.
  - Formateo de fecha y hora localizado en español.
  - Tratamiento diferenciado para estado de carga (`loading`), mensaje de error (`error`) y caso límite de historial vacío.
- **`frontend/src/components/Timeline/StatusTimeline.css`:**
  - Estructura vertical con línea conectora continua mediante gradiente CSS.
  - Nodos circulares con efecto hover y sombras suaves.
  - Tarjetas de hito con separación de cabecera y cuerpo descriptivo.
  - Adaptación completa a modo oscuro.
- **`frontend/src/components/Timeline/index.js`:**
  - Exportación del componente barril.

### 2.2 Modal de Detalle `IncidentDetailModal` (Subtarea 6.2)
- **`frontend/src/components/Map/IncidentDetailModal.jsx`:**
  - Modal accesible con `role="dialog"` y etiquetas ARIA.
  - Escucha global de eventos de teclado para descartar el modal al presionar `Escape`.
  - Descarte seguro al hacer clic sobre el fondo oscurecido (`modal-backdrop`).
  - Despliegue de título, categoría, badges de estado y prioridad, descripción completa, coordenadas numéricas y fecha de creación.
  - Botón de cierre en el pie del modal y botón "X" en la cabecera con autofoco gestionado.

### 2.3 Integración Condicional de Auditoría (Subtarea 6.3)
- **`frontend/src/components/Map/mapConstants.js`:**
  - Constante `CAN_VIEW_AUDIT_ROLES = ['ADMIN']`.
  - Helper `canViewAuditHistory(role)`.
  - Mapeo `getPriorityLabel(priority)` para etiquetas legibles en español (Baja, Media, Alta, Crítica).
- **Integración en `IncidentDetailModal.jsx`:**
  - Comprobación de permisos antes de iniciar la solicitud asíncrona.
  - Consumo del servicio `incidentService.getIncidentHistory(incident.id)` con cancelación limpia ante desmontaje de componente.
  - Omisión estricta del componente y llamadas para perfiles no autorizados.

### 2.4 Exportaciones Limpias (Subtarea 6.4)
- Actualización de `frontend/src/components/Map/index.js` y `frontend/src/components/Timeline/index.js` exportando todos los componentes y utilidades públicas.
- JSDoc completo en todos los ficheros con `@param`, `@returns` y descripción técnica detallada.

---

## 3. Archivos Modificados y Creados

| Archivo | Tipo de Cambio | Descripción |
|---------|----------------|-------------|
| `frontend/src/utils/dateUtils.js` | Creado | Función utilitaria `formatDateTime` para localización de fechas en español con `Intl.DateTimeFormat`. |
| `frontend/src/components/Timeline/StatusTimeline.css` | Creado | Hoja de estilos de la línea temporal de auditoría con soporte claro/oscuro y diseño responsivo. |
| `frontend/src/components/Timeline/StatusTimeline.jsx` | Creado | Componente modular desacoplado para representar el historial cronológico de cambios de estado. |
| `frontend/src/components/Timeline/index.js` | Creado | Archivo barril con exportación de `StatusTimeline`. |
| `frontend/src/components/Map/mapConstants.js` | Modificado | Añadida constante `CAN_VIEW_AUDIT_ROLES`, función `canViewAuditHistory` y función `getPriorityLabel`. |
| `frontend/src/components/Map/IncidentDetailModal.jsx` | Creado | Componente modal accesible de detalle con renderizado condicional de la línea temporal de auditoría. |
| `frontend/src/components/Map/Map.css` | Modificado | Añadidos estilos para badges de prioridad, ficha de metadatos del modal de detalle y adaptación a tema oscuro. |
| `frontend/src/components/Map/index.js` | Modificado | Exportación de `IncidentDetailModal`, `getPriorityLabel`, `CAN_VIEW_AUDIT_ROLES` y `canViewAuditHistory`. |
| `features/003-mapa-incidencias-y-auditoria-transversal/tasks.md` | Modificado | Marcada la Tarea 6 y subtareas 6.1 a 6.3 como completadas `[x]`. |
| `docs/003-mapa-incidencias-y-auditoria-transversal/06-tarea.md` | Creado | Documentación técnica exhaustiva de la Tarea 6. |

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

### 4.2 Compilación de Frontend (`npm run build`)

```bash
npm --prefix frontend run build
```

**Resultado:**
```
> frontend@0.0.0 build
> vite build

vite v8.3.3 building client environment for production...
transforming...
✓ 91 modules transformed.
rendering chunks...
computing gzip size...
dist/index.html                   0.47 kB │ gzip:   0.30 kB
dist/assets/index-BCxKg8OO.css   27.50 kB │ gzip:   9.07 kB
dist/assets/index-C_7UoZs9.js   328.37 kB │ gzip: 105.49 kB
✓ built in 404ms

Exit Code: 0
```

### 4.3 Pruebas de Backend (`mvn test`)

```bash
mvn --batch-mode test
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
```

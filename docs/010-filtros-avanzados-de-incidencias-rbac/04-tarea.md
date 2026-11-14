# 010 — Filtros avanzados RBAC — Tarea 4: cliente API y `IncidentFilterBar`

## Resumen global

Tarea 4 (solo frontend). Se amplió el cliente de incidencias con la búsqueda multicriterio y la barra de filtros con distrito (público), prioridad y departamento (roles internos), respetando el RBAC en la capa de presentación. No se tocaron `MapPage.jsx` ni `OperatorPanelPage.jsx` (Tarea 5).

Contrato de filtros emitido por `onFilterChange`:

```js
{ text, category, startDate, endDate, status, district, priority, department }
```

Todos los campos son cadenas; `department` vale `''` (todos), `'unassigned'` (sin asignar) o el id público del departamento.

Ficheros modificados:

- `frontend/src/services/incidentService.js`
- `frontend/src/services/incidentService.test.js`
- `frontend/src/components/Filters/IncidentFilterBar.jsx`
- `frontend/src/components/Filters/IncidentFilterBar.css`
- `frontend/src/components/Filters/IncidentFilterBar.test.jsx`

## Subtareas

### 1. Cliente HTTP `incidentService.js`

Se añadió `searchIncidents(params = {})`, que hace `api.get('/api/v2/incidents/search', { params })` y devuelve `response.data`. Se documentó con JSDoc (`text`, `category`, `start`, `end`, `status`, `district`, `priority`, `departmentId`, `unassignedDepartment`) y se incluyó en el objeto exportado por defecto.

Decisión: no se filtran ni renombran parámetros en el cliente. Axios omite `undefined`; los valores vacíos los gestiona el consumidor (Tarea 5) al construir el payload de búsqueda. Así el contrato con backend queda explícito y sin lógica duplicada.

Tests: se añadieron dos casos en `incidentService.test.js` (propagación completa de parámetros y llamada sin argumentos vía export default).

### 2. Prop `departments` en `IncidentFilterBar`

Se añadió `departments = []` a las props. El componente normaliza con `Array.isArray(departments) ? departments : []`, por lo que `null`, `undefined` o un valor no-array no rompen el render. Cada opción usa `dept.id ?? dept.publicId` como valor y clave, y `dept.name` como etiqueta.

### 3. Selector de distrito (público)

Visible para todos los roles. `<label htmlFor="filter-district">Distrito:</label>`, opción vacía "Todos los distritos" y el resto desde `MALAGA_DISTRICTS` (`src/utils/analyticsConstants.js`). `aria-label="Distrito"` explícito.

### 4. Selectores internos (prioridad y departamento)

Guarda única `canFilterInternal = Boolean(userRole) && userRole !== 'CITIZEN'`: los anónimos (sin rol) se comportan como `CITIZEN`.

- Prioridad: id `filter-priority`, etiqueta "Prioridad:", vacía "Todas las prioridades" y `LOW/MEDIUM/HIGH/CRITICAL` con `getPriorityLabel` de `mapConstants` ("Baja", "Media", "Alta", "Crítica"). `aria-label="Prioridad"`.
- Departamento: id `filter-department`, etiqueta "Departamento:", vacía "Todos los departamentos", `value="unassigned"` "Sin departamento asignado" y catálogo dinámico. `aria-label="Departamento"`.

El selector de estado existente pasó a usar la misma guarda `canFilterInternal`, corrigiendo el comportamiento previo (`userRole !== 'CITIZEN'` mostraba estado a anónimos).

### 5. `handleClear`

Resetea los ocho campos simultáneamente (incluidos `district`, `priority` y `department`) y emite el objeto completo en `onFilterChange`.

### 6. CSS responsivo

Se mantuvo el layout flex existente. Cambios mínimos: `min-width: 0` en `.filter-group`, `min-width/max-width` en `.filter-select` y media query a 640 px que apila los grupos en columna y anula el `margin-left: auto` del botón.

### 7. Tests de `IncidentFilterBar`

`IncidentFilterBar.test.jsx` reescrito con 12 casos: visibilidad por rol (`CITIZEN` y anónimo sin estado/prioridad/departamento; `OPERATOR` con todos), etiquetas de prioridad, resiliencia con `departments` vacío/`undefined`, opciones dinámicas de departamento, eventos de distrito/prioridad/departamento, limpieza completa con payload exacto y aviso RF-14 con `hasResults={false}`.

### 8. Validación

Verificación de RF-1, RF-2, RF-3, RF-5, RF-7, RF-10, RF-13 y RF-14 en la capa de presentación, y RNF-3/RNF-4 (responsivo y accesibilidad). RF-1/2/4/6/8/9/11/12 se completan en backend y en la integración de la Tarea 5.

## Problemas y solución

- **Test de "Limpiar filtros" desactualizado**: el payload ganó claves y la comparación estricta fallaba. Se actualizó al objeto completo de ocho campos.
- **Colisión de nombres accesibles en tests**: la categoría "Parques y Jardines" coincide con un departamento de prueba; `getByRole('option', { name })` era ambiguo. Se acotó la búsqueda con `within(departmentSelect)`.
- **Anónimos veían el filtro de estado**: se sustituyó la condición antigua por `canFilterInternal`, que exige rol y descarta `CITIZEN`.

## Tests ejecutados

```
cd frontend && npm test -- --run src/components/Filters/IncidentFilterBar.test.jsx src/services/incidentService.test.js
# Test Files 2 passed, Tests 21 passed

cd frontend && npm test -- --run
# Test Files 35 passed, Tests 279 passed

cd frontend && npm run lint
# sin errores
```

## Decisiones

- Un único objeto de estado local con las ocho claves; se mantienen los nombres previos `text`, `category`, `startDate`, `endDate`, `status`.
- La traducción del filtro `department` (`'' | 'unassigned' | id`) a `departmentId`/`unassignedDepartment` del API queda en la Tarea 5, donde se conecta la barra con `searchIncidents`.
- Sin ADR: no se introduce ninguna decisión arquitectónica nueva respecto a `plan.md`.
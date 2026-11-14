# 010 — Filtros avanzados RBAC — Tarea 5: integracion en `MapPage` y `OperatorPanelPage`

## Resumen global

Tarea 5 (solo frontend). Se conectaron las dos vistas consumidoras con el cliente de busqueda
multicriterio de la Tarea 4 y se cargo el catalogo de departamentos de forma no bloqueante.
No se modifico backend, ni `IncidentFilterBar`, ni `incidentService.js`.

Cambios funcionales:

- **Catalogo de departamentos**: ambas paginas piden `triageService.getDepartments()` solo si el
  rol es interno (rol definido y distinto de `CITIZEN`). Si la peticion falla se registra con
  `console.error` y el estado queda en `[]`; la pagina y la barra siguen renderizando.
- **Busqueda multicriterio (RF-12)**: `useEffect` con debounce de 300 ms que llama a
  `incidentService.searchIncidents(...)` cuando hay al menos un criterio de servidor activo
  (`text`, `category`, `status`, `district`, `priority`, `department`). Cancela el timer y
  descarta respuestas obsoletas con la bandera `cancelled`.
- **Fallback local**: el filtrado en cliente se mantiene siempre y parte de
  `searchResults ?? incidents`. Incluye los nuevos criterios de distrito, prioridad y
  departamento, y sirve tambien como red de seguridad si la busqueda falla (se vuelve al
  conjunto completo).
- **Fechas**: el rango de fechas sigue resolviendose en cliente y no se envia al endpoint. En
  `MapPage`, `filterIncidentsByDate` se aplica sobre la base (resultado de servidor o listado
  completo) antes de los criterios locales, sin desincronizaciones.
- **RF-14**: `hasResults` de la barra y el aviso de sin resultados usan la lista final filtrada.
- **RF-15**: los criterios viven en el estado de la pagina; abrir/cerrar modales no los resetea.

Ficheros modificados:

- `frontend/src/pages/MapPage.jsx`
- `frontend/src/pages/OperatorPanelPage.jsx`
- `frontend/src/pages/OperatorPanelPage.test.jsx`
- `frontend/src/pages/MapPage.test.jsx` (nuevo)

## Subtareas

### 1. Carga del catalogo de departamentos en ambas paginas

En el `useEffect` de montaje existente se anade la llamada a `triageService.getDepartments()`
cuando `canFilterInternal` es cierto. La guarda es `Boolean(user?.role) && user.role !== 'CITIZEN'`,
por lo que anonimos y ciudadanos no invocan el endpoint (requiere autenticacion). El resultado
se guarda en `departments` (`Array.isArray` como salvaguarda) y se pasa a
`<IncidentFilterBar departments={departments} ... />`. Cualquier rechazo se registra con
`console.error` sin `setError` ni bloqueo del render. La bandera `isMounted` existente evita
actualizar estado tras desmontar.

### 2. Busqueda multicriterio con debounce y descarte de respuestas obsoletas

Se anadio `toSearchParams(criteria)` a nivel de modulo en cada pagina:

```js
{
  text, category, status, district, priority,      // undefined si vacio
  departmentId,                                    // UUID o undefined
  unassignedDepartment                             // true solo para 'unassigned'
}
```

El efecto observa `filterCriteria` y `hasServerCriteria`. Con criterios activos programa un
`setTimeout` de 300 ms; el cleanup cancela el timer y marca `cancelled = true`. La respuesta
solo se aplica si la peticion sigue vigente. `searchResults` guarda un array o `null`.

Decision: cuando no hay criterios de servidor **no** se hace `setSearchResults(null)` de forma
sincrona dentro del efecto (la regla `react-hooks/set-state-in-effect` lo prohibe). En su lugar
la base de filtrado se deriva en render: `(hasServerCriteria ? searchResults : null) ?? incidents`.
El comportamiento observable es el mismo: al limpiar los filtros se vuelve al conjunto completo
y se ignora cualquier resultado previo.

En caso de error: `console.error` y `searchResults = null`, de modo que la vista continua con el
listado completo y el filtrado local (no se vacia ni se bloquea).

### 3. Filtrado local de distrito, prioridad y departamento

La base es `searchResults ?? incidents` (en `MapPage` despues del filtro temporal). Se aplica:

- `district`: `(incident.district || '').trim().toLowerCase()` comparado con el criterio
  normalizado. Un distrito nulo o vacio nunca coincide (C-1).
- `priority`: igualdad estricta con `incident.priority`.
- `department`: se usan `activeAssignments` (v2) o `assignments` como fallback, descartando
  `isActive === false`. `'unassigned'` exige cero asignaciones activas; un UUID exige alguna
  asignacion activa con `String(a.departmentId) === String(department)`.

El filtro es idempotente sobre resultados del servidor: al volver a aplicar los mismos criterios
no cambia el conjunto.

### 4. Convivencia con el filtro temporal y RF-14

`MapPage` mantiene su `MapDateFilter` y `filterIncidentsByDate` sin cambios. La lista final
(`filteredIncidents`) alimenta el mapa, el contador y `hasResults`. `OperatorPanelPage` usa
`displayedIncidents` para la tabla y `hasResults={displayedIncidents.length > 0}`. Con cero
resultados, `IncidentFilterBar` muestra el aviso de RF-14 sin errores.

### 5. Conservacion de estado ante modales (RF-15)

No se toco la logica de modales ni de triaje. Como los criterios son estado de la pagina y la
barra es un componente controlado por el padre, abrir/cerrar modales no los reinicia. Se anadio
cobertura explicita en ambas suites.

### 6. Pruebas

`OperatorPanelPage.test.jsx`:

- Se anadio `searchIncidents` al mock de `incidentService` (por defecto devuelve el listado) y
  `getDepartments` se reinicia a `[]` en `beforeEach`.
- Casos nuevos: (a) roles internos ven los tres selectores y el catalogo cargado; (b) la
  seleccion de distrito, prioridad y departamento invoca `searchIncidents` con `departmentId`
  y, al cambiar a "Sin departamento", con `unassignedDepartment: true`; (c) abrir y cerrar el
  modal de comentarios conserva el distrito; (d) `searchIncidents` vacio muestra el aviso RF-14.

`MapPage.test.jsx` (nuevo): mockea `../components/Map` (mapa con boton que invoca
`onSelectIncident`, modales stubs), `incidentService`, `triageService` y `authService`. Las
fixtures usan `createdAt` reciente para no ser descartadas por el preset semanal. Casos:
(a) `CITIZEN` solo ve distrito y no llama a `getDepartments`; (b) `OPERATOR` ve todos los
selectores y el catalogo; (c) distrito + "Sin departamento asignado" llama a `searchIncidents`
con los parametros esperados tras el debounce; (d) sin resultados aparece el aviso RF-14;
(e) abrir/cerrar el modal de detalle conserva el distrito.

### 7. Verificacion

```bash
cd frontend
npm test -- --run src/pages/OperatorPanelPage.test.jsx src/pages/MapPage.test.jsx
npm test -- --run
npm run lint
```

Resultado: 23/23 tests en las dos suites objetivo, 288/288 en la suite completa y lint sin
errores. (Vitest imprime un `AggregateError` de XHR de jsdom sin impacto en el resultado; no
proviene de estas paginas y no hace fallar ningun test.)

## Decisiones

- **Servidor + fallback local**: la busqueda en servidor aporta el filtrado multicriterio
  completo y acotado (incluido departamento activo) con debounce para no saturar la API; el
  filtrado local actua como red de seguridad ante errores y como segunda pasada idempotente.
  Asi un fallo de red no deja la vista vacia ni bloqueada (C-3) y el usuario conserva una
  respuesta coherente con los criterios visibles.
- **Fechas solo en cliente**: se evita enviar `start`/`end` para no duplicar logica ni alterar
  el comportamiento preexistente del mapa y del panel.
- **Sin helpers compartidos nuevos**: `toSearchParams` es una funcion pura de ~10 lineas y se
  duplica en ambas paginas para no introducir un modulo/abstraccion nueva, tal como pide la
  tarea. La comparacion de distrito/departamento se mantiene inline en cada `useMemo`.
- **Carga de departamentos condicionada al rol**: evita peticiones autenticadas innecesarias
  para `CITIZEN`/anonimos y mantiene el RBAC de presentacion.

## Problemas encontrados y solucion

- La regla `react-hooks/set-state-in-effect` (eslint-plugin-react-hooks v7) prohibe el
  `setSearchResults(null)` sincrono al quedarse sin criterios. Se sustituyo por la base derivada
  en render descrita en la subtarea 2, manteniendo el contrato funcional.
- En los tests de `MapPage`, las incidencias sin `district` desaparecian al seleccionar distrito
  (correcto por C-1) y ocultaban los botones necesarios para los casos de modal. Se usaron
  fixtures con `district: 'Centro'` y `createdAt` reciente.

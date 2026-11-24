# Tarea 3: Corrección de resultados obsoletos y estado de carga en Frontend durante debounce de filtros

**Fecha:** 2026-11-24  
**Rama:** `fix/91-regresiones-filtros-riesgos-carga`  
**Commit:** `b73416f` (`fix(frontend): evitar resultados obsoletos durante debounce y mejorar feedback visual`)  
**Requisitos cubiertos:** RF-FILTROS-UX

---

## 1. Contexto y Diagnóstico

En la implementación previa del filtrado multicriterio en `MapPage.jsx` y `OperatorPanelPage.jsx`:
- Cuando el usuario modificaba un filtro de búsqueda resuelto en servidor (texto, categoría, estado, distrito, prioridad, departamento), existía un debounce de 300 ms antes de invocar `incidentService.searchIncidents(...)`.
- Durante ese intervalo de 300 ms y mientras la promesa de red estaba en vuelo, los datos en pantalla correspondían a la búsqueda anterior sin indicar visualmente al usuario que una nueva búsqueda estaba en proceso.
- En caso de respuestas de red fuera de orden (race conditions si una petición lenta terminaba después de una más reciente), existía el riesgo de sobrescribir el estado con resultados desfasados.

## 2. Cambios Implementados

### 2.1. `frontend/src/components/Filters/IncidentFilterBar.jsx` y `.css`
- Se añadió la prop opcional `isLoading` (por defecto `false`).
- Cuando `isLoading` está activo, se muestra un indicador visual accesible con spinner y texto "Buscando incidencias..." (`.filter-loading-indicator` con `role="status"` y `aria-live="polite"`).
- Se evita mostrar el mensaje de "No se encontraron incidencias..." mientras `isLoading` es `true`.
- Estilos responsivos y accesibles con animación `@keyframes filter-spin`.

### 2.2. `frontend/src/pages/MapPage.jsx` y `frontend/src/pages/OperatorPanelPage.jsx`
- Se introdujo un identificador incremental de petición mediante `useRef` (`activeSearchRequestIdRef`) para descartar respuestas fuera de orden.
- Se implementó `handleFilterChange`:
  - Al cambiar cualquier criterio de servidor, se activa inmediatamente `isSearching(true)` y se limpia `searchResults(null)` para no mezclar datos desfasados con el nuevo criterio.
  - Al limpiar criterios de servidor, se restablece `isSearching(false)` y `searchResults(null)`.
- El efecto de debounce (`useEffect`) genera un nuevo `requestId = ++activeSearchRequestIdRef.current`. Al resolver o fallar la promesa de `searchIncidents`, se comprueba si `activeSearchRequestIdRef.current === requestId`. Si no coincide (respuesta obsoleta/desfasada), se descarta de forma segura.
- Se conecta `isLoading={isSearching}` a `IncidentFilterBar`.

### 2.3. Pruebas Unitarias y de Integración
- **`frontend/src/pages/MapPage.test.jsx`**:
  - Test añadido: `muestra estado visual de carga durante el debounce/búsqueda y no aplica resultados desfasados`.
- **`frontend/src/pages/OperatorPanelPage.test.jsx`**:
  - Test añadido: `muestra estado visual de carga durante el debounce/búsqueda y descarta respuestas desfasadas`.
- Ambos tests validan:
  1. Que `Buscando incidencias...` aparece inmediatamente al cambiar el criterio.
  2. Que múltiples cambios en el debounce y peticiones pendientes que se resuelven en orden inverso no sobrescriben los datos vigentes con los de peticiones antiguas.

## 3. Verificación de Calidad

- **Tests de frontend:** `npm test -- --run` ejecutado con éxito (37 suites, 305 tests pasados al 100%).
- **Linter de frontend:** `npm run lint` (`eslint .`) ejecutado con 0 errores y 0 warnings.

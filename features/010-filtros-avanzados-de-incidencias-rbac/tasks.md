# 010 — Filtros avanzados de incidencias con control de acceso RBAC — Tareas

_Checklist accionable derivada del `plan.md`. Tareas principales y subtareas pequeñas y concretas; marca `[x]` al completarlas._

## [ ] Tarea 1: DTO de criterios de filtrado y predicados en Criteria API — Depende de: Ninguna — RF: RF-1, RF-2, RF-4, RF-8, RF-9, RF-12; RNF: RNF-1, RNF-2
- [ ] Crear el DTO inmutable/record `IncidentFilterCriteria` en `org.bulbaseros.dto` con los campos de búsqueda (texto, categoría, fechas, estado, prioridad, distrito, id de departamento, flag sin departamento e indicador de inclusión de filtros internos).
- [ ] Implementar en `IncidentSpecifications` los métodos estáticos:
  - `hasDistrict(String district)` con comparación exacta insensible a mayúsculas (`LOWER(TRIM(...))`).
  - `hasPriority(PrioridadIncidencia priority)`.
  - `hasDepartment(Long departmentId)` mediante subconsulta `EXISTS` sobre `Tareas` con `isActive = true`.
  - `hasNoDepartment()` mediante subconsulta `NOT EXISTS` sobre `Tareas` con `isActive = true`.
- [ ] Añadir método sobrecargado `build(IncidentFilterCriteria criteria)` en `IncidentSpecifications` manteniendo la compatibilidad con el método `build(...)` previo.
- [ ] Crear pruebas unitarias en `IncidentSpecificationsTest`:
  - Verificar no-regresión de filtros previos (texto, categoría, fechas, estado).
  - Comprobar caso límite C-1: incidencias con `district` nulo o vacío se excluyen al filtrar por un distrito concreto y se incluyen con "Todos los distritos".
  - Comprobar predicados de prioridad (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
  - Comprobar departamento asignado activo (`isActive = true`), departamento inactivo (`isActive = false`) y sin departamento.
  - Comprobar combinaciones multicriterio con conjunción lógica `AND`.
- [ ] Validar contra los criterios de aceptación de `spec.md`.

## [ ] Tarea 2: Adaptación de la capa de servicio y control de acceso RBAC en backend — Depende de: Tarea 1 — RF: RF-6, RF-11, RF-12; RNF: RNF-1
- [ ] Actualizar `IncidentService.java` para construir `IncidentFilterCriteria` recibiendo los nuevos parámetros y aplicando la guarda de rol (desactivar filtros internos para usuarios `CITIZEN` o no autenticados).
- [ ] Actualizar los métodos de consulta y paginación en `IncidentService` para consumir la especificación construida.
- [ ] Crear pruebas unitarias en `IncidentServiceTest` verificando que cuando el usuario autenticado tiene rol `CITIZEN` o es anónimo, los filtros de prioridad, departamento y sin departamento son ignorados silenciosamente.
- [ ] Validar contra los criterios de aceptación de `spec.md`.

## [ ] Tarea 3: Endpoints de controladores REST (API v1 y API v2) — Depende de: Tarea 2 — RF: RF-1, RF-3, RF-6, RF-7, RF-8, RF-9, RF-11; RNF: RNF-1, RNF-2
- [ ] Extender `IncidentController.java` (API v1) con los `@RequestParam` opcionales: `district`, `priority`, `departmentId` y `unassignedDepartment`, delegando a `IncidentService`.
- [ ] Extender `IncidentControllerV2.java` (API v2) con los `@RequestParam` opcionales: `district`, `priority`, `departmentId` (UUID público mapeado a departamento) y `unassignedDepartment`.
- [ ] Añadir pruebas de integración en `IncidentControllerTest` y `IncidentControllerV2Test` con MockMvc:
  - Usuario `OPERATOR`: filtros de prioridad, departamento y distrito aplicados correctamente.
  - Usuario `CITIZEN` o anónimo: petición con parámetros internos responde HTTP 200 ignorando silenciosamente dichos filtros.
  - Caso límite C-4: consulta con `departmentId` inexistente devuelve lista vacía con HTTP 200 sin lanzar excepciones no controladas.
- [ ] Validar contra los criterios de aceptación de `spec.md`.

## [x] Tarea 4: Extensión del cliente API y componente `IncidentFilterBar` con estilos — Depende de: Ninguna — RF: RF-1, RF-2, RF-3, RF-5, RF-7, RF-10, RF-13, RF-14; RNF: RNF-3, RNF-4
- [x] Actualizar el cliente HTTP `incidentService.js` en frontend para propagar los nuevos parámetros de búsqueda (`district`, `priority`, `departmentId`, `unassignedDepartment`) en las llamadas a `/api/incidents`.
- [x] Actualizar `IncidentFilterBar.jsx` para admitir la prop `departments` (con array vacío por defecto).
- [x] Añadir el selector desplegable de distrito accesible para todos los roles, nutrido con `MALAGA_DISTRICTS` y la opción "Todos los distritos".
- [x] Añadir condicionalmente (`canFilterInternal = userRole && userRole !== 'CITIZEN'`):
  - Selector de prioridad (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) con etiquetas legibles.
  - Selector de departamento con opciones: "Todos los departamentos", "Sin departamento asignado" (`unassigned`) y la lista de departamentos disponibles.
- [x] Actualizar la función `handleClear` para resetear `district`, `priority` y `department` simultáneamente.
- [x] Actualizar `IncidentFilterBar.css` para asegurar maquetación responsiva y etiquetas semánticas accesibles (`aria-label`).
- [x] Añadir pruebas en `IncidentFilterBar.test.jsx`:
  - Renderizado para `CITIZEN`: solo muestra texto, categoría, fechas y distrito; oculta estado, prioridad y departamento.
  - Renderizado para `OPERATOR`: muestra todos los selectores.
  - Comportamiento resiliente si `departments` está vacío o la petición falla.
  - Emisión correcta de eventos al seleccionar y al limpiar filtros.
  - Comprobación de que la prop `hasResults={false}` renderiza el aviso de búsqueda sin resultados conforme a RF-14.
- [x] Validar contra los criterios de aceptación de `spec.md`.

## [ ] Tarea 5: Integración en vistas consumidoras (`MapPage` y `OperatorPanelPage`) y verificación E2E — Depende de: Tarea 3, Tarea 4 — RF: RF-12, RF-13, RF-14, RF-15; RNF: RNF-2, RNF-3
- [ ] Integrar la carga del catálogo de departamentos en `MapPage.jsx` y `OperatorPanelPage.jsx` de forma no bloqueante y pasarla a `IncidentFilterBar`.
- [ ] Conectar la barra de filtros con el nuevo cliente `incidentService.js` para ejecutar las búsquedas multicriterio.
- [ ] Comprobar que los filtros de fecha preexistentes conviven sin interferencias ni desincronizaciones con los nuevos filtros.
- [ ] Asegurar que el estado de los filtros se mantenga inalterado al interactuar con modales de detalle de incidencia (RF-15).
- [ ] Actualizar y ejecutar las suites de pruebas de `MapPage.test.jsx` y `OperatorPanelPage.test.jsx`.
- [ ] Validar contra todos los criterios de aceptación de `spec.md`.

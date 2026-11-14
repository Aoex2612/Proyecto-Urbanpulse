# 010 — Filtros avanzados de incidencias con control de acceso RBAC — Plan

_Cómo se implementa lo descrito en `spec.md`. Debe respetar la `constitution/`._

## Enfoque

La solución amplía la infraestructura de filtrado multicriterio existente en lugar de introducir abstracciones innecesarias o bibliotecas pesadas:
1. **Backend**:
   - Se encapsulan los criterios de búsqueda en un objeto de filtro `IncidentFilterCriteria` para evitar firmas de métodos con más de 10 parámetros posicionales frágiles y propensos a regresiones.
   - Se extiende la clase utilitaria `IncidentSpecifications` mediante predicados Criteria API para:
     - `district`: comparación exacta insensible a mayúsculas con `LOWER(TRIM(root.district))`.
     - `priority`: comparación directa sobre la columna `priority` (`PrioridadIncidencia`).
     - `departmentId`: subconsulta `EXISTS` sobre la entidad `Tareas` vinculando `tareas.incident = root` y `tareas.department.id = :departmentId` donde `tareas.isActive = true`.
     - `unassignedDepartment`: subconsulta `NOT EXISTS` sobre `Tareas` con `tareas.isActive = true`.
   - En API v1 se admite `departmentId` numérico (`Long`), mientras que en API v2 se admite la resolución por `UUID` (`publicId`) mapeado a su entidad correspondiente.
   - En `IncidentController` y `IncidentService`, se aplica la guarda RBAC de modo que los parámetros `priority`, `departmentId` y `unassignedDepartment` sean omitidos de forma transparente cuando el usuario es `CITIZEN` o anónimo (idéntico al mecanismo actual de `status`).
2. **Frontend**:
   - Se amplía el componente `IncidentFilterBar` para recibir opcionalmente la lista de departamentos como prop (`departments`), con carga asíncrona segura desacoplada en el componente padre o hook correspondiente.
   - Se añade el desplegable público de `distrito` (nutrido de la constante congelada existente `MALAGA_DISTRICTS`).
   - Se añaden bajo la guarda condicional `!isCitizen`:
     - El selector de prioridad (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
     - El selector de departamento (incluyendo la opción explícita "Sin departamento asignado").
   - Se actualiza la función de reseteo (`handleClear`) para restablecer todos los campos simultáneamente y se preserva el estado ante modales.

## Implementación

1. **Objeto de Criterios y Especificaciones Criteria API**
   - Archivos:
     - `Backend/src/main/java/org/bulbaseros/dto/IncidentFilterCriteria.java` (nuevo DTO inmutable/record de criterios).
     - `Backend/src/main/java/org/bulbaseros/specification/IncidentSpecifications.java`
   - Métodos nuevos en `IncidentSpecifications`:
     - `hasDistrict(String district)`: predicado `cb.equal(cb.lower(cb.trim(root.get("district"))), district.trim().toLowerCase())`.
     - `hasPriority(PrioridadIncidencia priority)`: predicado directo sobre el campo `priority`.
     - `hasDepartment(Long departmentId)`: subquery `EXISTS` en `Tareas` con `tareas.incident = root.id`, `tareas.department.id = departmentId` y `tareas.isActive = true`.
     - `hasNoDepartment()`: subquery `NOT EXISTS` en `Tareas` donde `tareas.incident = root.id` y `tareas.isActive = true`.
   - Actualización de `build(IncidentFilterCriteria criteria)` manteniendo sobrecarga retrocompatible con la firma anterior para garantizar compatibilidad con tests existentes.

2. **Capa de Servicio `IncidentService.java`**
   - Archivo: `Backend/src/main/java/org/bulbaseros/service/IncidentService.java`
   - Métodos actualizados: `searchIncidents(...)` y métodos auxiliares de consulta.
   - Pasa los criterios a `IncidentSpecifications.build(...)`, aplicando `includeInternalFilters = false` si el usuario no tiene rol `OPERATOR`, `TECHNICIAN`, `ADMIN` o `ANALYST`.

3. **Capa de Controladores `IncidentController.java` y `IncidentControllerV2.java`**
   - Archivos:
     - `Backend/src/main/java/org/bulbaseros/controller/IncidentController.java` (acepta `departmentId` numérico).
     - `Backend/src/main/java/org/bulbaseros/controller/v2/IncidentControllerV2.java` (acepta `departmentId` como UUID / `publicId`).
   - Parámetros `@RequestParam(required = false)` añadidos: `district`, `priority`, `departmentId`, `unassignedDepartment`.
   - Lógica RBAC: Determinar si el usuario autenticado tiene rol no-`CITIZEN`. Si es `CITIZEN` o visitante anónimo, `includeInternalFilters = false` (ignorando `priority`, `departmentId` y `status`).

4. **Ampliación de `IncidentFilterBar.jsx` y su diseño `IncidentFilterBar.css`**
   - Archivos:
     - `frontend/src/components/Filters/IncidentFilterBar.jsx`
     - `frontend/src/components/Filters/IncidentFilterBar.css`
   - Acepta prop `departments = []` (lista opcional cargada de forma no bloqueante).
   - Renderizado del selector de distrito usando `MALAGA_DISTRICTS` (público).
   - Renderizado de selectores de prioridad y departamento bajo `canFilterInternal` (`userRole !== 'CITIZEN' && userRole !== undefined`).
   - El selector de departamento incluye opción `""` ("Todos los departamentos"), `"unassigned"` ("Sin departamento asignado") y las opciones dinámicas de `departments`.
   - Actualización de `handleClear` para restablecer los estados de `district`, `priority` y `department`.

5. **Ajuste en Páginas Consumidoras (`MapPage.jsx` y `OperatorPanelPage.jsx`)**
   - Archivos:
     - `frontend/src/pages/MapPage.jsx`
     - `frontend/src/pages/OperatorPanelPage.jsx`
   - Carga inicial del catálogo de departamentos para suministrarlo a `IncidentFilterBar` sin bloquear la renderización inicial del mapa/tabla si la petición de catálogo tarda o falla.
   - Asegurar que la propagación del objeto de filtros incluya los nuevos campos y mantenga el estado durante la apertura/cierre de modales.

## Decisiones

- **Encapsulación de criterios en `IncidentFilterCriteria`** — Evita firmas con más de 10 parámetros posicionales en Java, reduciendo a cero el riesgo de errores de inversión de argumentos y facilitando futuras extensiones.
- **Condición estricta de asignación activa (`isActive = true`) en `Tareas`** — Cumple con el modelo de datos de la feature 005, asegurando que asignaciones históricas o canceladas no computen como departamento asignado actual.
- **Soporte de UUID en API v2 y Long en API v1** — Mantiene coherencia con la arquitectura de la feature 010 sin romper compatibilidad con clientes existentes de la v1.
- **Subquery Criteria API con `EXISTS` indexado** — Se utiliza una subconsulta correlacionada en lugar de un `JOIN` para evitar duplicación de filas en paginación y optimizar el plan de ejecución en PostgreSQL mediante los índices existentes en `incident_id` y `department_id`.
- **Reutilización de `MALAGA_DISTRICTS` existente en frontend** — Se reutiliza la constante congelada ya definida en `src/utils/analyticsConstants.js`.

## Riesgos

- **Degradación de rendimiento en consultas con subquery de departamento** — Mitigación: `Tareas` cuenta con índices en `incident_id` y `department_id`, y la condición `isActive = true` se resuelve de forma indexada mediante `cb.exists(...)`.
- **Fallo en la carga del catálogo de departamentos en frontend** — Mitigación: El componente `IncidentFilterBar` maneja `departments` como array con valor por defecto vacío `[]`. Si la llamada al catálogo falla, la interfaz muestra "Todos los departamentos" y "Sin departamento asignado", registrando el error sin romper la barra de filtros.

## Estrategia de tests

- **Backend (Unitarios & Integración)**:
  - `IncidentSpecificationsTest`:
    - Tests de no-regresión: verificación de que los filtros previos (texto, categoría, fechas, estado) funcionan de forma idéntica a como lo hacían antes.
    - Test de filtrado por distrito: coincidencia exacta insensible a mayúsculas y comportamiento ante distritos nulos/vacíos.
    - Test de filtrado por prioridad: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.
    - Test de filtrado por departamento: incidencia con tarea `isActive = true`, incidencia con tarea `isActive = false`, e incidencia con opción `unassignedDepartment = true`.
    - Test de combinación multicriterio: conjunción lógica `AND` de todos los criterios.
  - `IncidentControllerTest` y `IncidentControllerV2Test`:
    - Petición con rol `OPERATOR` aplicando `priority` y `departmentId` -> los criterios se aplican en la consulta.
    - Petición con rol `CITIZEN` o sin token autenticado enviando `priority` y `departmentId` -> los parámetros se ignoran silenciosamente y el filtro no se aplica.
- **Frontend (Vitest + React Testing Library)**:
  - `IncidentFilterBar.test.jsx`:
    - Renderizado con `userRole="CITIZEN"`: solo muestra texto, categoría, fechas y distrito; oculta estado, prioridad y departamento.
    - Renderizado con `userRole="OPERATOR"`: muestra todos los selectores.
    - Renderizado resiliente cuando la prop `departments` es vacía o nula.
    - Cambio de valor en selectores de distrito, prioridad y departamento emite el evento `onFilterChange` con el payload correcto.
    - Pulsar "Limpiar filtros" restablece todos los valores a vacío y emite el evento de reset.

# 010 — Filtros avanzados RBAC — Tarea 1: DTO de criterios y predicados Criteria API

## Resumen global

Se creó el DTO inmutable `IncidentFilterCriteria` y se amplió `IncidentSpecifications` con los predicados de distrito, prioridad y departamento (activo / sin departamento), más una sobrecarga `build(IncidentFilterCriteria)` que aplica los filtros internos solo cuando el criterio lo permite. La firma antigua de `build(...)` se conserva delegando en la nueva, por lo que `IncidentService` sigue compilando y comportándose igual.

Ficheros modificados:

- `Backend/src/main/java/org/bulbaseros/dto/IncidentFilterCriteria.java` (nuevo)
- `Backend/src/main/java/org/bulbaseros/specification/IncidentSpecifications.java`
- `Backend/src/test/java/org/bulbaseros/specification/IncidentSpecificationsTest.java`

Contrato para las tareas 2 y 3: el record declara, en este orden, `text, category, start, end, status, district, priority, departmentId, unassignedDepartment, includeInternalFilters`.

## Subtareas

### 1. DTO `IncidentFilterCriteria`

Record Java con Javadoc de clase y `@param` por componente. Sin lógica de negocio ni validaciones: la guarda RBAC se resuelve con `includeInternalFilters`, que la capa de servicio (Tarea 2) calcula a partir del rol.

### 2. Predicados en `IncidentSpecifications`

- `hasDistrict(String)`: `null`/blanco devuelve `null`; si no, `equal(lower(trim(district)), valor.trim().toLowerCase())`. Así `"  CENTRO  "` coincide con `"Centro"`.
- `hasPriority(PrioridadIncidencia)`: `null` devuelve `null`; si no, igualdad sobre `priority`.
- `hasDepartment(Long)`: subconsulta correlacionada `EXISTS` sobre `Tareas` con `incident = root`, `department.id = departmentId` e `isActive = true`. Sin `JOIN`, para no duplicar filas en paginación.
- `hasNoDepartment()`: subconsulta `NOT EXISTS` sobre `Tareas` con `incident = root` e `isActive = true`.

### 3. `build(IncidentFilterCriteria)` y retrocompatibilidad

Aplica siempre texto, categoría, fechas y distrito; estado, prioridad, departamento y sin-departamento solo si `includeInternalFilters` es `true`. Usa `Specification.where(null)` como base y `andIfNotNull`, igual que el código previo. La sobrecarga antigua de seis parámetros construye un `IncidentFilterCriteria` con distrito/prioridad/departamento nulos, `unassignedDepartment = false` e `includeInternalFilters = includeStatus`, manteniendo el comportamiento exacto. El único uso en producción (`IncidentService.searchIncidents`) no requiere cambios.

### 4. Tests

`IncidentSpecificationsTest` pasó de pruebas de "no es null" a `@DataJpaTest` con `TestEntityManager` e `IncidenciaRepository.findAll(spec)`, siguiendo el patrón de `TareasRepositoryTest`. 14 casos:

- No regresión: texto (`farola`), categoría insensible a mayúsculas, rango de fechas y estado.
- C-1: distrito nulo y distrito vacío se excluyen con `hasDistrict("Centro")` y aparecen sin filtro de distrito.
- Distrito insensible a mayúsculas y espacios exteriores (`"centro"` y `"  CENTRO  "`).
- Prioridad `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.
- Departamento: tarea activa cuenta; tarea inactiva no; incidencia con dos departamentos aparece por ambos (C-2); `hasNoDepartment()` devuelve las incidencias sin tareas activas.
- Combinación `AND` de distrito + prioridad + departamento.
- `includeInternalFilters = false` ignora estado, prioridad, departamento y sin-departamento; con `true` los aplica.
- Null-safety de `hasDistrict`, `hasPriority`, `hasDepartment` y sobrecarga antigua de `build`.

## Decisiones de diseño

- **`unassignedDepartment` como flag booleano independiente** de `departmentId`, tal y como define el plan; si ambos llegaran informados, el `AND` produce una intersección vacía sin excepción.
- **`NOT EXISTS` correlacionado** en lugar de `LEFT JOIN ... IS NULL`, coherente con el `EXISTS` de departamento y sin riesgo de duplicados.
- **Tarea inactiva no cuenta como asignación**: una incidencia con única tarea `isActive = false` cae en "sin departamento", que es lo que exige RF-9.
- **Sin ADR**: todas las decisiones están previstas en `plan.md` (subconsultas, DTO, retrocompatibilidad).

## Problemas y solución

- **`@CreationTimestamp` sobrescribe `createdAt` en el insert**: fijar la fecha antes de persistir no servía para el test de rango temporal. Se ajusta tras el insert con un `UPDATE incidents SET created_at = ...` nativo en el helper del test; el predicado se evalúa en SQL, por lo que el valor del contexto de persistencia no interfiere.
- **La incidencia con la tarea inactiva aparecía en `hasNoDepartment()`**: era el comportamiento correcto según RF-9, no un fallo del predicado; se ajustó la expectativa del test y se documentó el caso.

## Comandos de test ejecutados

```bash
cd Backend && mvn -q -Dtest=IncidentSpecificationsTest test
# Tests run: 14, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS

cd Backend && mvn test
# Tests run: 711, Failures: 0, Errors: 0, Skipped: 1 — BUILD SUCCESS
```

## Requisitos cubiertos

RF-1, RF-2, RF-4, RF-8, RF-9 y RF-12 en la capa de especificaciones; RNF-1 queda preparado vía `includeInternalFilters` (la resolución del rol se implementa en la Tarea 2). La validación formal de criterios de aceptación corresponde a la fase de validación posterior.
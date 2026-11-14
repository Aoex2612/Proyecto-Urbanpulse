# 010 — Filtros avanzados RBAC — Tarea 2: servicio y guarda RBAC

## Resumen global

`IncidentService` construye ahora `IncidentFilterCriteria` y aplica la guarda de rol en un único punto (RNF-1). Se añadieron dos sobrecargas públicas de búsqueda que reciben distrito, prioridad, departamento y el email del usuario autenticado; los métodos antiguos de seis argumentos quedan intactos para no romper los controladores actuales (Tarea 3 los migrará).

Ficheros modificados:

- `Backend/src/main/java/org/bulbaseros/service/IncidentService.java`
- `Backend/src/test/java/org/bulbaseros/service/IncidentServiceTest.java`

Métodos nuevos:

- `searchIncidents(text, category, start, end, status, district, priority, departmentId, unassignedDepartment, userEmail)` → `List<IncidentResponse>`.
- `searchIncidentsV2(..., departmentPublicId (UUID), unassignedDepartment, userEmail)` → `List<IncidentResponseV2>`.
- `hasInternalAccess(String userEmail)` (package-private): resuelve el rol.
- `buildFilterCriteria(...)` (package-private, 10 parámetros con email): aplica la guarda y devuelve el record.

## Subtareas

### 1. Construcción de `IncidentFilterCriteria` con guarda de rol

Roles internos: `OPERATOR`, `TECHNICIAN`, `ADMIN`, `ANALYST` (constante `INTERNAL_ROLES`).

- `hasInternalAccess`: email `null`/blanco → `false` (anónimo = sin acceso). Usuario no encontrado → `true`, que es el comportamiento previo del controlador (`Optional.empty()` se trataba como interno). Resto: pertenencia a `INTERNAL_ROLES`.
- `buildFilterCriteria`: si no hay acceso interno, `status`, `priority` y `departmentId` pasan a `null`, `unassignedDepartment` a `false` e `includeInternalFilters` a `false`; texto, categoría, fechas y distrito se conservan siempre. El email se normaliza a minúsculas antes de `findByEmail`, como en el resto del servicio.
- Existe una sobrecarga privada de `buildFilterCriteria` con `boolean internalAccess` para que `searchIncidentsV2` no consulte el usuario dos veces (una para resolver el UUID y otra para la guarda).

### 2. Consulta y paginación

Ambos métodos son `@Transactional(readOnly = true)`, llaman a `incidenciaRepository.findAll(IncidentSpecifications.build(criteria))` y mapean con `incidentMapper` / `incidentMapperV2`. No se tocó la paginación propia de `getMyIncidents` porque esta feature filtra sobre el listado de búsqueda, no sobre el panel personal.

### 3. Resolución de UUID de departamento en v2 (C-4)

Se añadió `DepartamentoRepository` como dependencia `@Autowired(required = false)` al constructor principal (y `null` en el constructor de conveniencia de seis argumentos). En `searchIncidentsV2`, solo con acceso interno y UUID no nulo:

- `findByPublicId` existente → se usa `getId()` como `departmentId`.
- inexistente (o repositorio no disponible) → `List.of()` sin consultar incidencias y sin excepción, HTTP 200 en el controlador.
- sin acceso interno el UUID ni siquiera se resuelve (el repositorio no se invoca).

### 4. Tests

8 pruebas nuevas en `IncidentServiceTest` (35 → 43):

- `buildFilterCriteria` con `CITIZEN`: ignora estado, prioridad, departamento y sin-departamento; conserva texto y distrito.
- Email `null`/blanco: `hasInternalAccess` falso y filtros internos anulados, sin tocar `UserRepository`.
- `OPERATOR`, `TECHNICIAN`, `ADMIN` y `ANALYST`: conservan los cuatro filtros internos (bucle sobre roles).
- Usuario inexistente: `hasInternalAccess` verdadero y prioridad/departamento conservados.
- `searchIncidents` nuevo: delega en `findAll(any(Specification.class))` y devuelve los DTOs del `IncidentMapper` real (spy).
- `searchIncidentsV2` con UUID existente: resuelve `findByPublicId` y devuelve los DTOs v2.
- `searchIncidentsV2` con UUID inexistente e interno: lista vacía y `verify(never()).findAll(...)`.
- `searchIncidentsV2` con UUID inexistente y `CITIZEN`: ignora el UUID (`never()` sobre `findByPublicId`) y consulta incidencias.

## Decisiones de diseño

- **Guarda centralizada en el servicio**: los controladores de la Tarea 3 solo pasarán `authentication.getName()`; no se duplica el cálculo de rol en otras capas.
- **Usuario inexistente = interno**: se replica el comportamiento del controlador previo para no endurecer el acceso por un fallo de datos y no alterar la suite existente.
- **UUID inexistente devuelve lista vacía en el servicio**: evita una excepción no controlada en el controlador y cumple C-4 sin lógica extra.
- **Sobrecarga privada con `boolean internalAccess`**: mantiene una sola consulta de usuario por petición v2.
- **Sin ADR**: todas las decisiones están previstas en `plan.md` (resolución UUID en v2, guarda RBAC en servicio, lista vacía para identificadores inexistentes).

## Problemas y solución

- **`@InjectMocks` y el nuevo constructor**: al añadir el parámetro de `DepartamentoRepository` al constructor principal, Mockito lo inyecta como `null` si no hay mock declarado. Se añadió `@Mock DepartamentoRepository` en el test para que la resolución del UUID sea verificable.
- **Maven no está en el `PATH`** del entorno: se ejecutó el wrapper local `~/.m2/wrapper/dists/apache-maven-3.8.4-bin/*/bin/mvn` con el mismo objetivo; no se modificó el proyecto.

## Comandos de test ejecutados

```bash
cd Backend && mvn -q -Dtest=IncidentServiceTest test
# Tests run: 43, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS

cd Backend && mvn -q test
# Tests run: 719, Failures: 0, Errors: 0, Skipped: 1 — BUILD SUCCESS
```

## Requisitos cubiertos

RF-6, RF-11 y RF-12 en la capa de servicio, y RNF-1 (RBAC tanto en UI como en backend). C-4 queda resuelto en v2. La validación formal de criterios de aceptación a nivel de API/UI corresponde a las tareas y fase posteriores.
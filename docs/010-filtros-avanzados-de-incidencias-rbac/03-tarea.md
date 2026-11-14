# 010 — Filtros avanzados RBAC — Tarea 3: endpoints REST v1 y v2

## Resumen global

Ambos controladores de búsqueda aceptan ya los filtros `district`, `priority`, `departmentId` y `unassignedDepartment` como `@RequestParam` opcionales y delegan en las sobrecargas nuevas de `IncidentService` (Tarea 2). Toda la lógica de rol y la resolución UUID→id permanece en el servicio: los controladores solo extraen `authentication.getName()` y pasan los parámetros tal cual.

Ficheros modificados:

- `Backend/src/main/java/org/bulbaseros/controller/IncidentController.java`
- `Backend/src/main/java/org/bulbaseros/controller/v2/IncidentControllerV2.java`
- `Backend/src/test/java/org/bulbaseros/controller/IncidentControllerTest.java`
- `Backend/src/test/java/org/bulbaseros/controller/v2/IncidentControllerV2Test.java`

No se tocaron el servicio, las especificaciones, los endpoints `GET /api/incidents` (`getActiveIncidents`), `my-incidents` ni el frontend. Los constructores y el campo `userRepository` de ambos controladores se mantienen intactos para no romper la superficie usada por los tests.

## Subtareas

### 1. API v1 — `IncidentController`

`GET /api/incidents/search` y `GET /api/v1/incidents/search` (mismo mapeo):

- `district` (`String`), `priority` (`PrioridadIncidencia`), `departmentId` (`Long`) y `unassignedDepartment` (`boolean`, `defaultValue = "false"`), todos `required = false`.
- Spring devuelve 400 automáticamente si `priority` no es un valor del enum.
- Se eliminó del controlador el cálculo de `includeStatus`/consulta de rol; ahora delega en `searchIncidents(text, category, startDate, endDate, status, district, priority, departmentId, unassignedDepartment, userEmail)`.
- `userEmail = auth != null ? auth.getName() : null`.
- Javadoc actualizado: distrito público; estado, prioridad, departamento y "sin departamento" ignorados silenciosamente para CITIZEN/anónimo en el servicio (RNF-1).

### 2. API v2 — `IncidentControllerV2`

`GET /api/v2/incidents/search`:

- Mismos parámetros, con `departmentId` como `UUID` (UUID público); Spring devuelve 400 si el formato es inválido.
- Se conserva el parseo manual de `status` (`statusStr` → `EstadoIncidencia`) con 400 ante valor inválido.
- Se eliminó el bloque de rol con `UserRepository`; delega en `searchIncidentsV2(..., departmentId, unassignedDepartment, userEmail)` con `authentication != null ? authentication.getName() : null`.
- La resolución UUID→id y C-4 (UUID inexistente ⇒ lista vacía HTTP 200) viven en el servicio.
- Javadoc actualizado con el mismo alcance RBAC y la nota de C-4.

### 3. Tests de integración MockMvc

`IncidentControllerTest` (26 tests):

- Se reescribieron los dos tests de `search` que verificaban la firma antigua de 6 argumentos y `eq(true/false)` (ya no compilaban).
- OPERATOR: `text=farola&category=Alumbrado&priority=HIGH&district=Centro&departmentId=10` → 200 y `verify` de la firma de 10 argumentos con `unassignedDepartment=false` y el email del operador.
- CITIZEN con los mismos parámetros internos → 200 (stub del servicio) y verificación de que se le pasa el email del ciudadano; la guarda que ignora los filtros está cubierta en `IncidentServiceTest` y no se duplica.
- C-4: `departmentId=9999` con OPERATOR y stub `List.of()` → 200 con `[]` (sin 500).
- Caso anónimo no añadido: `SecurityConfig` exige `authenticated()` para `/api/incidents/**`, por lo que una petición sin token responde 401 antes de llegar al controlador. El caso de email nulo está cubierto en el test del servicio.

`IncidentControllerV2Test` (10 tests):

- OPERATOR: `priority=CRITICAL&district=Centro&departmentId=<uuid>&unassignedDepartment=false` → 200 y `verify` de `searchIncidentsV2(...)` con el UUID y el email del operador.
- CITIZEN con parámetros internos → 200 y email del ciudadano pasado al servicio.
- C-4: UUID aleatorio inexistente con stub `List.of()` → 200 y `[]`.
- `status` inválido sigue devolviendo 400.

## Decisiones de diseño

- **Controladores finos**: la guarda RBAC y la resolución UUID→id no se duplican; el controlador solo propaga parámetros y el email autenticado.
- **`unassignedDepartment` con `defaultValue = "false"`**: evita `null` en el primitivo y fija el valor por defecto pedido por la spec.
- **Enums como tipo de `@RequestParam`**: `PrioridadIncidencia` y `EstadoIncidencia` en v1 delegan la validación en Spring (400 automático), sin parseo manual.
- **Parseo manual de `status` en v2**: se mantiene el comportamiento previo (texto + 400) para no alterar el contrato existente de la v2.
- **Sin ADR**: todas las decisiones estaban previstas en `plan.md`.

## Problemas y solución

- **Tests obsoletos**: las verificaciones con la firma de 6 argumentos y los matchers `eq(true/false)` de `includeStatus` rompían la compilación; se sustituyeron por la firma de 10 argumentos y verificaciones sobre `unassignedDepartment`.
- **Caso anónimo en MockMvc**: no es viable sin cambiar `SecurityConfig` (fuera de alcance); se documentó y se dejó cubierto en la capa de servicio.
- **Maven no está en el `PATH`** del entorno: se usó el wrapper local `~/.m2/wrapper/dists/apache-maven-3.8.4-bin/*/bin/mvn` en modo offline, sin modificar el proyecto.

## Comandos de test ejecutados

```bash
cd Backend && mvn -q -Dtest=IncidentControllerTest,IncidentControllerV2Test test
# IncidentControllerTest: 26 tests, 0 fallos
# IncidentControllerV2Test: 10 tests, 0 fallos

cd Backend && mvn -q test
# Tests run: 724, Failures: 0, Errors: 0, Skipped: 1 — BUILD SUCCESS
```

## Requisitos cubiertos

RF-1, RF-3, RF-6, RF-7, RF-8, RF-9 y RF-11 en la capa de controladores; RNF-1 (RBAC silencioso para CITIZEN/anónimo) y RNF-2. La validación formal de criterios de aceptación a nivel de API/UI corresponde a la tarea de validación y a la fase posterior.

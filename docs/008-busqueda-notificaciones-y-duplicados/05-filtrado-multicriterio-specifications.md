# Búsqueda y Filtrado Multicriterio con JPA Criteria API (HU19)

## Resumen del Componente
Este módulo implementa el motor de búsqueda multicriterio desacoplado y dinámico basado en JPA Criteria API (`IncidentSpecifications`) y `JpaSpecificationExecutor` en `IncidenciaRepository`.

## Criterios Soportados
1. **Texto Libre (`hasTextSearch`):** Búsqueda insensible a mayúsculas/minúsculas sobre `title` o `description`.
2. **Categoría (`hasCategory`):** Coincidencia exacta insensible a mayúsculas sobre la categoría temática.
3. **Rango de Fechas (`hasCreatedAfter` / `hasCreatedBefore`):** Filtrado por fecha de creación en formato ISO 8601.
4. **Estado (`hasStatus`):** Filtrado por estado del ciclo de vida (`EstadoIncidencia`).

## Control de Acceso Basado en Roles (RBAC)
- Los perfiles ciudadanos (`CITIZEN`) no pueden filtrar por estado operativo; el parámetro `status` es omitido de la especificación generada cuando el solicitante es ciudadano (`includeStatus = false`).
- Los perfiles internos (`OPERATOR`, `TECHNICIAN`, `ADMIN`, `ANALYST`) tienen habilitado el filtro de estado completo.

## Endpoint REST
- `GET /api/incidents/search`
- Parámetros opcionales: `text`, `category`, `startDate`, `endDate`, `status`.
- Retorna `List<IncidentResponse>`.

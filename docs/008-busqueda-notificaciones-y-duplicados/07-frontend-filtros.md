# Componente Frontend IncidentFilterBar con RBAC (HU19)

## Resumen del Componente
Barra reutilizable de filtrado multicriterio (`IncidentFilterBar.jsx`) con control de visibilidad por rol (RBAC), integrable en vistas de visualización cartográfica (`MapPage.jsx`) y de triaje (`OperatorPanelPage.jsx`).

## Reglas de Control de Acceso (RBAC)
- **Ciudadanos (`userRole === 'CITIZEN'`):** Acceso libre a búsqueda por texto libre, selector de categoría temática y selectores de fecha inicio/fin. El selector de estado (`status`) se omite por completo del DOM.
- **Perfiles Internos (`OPERATOR`, `TECHNICIAN`, `ADMIN`, `ANALYST`):** Selector de estado visible con el catálogo integral de estados de ciclo de vida (`INCIDENT_STATUSES`).

## Comportamiento
- Al cambiar cualquier campo se emite el objeto unificado de criterios a `onFilterChange`.
- El botón "Limpiar filtros" restablece todos los controles y emite los campos reseteados.
- Si no hay coincidencias (`hasResults === false`), muestra aviso informativo contextual.

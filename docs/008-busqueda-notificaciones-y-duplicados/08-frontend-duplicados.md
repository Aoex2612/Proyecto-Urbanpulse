# Visualización y Gestión de Sugerencias de Duplicados en Panel Operativo (HU20)

## Resumen del Componente
Integración visual en `OperatorPanelPage.jsx` para la detección, señalización y confirmación operativa de incidencias urbanas potencialmente duplicadas (RF-10, RF-11).

## Funcionalidades y Elementos de Interfaz
1. **Detección Reactiva:** Al cargar el panel o recibir notificaciones, se examinan las notificaciones de tipo `DUPLICATE_SUGGESTION` del operador para identificar las incidencias afectadas.
2. **Badge Visual:** En la celda del título de la incidencia se renderiza el distintivo `⚠ Posible Duplicado` (`.badge-duplicate-suggestion`).
3. **Acción de Confirmación Directa:** Botón contextual `"Confirmar Duplicado"` (`.btn-triage-duplicate`) para incidencias no terminales, que solicita confirmación al operador.
4. **Transición a Rechazo:** Invoca `POST /api/incidents/{id}/mark-duplicate` a través de `triageService.markAsDuplicate(id)`, actualizando inmediatamente el estado de la incidencia en la tabla local a `REJECTED` y mostrando mensaje de confirmación.

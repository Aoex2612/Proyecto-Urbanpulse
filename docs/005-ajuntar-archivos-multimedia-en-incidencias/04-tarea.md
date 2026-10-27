# Tarea 04: Integración en el Cliente Frontend (React)

## Descripción
Integración en la interfaz de usuario para permitir adjuntar imágenes opcionales al crear una incidencia, previsualizarlas antes del envío y visualizarlas en el modal de detalle.

## Cambios realizados
- **`incidentService.js`**:
  - `uploadIncidentAttachment(incidentId, file)`: Petición `POST /api/incidents/{id}/attachments` mediante `FormData` y `Content-Type: multipart/form-data`.
  - `getIncidentAttachments(incidentId)`: Petición `GET /api/incidents/{id}/attachments`.
- **`CreateIncidentModal.jsx`**:
  - Input file opcional con `accept="image/jpeg,image/png"`.
  - Validación en cliente de tipo MIME (JPEG/PNG) y tamaño (máx. 5MB).
  - Previsualización dinámica de la fotografía seleccionada con `URL.createObjectURL` y botón para descartar/eliminar la imagen.
  - Limpieza de memoria con `URL.revokeObjectURL`.
  - Flujo de creación: creación de la incidencia y, si hay imagen seleccionada, envío asíncrono del adjunto.
  - Soporte garantizado para la creación de incidencias con 0 imágenes adjuntas sin errores.
- **`IncidentDetailModal.jsx`**:
  - Consulta asíncrona de fotografías adjuntas y visualización de galería con miniaturas e hipervínculos a resolución completa.
- **`Map.css`**:
  - Estilos responsivos para `.form-input-file`, `.image-preview-card`, `.image-preview-thumbnail`, `.image-preview-remove-btn` y `.detail-attachments-grid`.

## Estado
- **Completada**: Cumple con validaciones de interfaz, accesibilidad y experiencia de usuario fluida.

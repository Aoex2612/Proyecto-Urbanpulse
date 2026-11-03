# Tarea 04: Integración en el Cliente Frontend (React)

**Feature:** `006-adjuntos-multimedia-en-incidencias` (`HU05`)  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-1, RF-2, RF-3, RF-5, RF-7  

---

## 1. Resumen de la Tarea

En esta tarea se implementó la interacción de usuario en el cliente web React para permitir adjuntar imágenes opcionales, previsualizarlas antes del envío y visualizarlas en el detalle de la incidencia:

1. **Cliente de Servicios (`frontend/src/services/incidentService.js`):**
   - `uploadIncidentAttachment(incidentId, file)`: Petición `POST /api/incidents/${incidentId}/attachments` empaquetando el fichero en un objeto `FormData` con cabecera `Content-Type: multipart/form-data`.
   - `getIncidentAttachments(incidentId)`: Petición `GET /api/incidents/${incidentId}/attachments` para consultar los adjuntos de una incidencia.

2. **Formulario de Creación de Incidencias (`CreateIncidentModal.jsx`):**
   - Incorporación de campo selector de archivo opcional (`<input type="file" accept="image/jpeg,image/png" />`).
   - Validación reactiva en cliente:
     - Comprobación de tipo MIME permitido (`image/jpeg` o `image/png`).
     - Comprobación de peso máximo inferior o igual a 5MB.
     - Notificación visual de error ante ficheros no conformes sin bloquear el formulario.
   - Tarjeta de previsualización inmediata mediante `URL.createObjectURL` con botón de descarte/eliminación de la imagen seleccionada.
   - Gestión adecuada de memoria mediante revocación de URLs de objeto (`URL.revokeObjectURL`).
   - Flujo de creación transaccional en cliente: creación del aviso principal y posterior subida del adjunto si el usuario lo seleccionó.
   - Soporte verificado para el alta de incidencias con 0 adjuntos (campo opcional).

3. **Visualización en Modal de Detalle (`IncidentDetailModal.jsx`):**
   - Carga asíncrona de adjuntos de la incidencia al abrir la vista de detalle.
   - Galería fotográfica integrada con cuadrícula responsiva, miniaturas estilizadas y enlaces para apertura en nueva pestaña a tamaño completo.

4. **Estilos y Experiencia Visual (`Map.css`):**
   - Definición de clases específicas para la zona de carga de archivos (`.form-input-file`), tarjeta de previsualización (`.image-preview-card`, `.image-preview-thumbnail`, `.image-preview-remove-btn`) y galería del detalle (`.detail-attachments-grid`, `.detail-attachment-thumb`).

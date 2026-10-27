# Tarea 03: Servicio y Controladores REST para Archivos Multimedia

## Descripción
Implementación de la lógica transaccional de validación, almacenamiento en disco y endpoints REST de carga y consulta de adjuntos multimedia en incidencias.

## Cambios realizados
- **`AttachmentService.java`**:
  - `uploadAttachment(Long incidentId, MultipartFile file)`:
    - Validación de archivo no vacío (HTTP 400).
    - Validación estricta de tipos MIME: solo `image/jpeg` y `image/png` (HTTP 400).
    - Validación de tamaño máximo de 5MB (HTTP 400).
    - Prevención de path traversal (`..`).
    - Verificación de existencia de incidencia (`IncidentNotFoundException` -> HTTP 404).
    - Creación de directorio si no existe y almacenamiento seguro con UUID único.
    - Persistencia en tabla `attachments` mediante `ArchivoAdjuntoRepository`.
    - Retorno mediante `AttachmentMapper.toDto`.
  - `getAttachmentsByIncidentId(Long incidentId)`:
    - Consulta cronológica de adjuntos de una incidencia.
- **`IncidentController.java`**:
  - Sustitución del borrador preliminar.
  - Endpoint `POST /api/incidents/{id}/attachments` (`consumes = MediaType.MULTIPART_FORM_DATA_VALUE`) retornando HTTP 201 Created y `AttachmentResponse`.
  - Endpoint `GET /api/incidents/{id}/attachments` retornando HTTP 200 OK y `List<AttachmentResponse>`.
  - Mantenimiento de constructor retrocompatible `new IncidentController(incidentService)` para tests preexistentes.
- **`GlobalExceptionHandler.java`**:
  - Capturador para `MaxUploadSizeExceededException` retornando HTTP 400 estructurado con `ErrorResponse`.
  - Capturador para `IllegalArgumentException` retornando HTTP 400.

## Estado
- **Completada**: Endpoints operativos, validados y con tratamiento de excepciones centralizado.

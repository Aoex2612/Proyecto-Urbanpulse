# Tarea 03: Servicio y Controladores REST para Archivos Multimedia

**Feature:** `006-adjuntos-multimedia-en-incidencias` (`HU05`)  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-1, RF-2, RF-3, RF-4, RF-5, RF-6, RF-7  

---

## 1. Resumen de la Tarea

En esta tarea se desarrolló la lógica de negocio transaccional para la validación, subida física y recuperación de archivos multimedia, junto con los endpoints REST y control de excepciones:

1. **Servicio Transaccional `AttachmentService` (`org.bulbaseros.service`):**
   - `@Transactional public AttachmentResponse uploadAttachment(Long incidentId, MultipartFile file)`:
     - Validación de fichero no nulo ni vacío (`file.isEmpty()`) -> rechazo con `IllegalArgumentException` (HTTP 400).
     - Validación estricta de Content-Type: solo formatos `image/jpeg` y `image/png` -> rechazo con `IllegalArgumentException` (HTTP 400).
     - Validación de tamaño máximo de 5MB (5,242,880 bytes) -> rechazo con `IllegalArgumentException` (HTTP 400).
     - Prevención de Path Traversal: verificación de secuencias `..` en el nombre original.
     - Comprobación de existencia de la incidencia en base de datos -> arroja `IncidentNotFoundException` (HTTP 404) si no existe.
     - Almacenamiento seguro en disco: generación de nombre físico único mediante prefijo `UUID.randomUUID()` preservando la extensión original saneada.
     - Persistencia de metadatos en base de datos (`ArchivoAdjunto`) y retorno de DTO mediante `attachmentMapper.toDto(...)`.
   - `@Transactional(readOnly = true) public List<AttachmentResponse> getAttachmentsByIncidentId(Long incidentId)`:
     - Verificación de la incidencia y recuperación de la lista cronológica ascendente de adjuntos.

2. **Controlador REST `IncidentController` (`org.bulbaseros.controller`):**
   - `POST /api/incidents/{id}/attachments`:
     - Parámetro multipart `@RequestParam("file") MultipartFile file`.
     - Responde con HTTP 201 Created y cuerpo `AttachmentResponse`.
   - `GET /api/incidents/{id}/attachments`:
     - Responde con HTTP 200 OK y lista `List<AttachmentResponse>`.
   - Compatibilidad conservada para constructores preexistentes usados en pruebas previas.

3. **Manejo Centralizado de Excepciones (`GlobalExceptionHandler`):**
   - `@ExceptionHandler(MaxUploadSizeExceededException.class)` retornando HTTP 400 Bad Request con `ErrorResponse` descriptivo.
   - `@ExceptionHandler(IllegalArgumentException.class)` retornando HTTP 400 Bad Request con mensaje de validación correspondiente.

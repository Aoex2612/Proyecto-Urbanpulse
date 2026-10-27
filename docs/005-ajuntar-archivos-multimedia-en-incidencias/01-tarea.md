# Tarea 01: Capa de Persistencia, DTOs y Mapeador para Adjuntos Multimedia

## Descripción
Creación de los componentes de persistencia y transferencia de datos para la gestión de archivos adjuntos asociados a incidencias urbanas.

## Cambios realizados
- **`ArchivoAdjunto.java`**: Entidad de dominio JPA vinculada a la tabla `attachments`, con campos para `incident_id`, `file_name`, `file_url`, `mime_type`, `file_size_bytes` y `uploaded_at` (con `@PrePersist` y Javadoc completo).
- **`ArchivoAdjuntoRepository.java`**: Repositorio Spring Data JPA con métodos para consultar adjuntos por incidencia (`findByIncidentId`, `findByIncidentIdOrderByUploadedAtAsc`).
- **`AttachmentResponse.java`**: DTO de salida estructurado para exponer los metadatos de los ficheros multimedia persistidos sin comprometer la entidad JPA.
- **`AttachmentMapper.java`**: Mapeador de componente Spring extendiendo `BaseMapper<ArchivoAdjunto, AttachmentResponse>` para conversiones individuales y en listas.
- **Pruebas unitarias**:
  - `AttachmentResponseTest.java`: Validación de constructores, getters, setters y builder.
  - `AttachmentMapperTest.java`: Validación de mapeo entidad a DTO y manejo de listas y nulos.

## Estado
- **Completada**: Cumple con el estándar de arquitectura desacoplada de capas y convención de mappers.

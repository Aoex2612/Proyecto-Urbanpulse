# 006 — Adjuntos Multimedia en Incidencias — Plan

_Cómo se implementa lo descrito en `spec.md`. Respeta estrictamente la `constitution/` y los principios de diseño de UrbanPulse._

## Enfoque

La solución amplía la plataforma UrbanPulse permitiendo asociar evidencias fotográficas a las incidencias reportadas mediante una arquitectura desacoplada de almacenamiento y transferencia:

1. **Capa de Persistencia y Transferencia (Backend):**
   - Entidad JPA `ArchivoAdjunto` mapeada a la tabla `attachments` de PostgreSQL (`id`, `incident_id`, `file_name`, `file_url`, `mime_type`, `file_size_bytes`, `uploaded_at`).
   - Repositorio `ArchivoAdjuntoRepository` para consultas por incidencia.
   - DTO `AttachmentResponse` y componente mapeador `AttachmentMapper` que extiende `BaseMapper`.
2. **Infraestructura de Almacenamiento y Seguridad (Backend):**
   - Propiedades de límites multipart (5MB) y directorio físico configurable (`app.upload.dir`).
   - `WebConfig` implementando `WebMvcConfigurer` para exponer `/uploads/**` mapeado a la carpeta física de disco.
   - `SecurityConfig` autorizando acceso público a `/uploads/**` para renderizado directo en clientes sin token JWT.
3. **Lógica Transaccional y Endpoints REST (Backend):**
   - `AttachmentService` para validar tipos MIME (`image/jpeg`, `image/png`), límites de peso, prevención de Path Traversal y persistencia de ficheros con nombres UUID.
   - Endpoints en `IncidentController`: `POST /api/incidents/{id}/attachments` (HTTP 201) y `GET /api/incidents/{id}/attachments` (HTTP 200).
   - Manejadores de excepción en `GlobalExceptionHandler` para `MaxUploadSizeExceededException` e `IllegalArgumentException`.
4. **Integración en Cliente Web (Frontend):**
   - Funciones `uploadIncidentAttachment` y `getIncidentAttachments` en `incidentService.js`.
   - Modificación de `CreateIncidentModal.jsx` con input file opcional, validación en cliente, previsualización dinámica y subida tras creación.
   - Actualización de `IncidentDetailModal.jsx` con galería fotográfica de miniaturas y enlaces a resolución completa.
   - Estilos responsivos en `Map.css`.
5. **Estrategia de Pruebas Automatizadas:**
   - Pruebas unitarias de DTOs, mappers y servicios (`AttachmentServiceTest`, `AttachmentMapperTest`, `AttachmentResponseTest`).
   - Pruebas de integración con MockMvc (`IncidentAttachmentControllerTest`).
   - Pruebas de componentes React con Vitest (`CreateIncidentModal.test.jsx`).

---

## Implementación

### 1. Backend: Persistencia, DTOs y Mapeadores
- `ArchivoAdjunto.java` con `@ManyToOne` hacia `Incidencia`.
- `ArchivoAdjuntoRepository.java` con métodos de búsqueda por `incidentId`.
- `AttachmentResponse.java` DTO inmutable para respuestas.
- `AttachmentMapper.java` como bean `@Component` extendiendo `BaseMapper`.

### 2. Backend: Configuración y Seguridad
- Mapeo de recursos en `WebConfig.java` (`/uploads/**`).
- Regla `.permitAll()` en `SecurityConfig.java` para `/uploads/**`.
- Parámetros en `application.properties` y `application-dev.properties`.

### 3. Backend: Servicio y Controladores
- `AttachmentService.java`: Métodos `uploadAttachment` y `getAttachmentsByIncidentId`.
- `IncidentController.java`: Endpoints `POST` y `GET` bajo `/api/incidents/{id}/attachments`.
- Manejo de excepciones en `GlobalExceptionHandler.java`.

### 4. Frontend: Integración de Vistas y Servicios
- `incidentService.js`: Funciones REST multipart.
- `CreateIncidentModal.jsx`: Selector de archivos, previsualización, descarte y subida.
- `IncidentDetailModal.jsx`: Galería visual de adjuntos.
- `Map.css`: Estilos visuales.

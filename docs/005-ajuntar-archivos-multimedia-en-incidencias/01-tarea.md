# Tarea 01: Capa de Persistencia, DTOs y Mapeador para Adjuntos Multimedia

**Feature:** `006-adjuntos-multimedia-en-incidencias` (`HU05`)  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-1, RF-6, RF-7  

---

## 1. Resumen de la Tarea

En esta tarea se ha establecido la arquitectura base de persistencia y transferencia desacoplada de datos para soportar la subida y asociación de archivos multimedia a incidencias en el backend de UrbanPulse:

1. **Entidad JPA `ArchivoAdjunto`:**
   - Ubicada en `org.bulbaseros.entity`, vinculada a la tabla `attachments` de PostgreSQL.
   - Campos: `id` (identificador primario autoincremental), `incident` (relación `@ManyToOne` con `Incidencia`), `fileName` (nombre seguro normalizado), `fileUrl` (ruta pública relativa de acceso), `mimeType` (tipo de contenido), `fileSizeBytes` (longitud en bytes) y `uploadedAt` (marca temporal).
   - Equipada con `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, ciclo de vida `@PrePersist` para inicializar automáticamente `uploadedAt`, y documentación Javadoc completa.

2. **Repositorio `ArchivoAdjuntoRepository`:**
   - Ubicado en `org.bulbaseros.repository`, extendiendo `JpaRepository<ArchivoAdjunto, Long>`.
   - Métodos declarativos de consulta: `findByIncidentId(Long incidentId)` y `findByIncidentIdOrderByUploadedAtAsc(Long incidentId)` para recuperar los adjuntos asociados a una incidencia en orden cronológico.

3. **DTO `AttachmentResponse`:**
   - Creado en `org.bulbaseros.dto` para exponer metadatos estructurados (`id`, `incidentId`, `fileName`, `fileUrl`, `mimeType`, `fileSizeBytes`, `uploadedAt`) abstrayendo la entidad JPA y cumpliendo el contrato de la API.
   - Provisto de anotaciones Lombok y constructores completos.

4. **Mapeador `AttachmentMapper`:**
   - Creado en `org.bulbaseros.mappers` como `@Component` de Spring, extendiendo la clase base `BaseMapper<ArchivoAdjunto, AttachmentResponse>`.
   - Implementa transformación individual `toDto(ArchivoAdjunto entity)` y de colecciones `toDtoList(List<ArchivoAdjunto> entities)` garantizando el aislamiento de capas arquitectónicas.

5. **Pruebas Unitarias Exhaustivas:**
   - `AttachmentResponseTest`: Validación de constructores, getters, setters, builder y `equals/hashCode/toString`.
   - `AttachmentMapperTest`: Validación de mapeo entidad a DTO, conversión de listas, comportamiento ante listas vacías y nulos.

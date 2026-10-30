# 006 — Adjuntos Multimedia en Incidencias — Tareas

_Checklist accionable derivada del `plan.md`. Tareas principales y subtareas concretas; marca `[x]` al completarlas y validarlas._

## [x] Tarea 1: Capa de Persistencia, DTOs y Mapeador para Adjuntos Multimedia en Backend - RF: RF-1, RF-4, RF-7
- [x] 1.1 Crear entidad `ArchivoAdjunto` en `org.bulbaseros.entity` con relación a `Incidencia`, `@Builder`, `@PrePersist` y Javadoc completo.
- [x] 1.2 Crear repositorio `ArchivoAdjuntoRepository` con métodos `findByIncidentId` y `findByIncidentIdOrderByUploadedAtAsc`.
- [x] 1.3 Crear DTO `AttachmentResponse` con metadatos estructurados (`id`, `incidentId`, `fileName`, `fileUrl`, `mimeType`, `fileSizeBytes`, `uploadedAt`).
- [x] 1.4 Crear mapeador `AttachmentMapper` extendiendo `BaseMapper<ArchivoAdjunto, AttachmentResponse>`.
- [x] 1.5 Crear pruebas unitarias en `AttachmentResponseTest` y `AttachmentMapperTest`.

## [x] Tarea 2: Configuración de Almacenamiento, Spring MVC y Spring Security en Backend - RNF: Seguridad, Almacenamiento - RF: RF-3, RF-6
- [x] 2.1 Configurar propiedades multipart (5MB) y directorio `app.upload.dir` en `application.properties`, `application-dev.properties` y entorno de test.
- [x] 2.2 Crear `WebConfig` con implementación de `WebMvcConfigurer` para exponer `/uploads/**` mapeado a la carpeta física de subidas.
- [x] 2.3 Modificar `SecurityConfig` para permitir acceso público anónimo con `.permitAll()` a la ruta `/uploads/**`.

## [x] Tarea 3: Lógica de Negocio y Controladores REST para Archivos Multimedia en Backend - RF: RF-2, RF-3, RF-4, RF-7, RF-8
- [x] 3.1 Implementar servicio transaccional `AttachmentService` con validaciones de fichero vacío, formato JPEG/PNG, tamaño máx. 5MB, prevención de Path Traversal y persistencia segura con UUID.
- [x] 3.2 Implementar método `getAttachmentsByIncidentId` en `AttachmentService` ordenado cronológicamente.
- [x] 3.3 Exponer endpoints `POST /api/incidents/{id}/attachments` (HTTP 201) y `GET /api/incidents/{id}/attachments` (HTTP 200) en `IncidentController`.
- [x] 3.4 Manejar excepciones `MaxUploadSizeExceededException` e `IllegalArgumentException` en `GlobalExceptionHandler`.
- [x] 3.5 Crear pruebas unitarias e integración en `AttachmentServiceTest` e `IncidentAttachmentControllerTest`.

## [x] Tarea 4: Integración en el Cliente Frontend (React) - RF: RF-1, RF-2, RF-3, RF-5, RF-7
- [x] 4.1 Añadir métodos `uploadIncidentAttachment` y `getIncidentAttachments` en `frontend/src/services/incidentService.js`.
- [x] 4.2 Incorporar input file opcional con validación reactiva de tipo MIME y tamaño máx. 5MB en `CreateIncidentModal.jsx`.
- [x] 4.3 Implementar previsualización dinámica de fotografía (`URL.createObjectURL`), botón de descarte y soporte garantizado para crear con 0 adjuntos en `CreateIncidentModal.jsx`.
- [x] 4.4 Integrar galería visual de miniaturas en `IncidentDetailModal.jsx`.
- [x] 4.5 Añadir estilos visuales y responsivos para componentes de carga y galería en `Map.css`.

## [x] Tarea 5: Verificación Integral de Criterios de Aceptación, Cobertura y Calidad - Depende de Tareas 1-4 - RF: Todos
- [x] 5.1 Crear y ejecutar pruebas frontend en `CreateIncidentModal.test.jsx` cubriendo creación sin adjuntos, error de formato y subida exitosa.
- [x] 5.2 Ejecutar suite de pruebas de Backend (`mvn clean test`) verificando 100% de tests en verde y cobertura en JaCoCo.
- [x] 5.3 Ejecutar suite de pruebas y cobertura de Frontend (`npm run test:coverage` y `npm run lint`).
- [x] 5.4 Validar criterios de aceptación contra `spec.md` y documentar guía de prueba manual.

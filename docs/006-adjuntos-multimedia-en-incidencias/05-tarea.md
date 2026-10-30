# Tarea 05: Pruebas Automatizadas y Guía de Verificación

**Feature:** `006-adjuntos-multimedia-en-incidencias` (`HU05`)  
**Estado:** Completada ✅  
**Requisitos funcionales y no funcionales:** Todos (RF-1 a RF-7, RNF)  

---

## 1. Resumen de la Tarea

En esta tarea se ejecutó y validó la cobertura de pruebas automatizadas tanto en el backend como en el frontend, garantizando la robustez de las validaciones y la ausencia de regresiones en la plataforma:

1. **Pruebas en Backend (Spring Boot & JUnit 5):**
   - `AttachmentResponseTest`: Validación unitaria de DTO, constructores y patrón Builder.
   - `AttachmentMapperTest`: Validación unitaria del mapeador entre entidad `ArchivoAdjunto` y DTO `AttachmentResponse` (instancias individuales, listas y nulos).
   - `AttachmentServiceTest`: Batería de 9 pruebas unitarias verificando:
     - Subida exitosa de archivos JPEG y PNG con persistencia y retorno adecuado.
     - Rechazo de fichero vacío (`IllegalArgumentException`).
     - Rechazo de tipo MIME no admitido (`application/pdf`).
     - Rechazo de fichero que excede los 5MB permitidos.
     - Prevención de Path Traversal ante nombres con secuencias maliciosas (`../`).
     - Lanzamiento de `IncidentNotFoundException` si la incidencia no existe al subir o consultar.
     - Consulta de lista de adjuntos ordenada cronológicamente.
   - `IncidentAttachmentControllerTest`: Batería de 6 pruebas de integración MockMvc:
     - Rechazo HTTP 401 Unauthorized sin token JWT al invocar endpoints protegidos.
     - Respuesta HTTP 201 Created con metadatos JSON al subir adjunto autenticado.
     - Rechazo HTTP 400 Bad Request ante formato de imagen no admitido.
     - Rechazo HTTP 400 Bad Request ante fichero que supera el límite de tamaño.
     - Respuesta HTTP 200 OK en la consulta `GET /api/incidents/{id}/attachments`.
     - Acceso público permitido a `/uploads/**` sin cabeceras de autorización.

2. **Pruebas en Frontend (React, Vitest & React Testing Library):**
   - `CreateIncidentModal.test.jsx`: Batería de 3 pruebas unitarias completas:
     - Creación de incidencia sin archivo adjunto (0 adjuntos, flujo estándar).
     - Validación reactiva y mensaje de error al seleccionar archivo con formato no admitido (ej. `.pdf`).
     - Flujo completo con imagen válida (JPEG), renderizado de previsualización en DOM y doble llamada `createIncident` + `uploadIncidentAttachment`.

3. **Guía de Verificación Manual Rápida:**
   - **Paso 1:** Iniciar sesión como ciudadano en el frontend (`/login`).
   - **Paso 2:** Navegar al mapa interactivo (`/map`) y hacer clic en cualquier punto para abrir el modal de reporte.
   - **Paso 3:** Completar título, descripción, categoría y seleccionar una imagen (`.jpg` o `.png` < 5MB). Observar la tarjeta de previsualización generada en el formulario.
   - **Paso 4:** Confirmar el envío del reporte. Comprobar que la incidencia se registra y la imagen se sube correctamente.
   - **Paso 5:** Abrir el modal de detalle de la incidencia recién creada y verificar la presencia de la fotografía en la galería inferior.

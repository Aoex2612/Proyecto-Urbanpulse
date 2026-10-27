# Tarea 05: Pruebas Automatizadas y Guía de Verificación

## Descripción
Validación de la funcionalidad completa mediante tests unitarios, tests de integración y pruebas de extremo a extremo, asegurando ausencia de regresiones.

## Cobertura de Pruebas
1. **Backend**:
   - `AttachmentResponseTest`: Validación unitaria de DTO, constructores y builder.
   - `AttachmentMapperTest`: Cobertura de mapeo entidad a DTO, listas y casos nulos.
   - `AttachmentServiceTest`: 9 pruebas unitarias cubriendo subida exitosa de JPEG y PNG, rechazo de archivos vacíos, rechazo de formatos no permitidos (PDF), rechazo de archivos > 5MB, prevención de path traversal y consulta de adjuntos.
   - `IncidentAttachmentControllerTest`: 6 pruebas de integración MockMvc verificando rechazo 401 sin autenticación, retorno 201 Created con metadatos JSON al autenticar, rechazo 400 en formatos inválidos y exceso de tamaño, consulta 200 OK y acceso público a `/uploads/**`.
   - **Resultado Backend**: 199 tests ejecutados con 100% de éxito (`BUILD SUCCESS`).
2. **Frontend**:
   - `CreateIncidentModal.test.jsx`:
     - Creación de incidencia con 0 imágenes adjuntas exitosa.
     - Detección y advertencia de formato no permitido.
     - Adjunto de imagen válido, previsualización en DOM y doble llamada `createIncident` + `uploadIncidentAttachment`.
   - **Resultado Frontend**: 9 tests ejecutados con 100% de éxito (Vitest) y ESLint limpio con 0 warnings/errores.

## Estado
- **Completada**: Cobertura integral y verificación lista para despliegue.

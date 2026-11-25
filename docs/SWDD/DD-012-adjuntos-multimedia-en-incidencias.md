# DD-012 — Adjuntos multimedia en incidencias

| Metadato | Valor |
|---|---|
| Estado | En revisión |
| Autoría | Equipo UrbanPulse (refactorización documental) |
| Fecha / última revisión | 2026-11-20 |
| Iteración | Refactorización documental: trazabilidad SDD ↔ ADR |
| Revisores | Equipo técnico UrbanPulse (pendiente de revisión) |
| Enlaces | [Feature 006](../../features/006-adjuntos-multimedia-en-incidencias/spec.md), [plan 006](../../features/006-adjuntos-multimedia-en-incidencias/plan.md), [QA note](../006-adjuntos-multimedia-en-incidencias/qa-note.md), [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md) |

## 1. Resumen ejecutivo

La feature 006 añade fotografías JPEG/PNG opcionales al reporte de incidencias, con validación de tipo y tamaño, almacenamiento en disco con nombre UUID y exposición mediante `/uploads/**`. El diseño se apoya en ADR-03: la entidad `ArchivoAdjunto` nunca se expone; `AttachmentResponse` se construye con `AttachmentMapper` en la capa de servicio. El riesgo principal es el acceso público a los ficheros y el consumo de disco; se mitiga con sanitización de nombres y límite de 5 MB.

> **Nota documental:** los documentos originales de la feature 006 no citaban ningún ADR. Este DD establece la relación con ADR-03 (DTOs/mappers) y ADR-01 (persistencia de metadatos).

## 2. Contexto y problema

- **Situación actual verificable:** `POST /api/incidents/{id}/attachments` (201), `GET /api/incidents/{id}/attachments` (200), recurso estático `GET /uploads/**`; tabla `attachments` con `file_name`, `file_url`, `mime_type`, `file_size_bytes`, `uploaded_at`. Ver [spec 006](../../features/006-adjuntos-multimedia-en-incidencias/spec.md) y [plan 006](../../features/006-adjuntos-multimedia-en-incidencias/plan.md).
- **Problema:** una incidencia sin evidencia visual dificulta la validación municipal; almacenar blobs en PostgreSQL incrementaría el tamaño de la base relacional.
- **Motivación:** RF05 del [caso de estudio](../DESCRIPCION.md); feature validada con 8/8 RF.

## 3. Objetivos, no objetivos y alcance

**Objetivos**
- O1. Adjuntar 0..N imágenes con previsualización antes del envío.
- O2. Rechazar MIME no soportado o tamaño > 5 MB con HTTP 400.
- O3. Listar adjuntos de una incidencia en orden determinista y devolver 404 si la incidencia no existe.

**No objetivos**
- N1. Almacenar ficheros en la base de datos relacional.
- N2. Soportar vídeo, PDF u otros formatos en esta iteración.
- N3. Implementar control de acceso por adjunto (la ruta `/uploads/**` es pública según el diseño actual).

**Dentro del alcance:** `AttachmentService`, `AttachmentMapper`, `AttachmentResponse`, `WebConfig`, `SecurityConfig`, `CreateIncidentModal`, volumen `backend_uploads`.
**Fuera del alcance:** CDN, antivirus, compresión de imágenes, cuotas por usuario.

## 4. Requisitos y drivers arquitectónicos

| ID | Tipo | Necesidad / escenario | Criterio de aceptación y evidencia | Prioridad |
|---|---|---|---|---|
| RF-1 | Funcional | Preview/descarte en el modal antes de subir | [spec 006](../../features/006-adjuntos-multimedia-en-incidencias/spec.md) | Alta |
| RF-2 | Funcional | MIME inválido o tamaño excedido → 400 | Spec 006; `MaxUploadSizeExceededException` | Alta |
| RF-3 | Funcional | Nombre UUID y HTTP 201; 0 adjuntos permitidos | Spec 006 | Alta |
| RF-4 | Funcional | Listado ordenado 200; 404 si incidencia inexistente | Spec 006 | Alta |
| RC-1 | Restricción | Límite 5 MB = 5.242.880 bytes | [plan 006](../../features/006-adjuntos-multimedia-en-incidencias/plan.md) | Alta |
| RC-2 | Restricción | Entidades JPA solo en servicio; DTOs en controladores | [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md) | Alta |

## 5. Arquitectura actual y dependencias

- **C4 Contexto/Contenedores:** [C4 L2](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2), volumen `backend_uploads` en [deployment.md](../deployment.md).
- **Flujo actual:** modal → multipart → controlador → `AttachmentService` → disco (`app.upload.dir`) + fila en `attachments` → `AttachmentMapper` → `AttachmentResponse`.
- **Dependencias y restricciones:** Spring multipart, `WebConfig` (`/uploads/**`), `SecurityConfig` (`permitAll`), Lombok en DTOs.

## 6. Alternativas consideradas

| Alternativa | Descripción | Ventajas | Inconvenientes / riesgos | Evidencia disponible |
|---|---|---|---|---|
| A | Blob en PostgreSQL | Transaccionalidad y backup único | Crecimiento de la BD y serialización de binarios | [plan 006](../../features/006-adjuntos-multimedia-en-incidencias/plan.md) |
| B | Servicio de objetos (S3/MinIO) | Escalabilidad y CDN | Infraestructura nueva fuera de alcance | Decisión de diseño: no adoptada en la feature |
| C | Disco local + metadatos relacionales (elegida) | Simple y desplegable con Compose | Dependencia del volumen; requiere sanitización | [plan 006](../../features/006-adjuntos-multimedia-en-incidencias/plan.md) |

**Criterios de comparación:** simplicidad, coste de infraestructura, consistencia, seguridad.
**Elección propuesta y justificación:** C; mantiene el monolito modular descrito en la constitución y permite el despliegue actual con un volumen Docker.

## 7. Diseño propuesto

### 7.1. Visión y diagramas C4

- **Contexto (L1):** sin cambios, [C4 L1](../architecture.md#1-diagrama-de-contexto-c4-nivel-1).
- **Contenedores (L2):** backend + volumen `backend_uploads`; [C4 L2](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2).
- **Componentes (L3):** `AttachmentController`, `AttachmentService`, `AttachmentMapper`, `CreateIncidentModal`.
- **Secuencias:** selección de archivo → `URL.createObjectURL` → subida → validación → guardado → respuesta con `fileUrl` → listado.

### 7.2. Interfaces y contratos

`POST /api/incidents/{id}/attachments` con `multipart/form-data` (`file`) → 201 `AttachmentResponse`; `GET /api/incidents/{id}/attachments` → 200 lista; `GET /uploads/**` → recurso estático público. Errores 400/404 con `ErrorResponse` en [docs/api.md](../api.md).

### 7.3. Modelo y gestión de datos

Tabla `attachments` con `id`, `incident_id`, `file_name`, `file_url`, `mime_type`, `file_size_bytes`, `uploaded_at`. `file_name` se genera con UUID; se bloquean rutas `../`. El mapper `AttachmentMapper extends BaseMapper<ArchivoAdjunto, AttachmentResponse>` (ADR-03). Retención: mientras viva la incidencia; no se define política de borrado automático.

### 7.4. Fallos, concurrencia y resiliencia

Fichero vacío, MIME no JPEG/PNG o > 5 MB → 400 vía `GlobalExceptionHandler`. No hay reintentos; la subida no es idempotente (cada POST crea un adjunto distinto). La concurrencia se resuelve con nombres UUID.

### 7.5. Seguridad

Sanitización de nombres, bloqueo de `../` y validación de MIME en servidor (no solo en cliente). `/uploads/**` está marcado `permitAll`: es una decisión consciente del diseño actual y un riesgo a revisar si se requiere privacidad.

### 7.6. Operación y despliegue

Directorio configurable con `app.upload.dir`; volumen `backend_uploads` en Compose; rollback por tag elimina código pero no ficheros ya subidos. No hay SLOs numéricos de subida; el límite de 5 MB es la única restricción cuantitativa.

## 8. Decisiones y trade-offs

| Decisión propuesta | Beneficio | Coste / consecuencia | ¿Requiere ADR? |
|---|---|---|---|
| Disco local + tabla de metadatos | Simple y sin infra nueva | Dependencia del volumen | No — decisión de feature 006 |
| `AttachmentMapper` explícito | Contrato desacoplado | Clase adicional | No — ADR-03 |
| `/uploads/**` público | Galería inmediata | Sin control de acceso por fichero | Sí, si se decide restringir |
| Límite 5 MB | Control de disco | Rechazo de vídeos | No — spec 006 |

### 8.1. Decisiones arquitectónicas derivadas

- [ADR-03: DTOs y mappers dedicados](../ADR/ADR-03-desacoplamiento-dtos-mappers.md) — **aceptado**. Decisión de origen para la capa de contrato.
- [ADR-01: Persistencia de enums con VARCHAR/CHECK](../ADR/ADR-01-persistencia-enums-jpa.md) — coherencia del modelo relacional de metadatos.

## 9. Plan de implementación y migración

1. Crear entidad `ArchivoAdjunto`, tabla `attachments` y `AttachmentMapper`.
2. Implementar `AttachmentService` con validación, UUID y sanitización.
3. Exponer endpoints y mapeo estático `/uploads/**`; integrar preview en React.
4. Probar casos válidos/inválidos y ejecutar `mvn test`/`npm run lint`; reversión: desactivar endpoints y ocultar el selector de archivos.

**Dependencias:** ADR-01, ADR-03, DD-011 (alta de incidencias). **Impacto en versiones anteriores:** ninguno; los adjuntos son opcionales.

## 10. Estrategia de verificación y evaluación

| Requisito | Prueba/experimento | Entorno y datos | Métrica / umbral | Evidencia esperada |
|---|---|---|---|---|
| RF-1/RF-2 | `AttachmentServiceTest`, `IncidentAttachmentControllerTest` | Local, H2 + archivos temporales | 0 fallos | [QA note 006](../006-adjuntos-multimedia-en-incidencias/qa-note.md): 8/8 RF |
| RF-3/RF-4 | Tests de mapper y controlador | Local | 201/200/404 correctos | `AttachmentMapperTest`, `AttachmentResponseTest` |
| RC-1 | Fichero > 5 MB | Local | HTTP 400 | QA note 006 |
| RC-2 | JaCoCo | CI | 70% instr. / 60% ramas (referencia) | QA note 006: 95% instr. / 77% ramas backend; `AttachmentService` 97/79/98% |

No hay pruebas de carga ni SLOs de subida; los límites se validan solo funcionalmente.

## 11. Riesgos, hipótesis y cuestiones abiertas

| ID | Tipo | Descripción | Impacto | Acción y responsable |
|---|---|---|---|---|
| R-1 | Riesgo | `/uploads/**` público permite acceso directo a imágenes | Medio | Evaluar control de acceso — seguridad |
| R-2 | Riesgo | Crecimiento del volumen sin retención | Medio | Definir política de limpieza — operaciones |
| Q-1 | Pregunta | ¿Ampliar formatos (vídeo/PDF) en otra iteración? | Bajo | Requiere nuevo DD y posible ADR |

## 12. Revisión y resultado

- **Comentarios de revisión:** pendientes.
- **Resolución:** pendiente de revisión técnica.
- **Decisiones registradas:** [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md), [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md).
- **Diferencias observadas tras implementar:** el uso de `URL.createObjectURL`/`revokeObjectURL` y el manejo de `MaxUploadSizeExceededException` son detalles de implementación no presentes en el ADR; se documentan en los `docs/006-*` y aquí.

## Checklist de aprobación

- [x] Problema y alcance delimitados.
- [x] Drivers y restricciones documentados, sin atribuir al enunciado requisitos inexistentes.
- [x] Al menos dos alternativas comparadas.
- [x] Diagramas, datos y contratos coherentes.
- [x] Fallos, seguridad, operación y costes considerados en proporción al cambio.
- [x] Criterios de aceptación verificables y plan de pruebas reproducible.
- [x] Riesgos y cuestiones abiertas explícitos.
- [ ] Revisión técnica realizada; ADR enlazados cuando corresponde.

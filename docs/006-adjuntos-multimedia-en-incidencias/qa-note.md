# QA Note — Feature 006 / HU05: Adjuntos Multimedia en Incidencias

**Veredicto:** ✅ APROBADO  
**Fecha de validación:** 2026-10-30  
**Rama validada:** `fix/arregloHU05` (base `feature/feature006_Adjuntos_Multimedia_IncidenciasHU05`)  
**Validado por:** sdd-validate (Antigravity)

---

## 1. Resumen de la Validación

La Feature 006 (Historia de Usuario HU05 - Adjuntos Multimedia en Incidencias) ha superado la validación completa. Todos los Requisitos Funcionales (RF-1 a RF-8) están cubiertos por pruebas automatizadas con resultado 100% satisfactorio.

Se ha verificado exhaustivamente que:
- Los ciudadanos pueden adjuntar de forma voluntaria fotografías (JPEG o PNG) con un tamaño máximo estricto de 5MB al momento de reportar una incidencia.
- El formulario modal de creación (`CreateIncidentModal`) genera de forma reactiva una previsualización de la imagen seleccionada y provee un botón para descartarla o reemplazarla.
- Se previene la sobrecarga de memoria en el navegador liberando los objetos Blob/URL generados tras el envío o cancelación (`URL.revokeObjectURL`).
- El backend (`AttachmentService`) valida estrictamente el tipo MIME y el peso de los archivos, sanitiza los nombres previniendo ataques de Path Traversal (`..`) y almacena los ficheros con prefijos únicos UUID en el directorio configurado (`app.upload.dir`).
- El servidor expone los archivos estáticos en `/uploads/**` de manera pública (`permitAll`) mediante `WebConfig`, permitiendo el renderizado directo en la interfaz sin requerir cabeceras de autorización.
- El modal de detalle (`IncidentDetailModal`) incorpora una galería de miniaturas con enlaces directos para visualización a resolución completa.
- Las incidencias reportadas sin fotografías (0 adjuntos) se crean y persisten sin errores ni llamadas innecesarias al servicio de subida.
- La suite completa de Backend arroja **272 tests, 0 fallos, 0 errores y 0 omitidos** (`BUILD SUCCESS`).
- La suite de Frontend arroja **6 suites, 43 tests, 0 fallos** y ESLint limpio con **0 errores y 0 advertencias**.

---

## 2. Tabla de Trazabilidad de Requisitos Funcionales (EARS) y Cobertura de Tests

| RF | Descripción (resumen EARS) | Tests que lo cubren | Estado |
|:---|:---|:---|:---:|
| **RF-1** | CUANDO el ciudadano selecciona una fotografía en el modal de reporte, EL SISTEMA mostrará la tarjeta de previsualización y botón de descarte. | `CreateIncidentModal.test.jsx` — permite adjuntar una imagen válida, muestra preview y la sube tras crear | ✅ PASS |
| **RF-2** | SI el tipo MIME no es `image/jpeg` ni `image/png`, ENTONCES EL SISTEMA rechazará la subida con HTTP 400 y mensaje en UI. | `AttachmentServiceTest` — `uploadAttachment_invalidMimeType_throwsException`; `IncidentAttachmentControllerTest` — `uploadAttachment_invalidMimeType_returns400`; `CreateIncidentModal.test.jsx` — muestra error con archivo no permitido | ✅ PASS |
| **RF-3** | SI el fichero supera el límite de 5MB, ENTONCES EL SISTEMA rechazará la subida con HTTP 400. | `AttachmentServiceTest` — `uploadAttachment_oversizedFile_throwsException`; `IncidentAttachmentControllerTest` — `uploadAttachment_oversizedFile_returns400` | ✅ PASS |
| **RF-4** | CUANDO se sube un archivo válido, EL SISTEMA almacenará con UUID en disco, persistirá en tabla `attachments` y retornará HTTP 201 Created. | `AttachmentServiceTest` — `uploadAttachment_validJpeg_success`, `uploadAttachment_validPng_success`; `IncidentAttachmentControllerTest` — `uploadAttachment_success_returns201` | ✅ PASS |
| **RF-5** | DONDE el ciudadano decida no adjuntar fotografía, EL SISTEMA creará la incidencia con 0 adjuntos sin error. | `CreateIncidentModal.test.jsx` — permite crear una incidencia sin adjuntar ninguna imagen (0 adjuntos) | ✅ PASS |
| **RF-6** | EL SISTEMA servirá públicamente las fotografías en `/uploads/**` sin exigir token JWT. | `IncidentAttachmentControllerTest` — `publicUploadsEndpoint_isPermittedWithoutAuth` | ✅ PASS |
| **RF-7** | CUANDO se consulte `GET /api/incidents/{id}/attachments`, EL SISTEMA retornará la lista ordenada cronológicamente con HTTP 200 OK. | `AttachmentServiceTest` — `getAttachmentsByIncidentId_success`; `IncidentAttachmentControllerTest` — `getAttachments_success_returns200` | ✅ PASS |
| **RF-8** | SI se solicita subida o consulta para un id de incidencia inexistente, ENTONCES EL SISTEMA retornará HTTP 404 Not Found. | `AttachmentServiceTest` — `uploadAttachment_incidentNotFound_throwsException`, `getAttachments_incidentNotFound_throwsException` | ✅ PASS |

**Cobertura funcional: 8/8 RF cubiertos (100%)**

---

## 3. Métricas de Cobertura Técnica de Código

### 3.1 Backend — JaCoCo

Ejecución: `mvn clean test`  
**Resultado: 272 tests — 0 fallos — 0 errores — 0 omitidos (`BUILD SUCCESS`)**  
**Clases analizadas:** 39 clases

| Métrica | Cubiertas | Total | % |
|:---|:---:|:---:|:---:|
| Instrucciones | 4.212 | 4.406 | **95%** |
| Ramas | 319 | 410 | **77%** |
| Líneas | 1.094 | 1.144 | **95.6%** |
| Métodos | 288 | 299 | **96.3%** |
| Clases | 38 | 39 | **97.4%** |

#### Desglose por componente de la feature:

| Componente Backend | % Instrucciones | % Ramas | % Líneas |
|:---|:---:|:---:|:---:|
| `org.bulbaseros.service.AttachmentService` | 97% | 79% | 98% |
| `org.bulbaseros.mappers.AttachmentMapper` | 100% | 100% | 100% |
| `org.bulbaseros.config.WebConfig` | 100% | 87% | 100% |
| `org.bulbaseros.dto.AttachmentResponse` | 95% | 69% | 96% |
| `org.bulbaseros.entity.ArchivoAdjunto` | 93% | 70% | 94% |
| `org.bulbaseros.controller.IncidentController` | 98% | 77% | 100% |

> Los umbrales mínimos del proyecto (70% instrucciones, 60% ramas) se superan ampliamente en todos los componentes.

---

### 3.2 Frontend — Vitest & ESLint

Ejecución: `npm run test:coverage` y `npm run lint`  
**Resultado: 6 suites — 43 tests — 0 fallos — lint 0 errores — lint 0 warnings**

#### Cobertura global del proyecto:

| Métrica | % |
|:---|:---:|
| Sentencias | 66.60% |
| Ramas | 56.88% |
| Funciones | 54.62% |
| Líneas | 67.27% |

#### Cobertura de los archivos implicados en la feature:

| Archivo Frontend | % Sentencias | % Ramas | % Funciones | % Líneas |
|:---|:---:|:---:|:---:|:---:|
| `CreateIncidentModal.jsx` | 74.55% | 73.33% | 69.23% | 77.35% |
| `IncidentDetailModal.jsx` | 60.29% | 51.94% | 52.94% | 61.19% |
| `incidentService.js` | 52.94% | 71.42% | 22.22% | 52.94% |

---

## 4. Guía de Prueba Manual

### Requisitos previos
- Docker Desktop o servicio Docker en ejecución.
- Node.js ≥ 18 y Java 21 instalados.

### Pasos de inicialización rápida

1. **Levantar la base de datos:**
   ```bash
   docker compose up -d
   ```
2. **Iniciar el Backend:**
   ```bash
   cd Backend
   mvn spring-boot:run
   ```
   El backend iniciará en `http://localhost:8080`.
3. **Iniciar el Frontend:**
   ```bash
   cd frontend
   npm run dev
   ```
   El cliente web estará disponible en `http://localhost:5173`.

---

### Escenario A — Reporte con imagen adjunta válida (flujo principal)

1. Abre `http://localhost:5173` e inicia sesión como ciudadano (o regístrate si es la primera vez).
2. Haz clic en **Mapa** y pulsa sobre cualquier punto en la ciudad de Málaga para reportar una incidencia.
3. Completa los campos obligatorios: **Título**, **Descripción** y **Categoría**.
4. En el campo **Fotografía / Evidencia**, selecciona un archivo de imagen en formato `.jpg`, `.jpeg` o `.png` menor a 5MB.
5. Comprueba que inmediatamente aparece la tarjeta de **Previsualización de imagen** mostrando la miniatura y un botón "Eliminar".
6. Pulsa en **"Crear Incidencia"**.
7. Verifica que la incidencia se registra en el mapa y que la imagen se almacena en el servidor.
8. Pulsa sobre el marcador de la incidencia creada para abrir el **Modal de Detalle**.
9. Comprueba que en la sección **Fotografías adjuntas** se muestra la imagen en la galería y que al hacer clic en ella se abre a tamaño completo en una nueva pestaña.

---

### Escenario B — Creación de incidencia sin archivo (0 adjuntos)

1. En la vista **Mapa**, haz clic sobre un punto para reportar una nueva incidencia.
2. Rellena los datos de texto obligatorios y **no selecciones ningún archivo**.
3. Pulsa en **"Crear Incidencia"**.
4. Verifica que la incidencia se da de alta correctamente sin errores de red ni advertencias de validación.

---

### Escenario C — Rechazo de formatos no permitidos o tamaño excesivo

1. En el modal de reporte, selecciona un archivo con extensión no permitida (por ejemplo un `.pdf` o `.txt`).
2. Comprueba que el formulario muestra una alerta de validación ("Solo se permiten imágenes JPEG o PNG") y no permite procesar el archivo.
3. Intenta seleccionar una imagen cuyo tamaño supere los 5MB.
4. Comprueba que el formulario muestra la advertencia correspondiente ("El archivo no debe superar los 5MB") y limpia el campo.

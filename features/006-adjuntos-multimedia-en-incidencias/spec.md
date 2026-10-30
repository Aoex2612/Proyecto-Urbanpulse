# Spec 006 — Adjuntos Multimedia en Incidencias
**Estado:** implementado ✅

## Contexto y objetivo
Permitir a la ciudadanía aportar evidencias gráficas y fotográficas sobre incidencias urbanas detectadas en la vía pública, facilitando la comprensión visual de los problemas por parte de los servicios municipales y técnicos encargados de su resolución. Asimismo, permitir la consulta pública y transparente de dichas evidencias tanto en la ficha de detalle de cada aviso como a través de la interfaz web.

## Usuarios / actores
- **Ciudadano (`CITIZEN`):** Puede adjuntar voluntariamente una fotografía (JPEG o PNG, máx. 5MB) en el momento de reportar una incidencia en el mapa interactivo, previsualizarla antes de enviarla o descartarla.
- **Usuario Autenticado:** Puede consultar la ficha de detalle de cualquier incidencia y visualizar en la galería multimedia las fotografías asociadas.
- **Operador (`OPERATOR`) y Administrador (`ADMIN`):** Pueden examinar visualmente las evidencias adjuntas desde el panel de triaje y detalle de incidencias para priorizar y planificar las intervenciones.

## Historias de usuario
- **H1 (HU05 - Adjuntos Multimedia en Incidencias):** Como ciudadano quiero adjuntar una fotografía opcional al reportar una incidencia para que los operarios municipales puedan apreciar visualmente el problema reportado.

## Requisitos funcionales (criterios de aceptación en EARS)

### Subida y Asociación de Fotografías (H1)
- **RF-1:** CUANDO el ciudadano selecciona una fotografía en el formulario de creación de incidencia, EL SISTEMA mostrará una tarjeta de previsualización de la imagen junto con la opción de descartarla o cambiarla.
- **RF-2:** SI el fichero seleccionado no corresponde a un tipo MIME admitido (`image/jpeg` o `image/png`), ENTONCES EL SISTEMA informará del error al usuario impidiendo la subida y rechazando la solicitud con código HTTP 400 Bad Request.
- **RF-3:** SI el fichero supera el límite máximo de 5MB (5,242,880 bytes), ENTONCES EL SISTEMA informará del exceso de tamaño al usuario rechazando la solicitud con código HTTP 400 Bad Request.
- **RF-4:** CUANDO se confirma la subida de un archivo válido asociado a una incidencia, EL SISTEMA almacenará el archivo en disco con un nombre físico único generado por UUID para prevenir colisiones y riesgos de Path Traversal, persistirá los metadatos en la tabla `attachments` y retornará código HTTP 201 Created.
- **RF-5:** DONDE el ciudadano decida no adjuntar ninguna fotografía al crear una incidencia, EL SISTEMA registrará la incidencia con 0 adjuntos asociados sin provocar errores ni requerir archivos obligatorios.
- **RF-6:** EL SISTEMA expondrá las fotografías adjuntas a través de la ruta web `/uploads/**` de forma pública y accesible para su renderizado directo en clientes sin requerir cabeceras de autorización.
- **RF-7:** CUANDO un usuario consulte el detalle o el listado de adjuntos de una incidencia existente (`GET /api/incidents/{id}/attachments`), EL SISTEMA retornará la lista ordenada cronológicamente de los archivos asociados con código HTTP 200 OK.
- **RF-8:** SI se solicita la subida o consulta de adjuntos para un identificador de incidencia inexistente, ENTONCES EL SISTEMA retornará código HTTP 404 Not Found.

## Requisitos no funcionales
- **Seguridad en almacenamiento:** Los nombres físicos de los ficheros se normalizan sustituyendo caracteres no seguros y agregando prefijos UUID aleatorios para prevenir vulnerabilidades de Path Traversal.
- **Rendimiento de subida y lectura:** Los recursos estáticos se sirven de forma directa y asíncrona desde el sistema de archivos local desacoplado de la lógica transaccional.
- **Límites de tamaño en servidor:** Se restringe a nivel de infraestructura Spring Boot el tamaño máximo de petición multipart y fichero individual a 5MB.
- **Experiencia de usuario y usabilidad frontend:** La previsualización de la fotografía en el cliente es instantánea mediante `URL.createObjectURL`, liberando los recursos de memoria asociados tras el envío o cancelación (`URL.revokeObjectURL`).

## Casos límite
- **Subida de fichero con nombre conteniendo secuencias de escape:** Ficheros con nombres que contienen `../` o secuencias de escape son bloqueados arrojando excepción de seguridad.
- **Subida con fichero vacío (0 bytes):** Se rechaza con código HTTP 400 Bad Request y mensaje descriptivo.
- **Petición sin autenticación:** Endpoints de subida protegidos rechazan solicitudes anónimas con código HTTP 401 Unauthorized.
- **Creación de incidencias sin imagen:** Se completa el flujo de creación sin ejecutar llamadas innecesarias de subida de adjuntos.

## Fuera de alcance
- Subida de archivos de vídeo, audio o documentos PDF (restringido estrictamente a formatos gráficos JPEG y PNG).
- Almacenamiento en servicios cloud de objetos como AWS S3 o Google Cloud Storage (se implementa almacenamiento en sistema de archivos local para la fase actual).
- Edición, recorte o rotación de fotografías en el cliente frontend.

## Criterios de aceptación
- [x] Un ciudadano puede adjuntar una imagen JPEG o PNG menor a 5MB al crear una incidencia.
- [x] El formulario permite previsualizar la fotografía y eliminarla antes de confirmar el envío.
- [x] El sistema rechaza formatos inválidos y tamaños superiores a 5MB tanto en cliente como en backend (HTTP 400).
- [x] El sistema permite crear incidencias con 0 adjuntos sin errores.
- [x] Las imágenes se sirven públicamente bajo la ruta `/uploads/**`.
- [x] El modal de detalle de una incidencia muestra la galería de adjuntos asociados.
- [x] Cobertura de tests unitarios y de integración superada al 100%.

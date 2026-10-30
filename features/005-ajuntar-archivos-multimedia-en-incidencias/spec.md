# Spec 005 / 006 — Adjuntos Multimedia en Incidencias
**Estado:** implementado ✅

_Referencia principal: [Spec 006 — Adjuntos Multimedia en Incidencias](../006-adjuntos-multimedia-en-incidencias/spec.md)_

## Contexto y objetivo
Permitir a la ciudadanía aportar evidencias gráficas y fotográficas sobre incidencias urbanas detectadas en la vía pública, facilitando la comprensión visual de los problemas por parte de los servicios municipales y técnicos encargados de su resolución.

## Usuarios / actores
- **Ciudadano (`CITIZEN`)**: Adjunta fotografía opcional (JPEG/PNG, máx. 5MB) al crear una incidencia en el mapa.
- **Usuario Autenticado**: Consulta fotografías en el modal de detalle de incidencias.
- **Operador (`OPERATOR`) / Administrador (`ADMIN`)**: Visualiza evidencias fotográficas para triaje.

## Historias de usuario
- **H1 (HU05 - Adjuntos Multimedia en Incidencias)**: Como ciudadano quiero adjuntar una fotografía opcional al reportar una incidencia para que los operarios municipales puedan apreciar visualmente el problema reportado.

## Requisitos funcionales (criterios de aceptación en EARS)
- **RF-1:** CUANDO el ciudadano selecciona una fotografía en el formulario de creación de incidencia, EL SISTEMA mostrará una tarjeta de previsualización de la imagen junto con la opción de descartarla o cambiarla.
- **RF-2:** SI el fichero seleccionado no corresponde a un tipo MIME admitido (`image/jpeg` o `image/png`), ENTONCES EL SISTEMA informará del error al usuario impidiendo la subida y rechazando la solicitud con código HTTP 400 Bad Request.
- **RF-3:** SI el fichero supera el límite máximo de 5MB (5,242,880 bytes), ENTONCES EL SISTEMA informará del exceso de tamaño al usuario rechazando la solicitud con código HTTP 400 Bad Request.
- **RF-4:** CUANDO se confirma la subida de un archivo válido asociado a una incidencia, EL SISTEMA almacenará el archivo en disco con un nombre físico único generado por UUID para prevenir colisiones y riesgos de Path Traversal, persistirá los metadatos en la tabla `attachments` y retornará código HTTP 201 Created.
- **RF-5:** DONDE el ciudadano decida no adjuntar ninguna fotografía al crear una incidencia, EL SISTEMA registrará la incidencia con 0 adjuntos asociados sin provocar errores ni requerir archivos obligatorios.
- **RF-6:** EL SISTEMA expondrá las fotografías adjuntas a través de la ruta web `/uploads/**` de forma pública y accesible para su renderizado directo en clientes sin requerir cabeceras de autorización.
- **RF-7:** CUANDO un usuario consulte el detalle o el listado de adjuntos de una incidencia existente (`GET /api/incidents/{id}/attachments`), EL SISTEMA retornará la lista ordenada cronológicamente de los archivos asociados con código HTTP 200 OK.
- **RF-8:** SI se solicita la subida o consulta de adjuntos para un identificador de incidencia inexistente, ENTONCES EL SISTEMA retornará código HTTP 404 Not Found.

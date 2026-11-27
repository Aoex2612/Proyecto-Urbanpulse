# ADR-12: Contrato de lectura aditivo para listados de incidencias

* Estado: propuesto
* Fecha: 2026-11-26
* Decisiones relacionadas: ADR-03 (desacoplamiento DTOs/mappers), ADR-10 (identificadores públicos UUID y versionado API v2), ADR-11 (agrupación de marcadores), Spec 013 (lectura paginada y respuesta ligera de incidencias)

## Contexto

Los listados de incidencias se sirven completos y sin paginación. La auditoría estima un payload de **3–8 MB con 5.000 incidencias**; la medición de la línea base del 2026-11-26 contra `/api/v2` con 4.500 incidencias activas registra **4.771.646 bytes (~4,55 MiB) por respuesta y una mediana de 9.851 ms** en el listado, y **659.660 bytes (~644 KiB) y 1.368 ms** en una búsqueda por categoría con 626 resultados. El DTO de la API v2 incluye `citizenEmail`, `description`, `urbanAssets`, `urbanContext` y `activeAssignments`, de modo que el mapa ciudadano recibe datos personales y colecciones que no necesita; el correo del ciudadano aparece 4.500 veces en una sola respuesta. Además, `pg_stat_user_tables` confirma **4.502 `seq_scan` sobre `assignments` para una sola petición**, evidencia del N+1 en el mapeo.

El frontend consume estos listados desde el mapa y desde el panel de operador. El panel descarga el catálogo completo y filtra en el navegador, y varias pruebas automatizadas dependen de la forma actual de la respuesta. El proyecto exige compatibilidad hacia atrás en los contratos existentes y prohíbe exponer entidades JPA directamente, por lo que cualquier cambio debe pasar por DTOs y mappers (ADR-03).

Además, el mapeo de cada incidencia resuelve datos de tareas y asignaciones, lo que hace sospechar un problema N+1 al procesar 5.000 entidades. La solución del contrato debe permitir medir y acotar el número de consultas.

## Opciones consideradas

* **Opción 1: convertir los endpoints actuales a paginación obligatoria.** Cambiar `List` por una respuesta paginada en `/incidents` y `/search`, sin parámetros opcionales.
* **Opción 2: contrato aditivo.** Añadir un endpoint ligero específico para el mapa y aceptar paginación opcional en los listados existentes, conservando la respuesta actual cuando no se envían parámetros.
* **Opción 3: GraphQL o BFF.** Exponer un contrato de consulta flexible que devuelva solo los campos pedidos por cada vista.
* **Opción 4: paginación solo en cliente.** Mantener el contrato actual y paginar en el navegador sobre la lista completa.

## Decisión tomada

Optamos por la **Opción 2: contrato de lectura aditivo**, con este alcance:

1. **Endpoint ligero de mapa:** `GET /api/v2/incidents/map` devuelve un DTO reducido con identificador, latitud, longitud, estado, prioridad, categoría, fecha de creación y título corto. No incluye descripción, correo del ciudadano, activos urbanos, contexto urbano ni asignaciones activas. Las incidencias sin coordenadas válidas se omiten.
2. **Paginación opcional en listados:** `/api/v2/incidents` y `/api/v2/incidents/search` aceptan `page` y `size` opcionales. Sin esos parámetros, la respuesta conserva la forma y el contenido actuales. Con ellos, la respuesta usa el `PageResponse` ya existente (`content`, `totalElements`, `totalPages`, `page`, `size`).
3. **Límites de paginación:** `size` entre 1 y 100; página base 0 en el API y presentación base 1 en la interfaz. Un `size` inválido devuelve error de validación, no un valor silencioso.
4. **Sin cambios por defecto:** si el cliente no envía `page` ni `size`, tanto `/incidents` como `/search` conservan la forma y el contenido actuales, incluida la búsqueda sin límite. La acotación por defecto se pospone a una versión mayor para no romper consumidores ni pruebas.
5. **Panel de operador:** consume la paginación del servidor con 10 elementos por página y envía los filtros multicriterio como parámetros de consulta.
6. **Eficiencia de consulta:** la recuperación de incidencias activas se resuelve con proyección a DTO o con carga anticipada de relaciones, de forma que el número de consultas no crezca con el número de incidencias. Se verifica con el registro de SQL.
7. **Privacidad:** el correo del ciudadano no forma parte del DTO de mapa. Se añade una prueba de regresión que falla si el campo reaparece.

## Razón / Por qué se tomó la decisión

* **Retrocompatibilidad real:** los consumidores y pruebas que no envían paginación siguen recibiendo la misma forma, lo que permite migrar de forma incremental y no bloquea otras funcionalidades.
* **Privacidad por diseño:** el endpoint de mapa elimina el correo del ciudadano y las colecciones que no se usan, corrigiendo una fuga de dato personal además de una mejora de rendimiento.
* **Reducción medible del payload:** pasar de 4,55 MiB medidos a menos de 1 MB para 4.500 incidencias activas reduce el tiempo de transferencia, el parseo y la memoria del navegador.
* **Reutilización del contrato existente:** el `PageResponse` ya se usa en `/my-incidents` y en el listado legacy, por lo que no se introduce un formato nuevo.
* **Alineación con el flujo SDD:** permite aprobar la Spec 013 con criterios verificables y mantiene las decisiones de arquitectura documentadas antes de tocar código.
* **Menor riesgo que las alternativas:** GraphQL o un BFF añaden infraestructura y superficie de mantenimiento; la paginación solo en cliente no reduce el payload ni la exposición de datos; la paginación obligatoria rompe consumidores y pruebas en la misma entrega.

## Consecuencias

* **Positivo:** mejora del tiempo de carga y de la memoria del mapa, eliminación de datos personales de la respuesta cartográfica, panel de operador navegable con 5.000 registros y base para medir el N+1.
* **Positivo:** el patrón de paginación queda alineado con `/my-incidents`, lo que reduce la diversidad de contratos de lectura.
* **Negativo:** conviven temporalmente dos formas de respuesta en los listados (lista completa y paginada) hasta que se decida una deprecación en una versión mayor.
* **Negativo:** se añade un DTO, un mapper y un endpoint nuevos, con su coste de pruebas y documentación.
* **Negativo:** la búsqueda sin paginación sigue devolviendo el catálogo completo, por lo que el riesgo de respuestas grandes permanece hasta que se adopte un límite por defecto en una versión mayor. Se mitiga migrando el panel de operador a paginación y documentando el límite como deuda futura.
* **Neutral:** la paginación base 0 en el API y base 1 en la interfaz exige una conversión explícita en el cliente y pruebas de los límites.
* **Neutral:** la proyección a DTO puede requerir ajustes en repositorios y consultas; se validará con los tests de integración con PostgreSQL.
* **Riesgo:** si la proyección no elimina el N+1, el endpoint ligero seguirá siendo lento pese a devolver menos datos. La verificación con SQL es parte de la aceptación, no una tarea opcional.
* **Deprecación futura:** en una versión mayor podrá hacerse obligatoria la paginación y retirarse la respuesta completa, pero no en esta entrega.
* **Seguridad:** el correo del ciudadano deja de viajar en la respuesta del mapa; los roles y permisos de los endpoints se mantienen sin cambios.

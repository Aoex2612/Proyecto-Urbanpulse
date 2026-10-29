# Spec 004 — Reporte y Seguimiento de Incidencias con Detección Territorial
**Estado:** implementado ✅

## Contexto y objetivo
Facilitar a la ciudadanía de Málaga la comunicación integral de incidencias urbanas con captura precisa de localización y dirección normalizada, complementando el aviso con la resolución automática de su barrio y distrito municipal sin bloquear el registro ante fallos externos. Asimismo, proporcionar a los ciudadanos una sección dedicada para consultar, filtrar y seguir de forma transparente la evolución y las fechas clave del estado de sus propios reportes.

## Usuarios / actores
- **Ciudadano (`CITIZEN`):** Puede dar de alta incidencias indicando posición geográfica en el mapa interactivo, beneficiándose de la dirección y delimitación territorial resueltas automáticamente, y acceder a su sección personal "Mis Incidencias" para consultar y filtrar los avisos reportados por él.
- **Usuario Autenticado:** Puede acceder a la visualización de la ficha de detalle de cualquier incidencia registrada para consultar sus datos básicos, dirección, distrito, barrio, estado actual y fechas clave.
- **Operador (`OPERATOR`) y Administrador (`ADMIN`):** Visualizan en los paneles de gestión y detalle existentes la dirección normalizada, el barrio y el distrito asignados automáticamente a cada reporte.

## Historias de usuario
- **H1 (HU04 - Reporte con Ubicación y Dirección):** Como ciudadano quiero registrar una incidencia marcando el punto en el mapa interactivo para que el sistema asocie las coordenadas y la dirección oficial del callejero de Málaga de forma automática.
- **H2 (HU15 - Detección Automática de Barrio y Distrito):** Como sistema quiero identificar automáticamente el barrio y distrito municipal a partir de las coordenadas del reporte consultando los datos abiertos de Málaga para catalogar territorialmente la incidencia.
- **H3 (HU06 - Consulta y Seguimiento de Incidencias Propias):** Como ciudadano quiero consultar un listado paginado y ordenado de mis incidencias reportadas junto con su detalle y fechas de avance para conocer su estado de resolución en tiempo real.

## Requisitos funcionales (criterios de aceptación en EARS)

### Reporte de Incidencia con Ubicación y Dirección (H1)
- **RF-1:** CUANDO el ciudadano confirma el punto de una incidencia en el mapa interactivo, EL SISTEMA capturará las coordenadas geográficas de latitud y longitud.
- **RF-2:** CUANDO se capturan las coordenadas geográficas, EL SISTEMA resolverá automáticamente la dirección postal a partir del callejero oficial de datos abiertos del Ayuntamiento de Málaga.
- **RF-3:** CUANDO el ciudadano visualiza el formulario de reporte de incidencia, EL SISTEMA mostrará la dirección postal resuelta en modo de solo lectura.
- **RF-4:** CUANDO el ciudadano envía el formulario de alta con título, descripción y categoría, EL SISTEMA registrará la incidencia vinculada a su cuenta con estado inicial `REPORTED` y prioridad `MEDIUM`.
- **RF-5:** SI las coordenadas geográficas aportadas se encuentran fuera del término municipal de Málaga (latitud fuera del rango [36.5000, 36.9000] o longitud fuera del rango [-4.7000, -4.2000]), ENTONCES EL SISTEMA notificará la advertencia al usuario bloqueando el registro del aviso.
- **RF-6:** SI la resolución de dirección no encuentra correspondencia en el callejero del Ayuntamiento de Málaga, ENTONCES EL SISTEMA persistirá la dirección con valor nulo.

### Detección Automática de Barrio y Distrito (H2)
- **RF-7:** CUANDO se registra una incidencia con coordenadas válidas dentro de Málaga, EL SISTEMA determinará automáticamente el distrito municipal consultando los límites oficiales de datos abiertos del Ayuntamiento de Málaga.
- **RF-8:** CUANDO se registra una incidencia con coordenadas válidas dentro de Málaga, EL SISTEMA determinará automáticamente el barrio municipal consultando los límites oficiales de datos abiertos del Ayuntamiento de Málaga.
- **RF-9:** CUANDO el usuario visualiza el formulario o detalle de una incidencia, EL SISTEMA mostrará el barrio y distrito como campos estrictamente de solo lectura.
- **RF-10:** SI la ubicación geográfica no coincide con ningún barrio o distrito catalogado, ENTONCES EL SISTEMA persistirá el barrio y distrito con valor nulo.
- **RF-11:** SI el barrio, distrito o dirección tienen valor nulo en una incidencia, ENTONCES EL SISTEMA presentará la etiqueta informativa "Sin asignar" en la interfaz.

### Consulta y Seguimiento de Incidencias (H3)
- **RF-12:** CUANDO un ciudadano autenticado accede a su sección personal de "Mis Incidencias", EL SISTEMA mostrará el listado paginado de las incidencias reportadas por dicho usuario.
- **RF-13:** CUANDO el ciudadano consulta su listado personal de incidencias, EL SISTEMA presentará los avisos ordenados descendentemente por fecha de creación.
- **RF-14:** CUANDO el ciudadano aplica un filtro por estado en su listado personal, EL SISTEMA actualizará los resultados permitiendo seleccionar entre "TODOS" (opción por defecto) o cualquiera de los estados oficiales (`REPORTED`, `VALIDATED`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `REOPENED`, `CLOSED`, `REJECTED`).
- **RF-15:** CUANDO un usuario autenticado consulta el detalle de una incidencia, EL SISTEMA mostrará la información completa del reporte con sus fechas clave de creación y última actualización.
- **RF-16:** EL SISTEMA permitirá la visualización del detalle de cualquier incidencia a los usuarios autenticados.
- **RF-17:** SI el ciudadano no cuenta con ninguna incidencia registrada en su listado personal, ENTONCES EL SISTEMA mostrará una vista vacía descriptiva con un botón de acción rápida para reportar una nueva incidencia en el mapa.
- **RF-18:** SI se solicita el detalle de una incidencia con un identificador inexistente en base de datos, ENTONCES EL SISTEMA retornará código HTTP 404 Not Found con mensaje estructurado de error y la interfaz informará de la no existencia del reporte.

## Requisitos no funcionales
- **Privacidad y aislamiento de datos:** Cada ciudadano solo puede acceder al listado privado de las incidencias creadas por su propia cuenta, protegiendo datos personales.
- **Tolerancia a fallos y degradación controlada:** El fallo o indisponibilidad en la resolución de dirección, barrio o distrito nunca debe abortar ni bloquear el alta de una incidencia ciudadana.
- **Tiempo de respuesta y timeout en fuentes externas:** La consulta a los servicios de datos abiertos del Ayuntamiento de Málaga dispondrá de un límite máximo de espera (timeout) de 3 segundos; si se rebasa, el sistema continuará el flujo persistiendo valor nulo en los campos territoriales afectados.
- **Usabilidad y adaptabilidad responsive:** La vista de seguimiento de incidencias propias debe ser accesible y legible en dispositivos móviles y de escritorio.
- **Rendimiento de consulta:** La consulta paginada del listado de incidencias del ciudadano debe responder en un tiempo inferior a 1 segundo para un tamaño de página estándar de 10 elementos.

## Casos límite
- **Paginación fuera de rango:** Si se solicita un número de página superior al total existente en el listado de incidencias, el sistema responderá con código HTTP 200 OK y una lista vacía de elementos (`content: []`), manteniendo metadatos coherentes (`totalElements`, `totalPages`).
- **Incidencias en zonas limítrofes entre distritos o barrios:** El sistema asigna el polígono territorial oficial con coincidencia o intersección espacial directa según la cartografía municipal.
- **Ciudadano con sesión expirada:** Redirección automática al inicio de sesión preservando la ruta solicitada tras autenticarse satisfactoriamente.
- **Dispositivo sin cobertura o fallo de red con la API municipal:** Se aplica degradación controlada inmediata registrando la incidencia con coordenadas y asignando valor nulo a dirección, barrio y distrito.
- **Identificador inválido en ruta de detalle:** Peticiones con identificadores alfanuméricos o negativos se rechazan con validación HTTP 400 Bad Request.

## Fuera de alcance
- Adjuntos multimedia y evidencias fotográficas (pertenecientes a la Feature 006 / HU05).
- Triaje operativo, cambio de estado asistido o reasignación por parte del ciudadano (restringido a roles de operador y administración).
- Algoritmos de detección semántica de duplicados (HU19).
- Modificación o edición de incidencias una vez emitidas y registradas.
- Edición manual de dirección, barrio o distrito por parte del usuario.

## Criterios de aceptación
- [x] Captura automática de coordenadas geográficas de latitud y longitud al marcar el punto en el mapa interactivo.
- [x] Resolución automática de la dirección postal a través del callejero de datos abiertos del Ayuntamiento de Málaga en modo de solo lectura.
- [x] Detección automática y asignación de distrito municipal a partir de las coordenadas del aviso consultando los datos abiertos de Málaga.
- [x] Detección automática y asignación de barrio a partir de las coordenadas del aviso consultando los datos abiertos de Málaga.
- [x] Persistencia con valor nulo (`null`) y visualización como "Sin asignar" si no se halla correspondencia territorial o si la fuente externa supera el timeout de 3 segundos.
- [x] Bloqueo de registro si las coordenadas geográficas exceden el término municipal de Málaga (latitud [36.5000, 36.9000], longitud [-4.7000, -4.2000]).
- [x] Endpoint `GET /api/incidents/my-incidents` implementado con soporte de paginación (`page`, `size`) para recuperar exclusivamente las incidencias del usuario autenticado.
- [x] Vista frontend "Mis Incidencias" implementada y protegida para el ciudadano, con listado ordenado cronológicamente descendentemente y filtro por estado.
- [x] Visualización de detalle de la incidencia mostrando estado actual, barrio, distrito, dirección y fechas clave (creación y actualización).
- [x] Respuesta HTTP 404 controlada ante identificadores de incidencia inexistentes tanto en backend como en frontend.
- [x] Respuesta HTTP 200 con lista vacía ante solicitudes de páginas fuera de rango en la paginación.
- [x] Permiso de lectura pública del detalle de cualquier incidencia para todos los usuarios autenticados.
- [x] Suite de pruebas unitarias y de integración de backend pasando satisfactoriamente (`mvn test`).
- [x] Suite de pruebas de frontend y verificación de estilo pasando satisfactoriamente (`npm run lint` y `npm test`).

## Dudas abiertas
_Todas las aclaraciones y ambigüedades detectadas en la clarificación han sido resueltas y consensuadas._

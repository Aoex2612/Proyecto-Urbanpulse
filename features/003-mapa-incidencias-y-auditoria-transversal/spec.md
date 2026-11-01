# Spec 003 — Mapa de Incidencias con Leaflet y Auditoría Transversal
**Estado:** en curso (incorporación de HU14.4)

## Contexto y objetivo
Proporcionar a los usuarios de UrbanPulse una experiencia visual interactiva centrada en el territorio mediante un mapa basado en Leaflet, facilitando tanto la visualización de las incidencias no terminadas en la ciudad de Málaga como la captura geolocalizada precisa para dar de alta nuevos avisos. Asimismo, garantizar la trazabilidad transversal y rendición de cuentas del ciclo de vida de cada incidencia mediante el registro inmutable de sus transiciones de estado (`status_changes`) y la visualización de una línea temporal (Timeline) de auditoría accesible para el rol de administración, diseñada de forma modular para admitir futura extensión a otros roles operativos.

## Usuarios / actores
- **Usuario Autenticado:** Puede acceder a la vista cartográfica `/map` para explorar incidencias en la ciudad de Málaga y consultar su información pública en los popups del mapa.
- **Ciudadano (`CITIZEN`):** Puede iniciar el reporte de una nueva incidencia pulsando el botón de creación, marcando la ubicación exacta en el mapa mediante clic y completando los datos básicos (título, descripción, categoría desde catálogo).
- **Operador (`OPERATOR`):** Puede inspeccionar incidencias en el mapa y ejecutar transiciones de estado en el flujo operativo de triaje.
- **Técnico (`TECHNICIAN`) y Analista (`ANALYST`):** En esta iteración disponen de los mismos permisos de visualización general y reporte que los ciudadanos; las vistas de asignación de técnico y análisis agregado quedan diferidas a sus correspondientes HU posteriores.
- **Administrador (`ADMIN`):** Dispone de todas las capacidades anteriores y acceso exclusivo para auditar la traza cronológica completa de transiciones de cualquier incidencia a través del endpoint histórico y del componente visual de línea temporal (`Timeline`) en el modal de detalle.

## Historias de usuario
- **H1 (Visualización Cartográfica con Leaflet):** Como usuario autenticado quiero acceder a una vista de mapa interactivo centrado en Málaga para ver los marcadores de las incidencias no terminadas de la ciudad y consultar sus datos básicos al seleccionarlas en un popup.
- **H2 (Reporte Geolocalizado Asistido por Mapa):** Como ciudadano quiero activar el modo de creación de incidencia y marcar con un clic el punto exacto en el mapa para asociar automáticamente las coordenadas geográficas al formulario básico del aviso.
- **H3 (Auditoría Inmutable de Estados):** Como sistema quiero persistir un registro histórico auditable cada vez que una incidencia cambie de estado, vinculando automáticamente el usuario responsable a partir de su token JWT.
- **H4 (Consulta Histórica y Línea Temporal):** Como administrador quiero consultar el historial cronológico de cambios de estado de una incidencia en su modal de detalle para auditar quién realizó cada transición, en qué fecha y bajo qué justificación.
- **H5 (HU14.4 - Captura de Ubicación Actual en Reporte):** Como usuario que da de alta una incidencia, quiero pulsar el botón opcional "Usar mi ubicación actual" dentro del bloque de coordenadas del formulario para asociar automáticamente las coordenadas de mi dispositivo sin necesidad de fijarlas a mano sobre el mapa interactivo.

## Requisitos funcionales (criterios de aceptación en EARS)

### Visualización y Consulta en Mapa con Leaflet (H1)
- **RF-1:** CUANDO un usuario autenticado accede a la ruta `/map`, EL SISTEMA renderizará un mapa interactivo con Leaflet y teselas de OpenStreetMap centrado por defecto en la ciudad de Málaga (coordenadas de referencia `[36.7213, -4.4214]` y nivel de zoom urbano 13), obteniendo vía `GET /api/incidents` la lista de incidencias no terminadas (cuyo estado no sea `CLOSED` ni `REJECTED`) y posicionando un marcador en las coordenadas de cada una.
- **RF-2:** CUANDO el usuario hace clic sobre el marcador de una incidencia en el mapa, EL SISTEMA desplegará un Popup nativo de Leaflet mostrando los detalles básicos del reporte (título, descripción, categoría, estado actual) y un botón de acción "Ver detalle completo".
- **RF-3:** CUANDO el usuario pulsa el botón "Ver detalle completo" dentro del Popup de una incidencia, EL SISTEMA abrirá un modal de detalle con la información completa de la misma.
- **RF-4:** SI el usuario hace clic sobre el lienzo del mapa sin haber activado el modo de creación de incidencias, ENTONCES EL SISTEMA no registrará ningún punto ni abrirá formularios, limitándose a cerrar los popups que se encuentren abiertos.
- **RF-5:** SI el mapa no puede cargar las teselas de OpenStreetMap por fallo de red, ENTONCES EL SISTEMA mantendrá la interfaz operativa mostrando un mensaje de advertencia no bloqueante sin interrumpir la sesión del usuario.

### Reporte Geolocalizado desde el Mapa (H2)
- **RF-6:** CUANDO el usuario autenticado pulsa el botón "Crear incidencia" en la vista del mapa, EL SISTEMA conmutará el texto del botón a "Cancelar selección", habilitará el modo de selección geográfica e informará al usuario de que debe marcar el punto del incidente en el mapa.
- **RF-7:** CUANDO el usuario pulsa el botón "Cancelar selección" MIENTRAS el modo de selección geográfica esté activo, EL SISTEMA desactivará el modo de selección y restaurará el botón a "Crear incidencia" sin fijar ningún punto.
- **RF-8:** CUANDO el usuario hace clic sobre el mapa en modo de selección geográfica, EL SISTEMA capturará la latitud y longitud del punto seleccionado, restaurará el estado del botón y abrirá el formulario modal de alta con dichas coordenadas prefijadas en modo solo lectura.
- **RF-9:** SI el usuario cierra o cancela el formulario modal de alta sin enviarlo, ENTONCES EL SISTEMA descartará las coordenadas capturadas y cerrará el modal sin crear ningún marcador temporal ni persistir datos.
- **RF-10:** CUANDO el usuario completa y envía el formulario seleccionando una categoría del desplegable predefinido (`Alumbrado`, `Vías Públicas`, `Limpieza`, `Parques y Jardines`, `Mobiliario Urbano`) junto con título y descripción, EL SISTEMA emitirá una petición `POST /api/incidents` persistiendo la incidencia con estado inicial `REPORTED`, prioridad `MEDIUM` por defecto, fecha de creación actual y `citizen_id` obtenido del token JWT del usuario emisor.
- **RF-11:** CUANDO la creación de la incidencia concluye con éxito, EL SISTEMA responderá con código HTTP 201 Created, cerrará el formulario modal e incorporará de inmediato el nuevo marcador al mapa interactivo.
- **RF-12:** SI el usuario intenta enviar el formulario de reporte omitiendo el título, la descripción, la categoría o con coordenadas ausentes o fuera de rango geográfico válido, ENTONCES EL SISTEMA bloqueará el envío y notificará los errores de validación.

### Registro y Auditoría Transversal de Transiciones (H3)
- **RF-13:** CUANDO un usuario autorizado (`OPERATOR` o `ADMIN`) ejecuta una modificación de estado a través de `PATCH /api/incidents/{id}/status`, EL SISTEMA actualizará el estado de la incidencia e insertará de forma transaccional un registro en la tabla `status_changes` conteniendo el identificador de la incidencia, el estado previo (`from_status`), el estado resultante (`to_status`), el motivo o comentario justificado (`reason`), la fecha/hora actual y el identificador del usuario actor extraído de su token JWT (`changed_by_user_id`).
- **RF-14:** SI se invoca la actualización de estado sobre una incidencia inexistente en la base de datos, ENTONCES EL SISTEMA retornará código HTTP 404 (Not Found) con cuerpo normalizado `ErrorResponse`.

### Consulta Histórica y Componente Timeline (H4)
- **RF-15:** CUANDO un usuario con rol `ADMIN` solicita el historial de una incidencia mediante `GET /api/incidents/{id}/history`, EL SISTEMA retornará una lista cronológica ordenada ascendentemente por fecha con las transiciones de estado registradas en `status_changes` (id, estado anterior, estado nuevo, nombre/email del usuario actor, motivo y marca temporal) con código HTTP 200 OK.
- **RF-16:** SI se solicita el historial (`GET /api/incidents/{id}/history`) de un identificador de incidencia que no existe en la base de datos, ENTONCES EL SISTEMA retornará código HTTP 404 (Not Found) con cuerpo normalizado `ErrorResponse`.
- **RF-17:** SI un usuario sin rol `ADMIN` (como `CITIZEN`, `OPERATOR`, `TECHNICIAN` o `ANALYST`) intenta consultar `GET /api/incidents/{id}/history`, ENTONCES EL SISTEMA denegará la petición respondiendo con código HTTP 403 Forbidden y cuerpo normalizado `ErrorResponse`.
- **RF-18:** CUANDO un usuario con rol `ADMIN` abre el modal de detalle de una incidencia en el frontend, EL SISTEMA consultará su historial vía API y renderizará el componente de línea temporal (`Timeline`) mostrando ordenadamente cada hito de cambio de estado.
- **RF-19:** MIENTRAS el usuario activo en sesión no disponga de rol `ADMIN`, EL SISTEMA omitirá la visualización del componente `Timeline` en el modal de detalle de la incidencia.
- **RF-20:** EL SISTEMA estructurará el componente `Timeline` en frontend y la capa de servicios en backend de forma desacoplada y modular, de tal forma que habilitar la visualización del historial al rol `OPERATOR` en futuras iteraciones requiera únicamente actualizar la política de autorización sin refactorizar la lógica central.

### Captura Opcional de Ubicación Actual con Geolocalización en Alta (HU14.4)
- **RF-21:** CUANDO el usuario abre el formulario modal de alta de incidencia, EL SISTEMA mostrará un botón opcional con el texto "Usar mi ubicación actual" e icono de geoposicionamiento en el bloque de coordenadas geográficas.
- **RF-22:** CUANDO el usuario pulsa el botón "Usar mi ubicación actual", EL SISTEMA solicitará al navegador las coordenadas de latitud y longitud del dispositivo.
- **RF-23:** MIENTRAS se procesa la consulta de geolocalización, EL SISTEMA mantendrá deshabilitado el botón "Usar mi ubicación actual".
- **RF-24:** MIENTRAS se procesa la consulta de geolocalización, EL SISTEMA mostrará en dicho botón el texto "Obteniendo ubicación..." acompañado de un indicador animado de actividad.
- **RF-25:** MIENTRAS se procesa la consulta de geolocalización, EL SISTEMA bloqueará el botón de envío del formulario para evitar registros con coordenadas incompletas o transitorias.
- **RF-26:** CUANDO la consulta de geolocalización finaliza satisfactoriamente, EL SISTEMA actualizará los campos de latitud y longitud con los valores obtenidos expresados con 6 decimales.
- **RF-27:** CUANDO se actualizan las coordenadas con la posición actual obtenida, EL SISTEMA desplegará una etiqueta visual informativa con el texto "Ubicación actual obtenida".
- **RF-28:** CUANDO finaliza la consulta de geolocalización, EL SISTEMA restaurará el botón a su estado interactivo original para permitir reintentos posteriores.
- **RF-29:** SI el usuario rechaza o revoca el permiso de geolocalización en su navegador, ENTONCES EL SISTEMA mostrará un mensaje de error contextual en línea bajo las coordenadas indicando que el permiso fue denegado y que puede mantener el punto marcado en el mapa.
- **RF-30:** SI la consulta de geolocalización supera un tiempo límite de 10 segundos sin respuesta, ENTONCES EL SISTEMA cancelará la solicitud y mostrará un mensaje de error contextual en línea indicando que se agotó el tiempo de espera.
- **RF-31:** SI el dispositivo no puede determinar su posición geográfica por falta de cobertura GPS o señal de red, ENTONCES EL SISTEMA mostrará un mensaje de error contextual en línea indicando que la posición no está disponible.
- **RF-32:** SI ocurre cualquier error durante la obtención de la geolocalización, ENTONCES EL SISTEMA preservará intactas las coordenadas que estuviesen previamente cargadas en el formulario sin bloquear la edición de los demás campos.
- **RF-33:** SI el navegador o entorno de ejecución no dispone de soporte para la API de geolocalización, ENTONCES EL SISTEMA presentará el botón "Usar mi ubicación actual" deshabilitado con el mensaje informativo "Geolocalización no disponible en este navegador".
- **RF-34:** CUANDO el usuario completa y envía con éxito el formulario habiendo empleado la ubicación actual, EL SISTEMA cerrará el modal, posicionará el nuevo marcador en dichas coordenadas e iniciará un centrado suave de la cámara del mapa hacia ese punto.
- **RF-35:** SI el usuario cancela o cierra el modal de alta tras haber obtenido su ubicación actual, ENTONCES EL SISTEMA descartará las coordenadas capturadas y mantendrá el encuadre cartográfico previo sin fijar marcadores residuales.

## Requisitos no funcionales
- **Seguridad perimetral y RBAC:** El endpoint de historial está protegido en Spring Security garantizando acceso exclusivo a la autoridad `ROLE_ADMIN`. La identidad del actor en cambios de estado se toma inmutablemente del `SecurityContextHolder` (JWT firmado), imposibilitando la suplantación de identidad (`changed_by_user_id`).
- **Inmutabilidad de auditoría:** Los registros de `status_changes` son estrictamente de solo lectura y append-only; no existen endpoints de modificación (`PUT`/`PATCH`) ni borrado (`DELETE`) sobre el historial.
- **Modularidad y desacoplamiento:** El componente `Timeline` se concibe desacoplado del mapa, permitiendo su inserción en paneles de operador o vistas dedicadas de detalle con mínimo esfuerzo.
- **Rendimiento cartográfico:** El mapa gestiona marcadores de forma ligera sin bloquear el hilo principal del navegador ni provocar re-renderizados innecesarios del lienzo de Leaflet.
- **Accesibilidad en geolocalización:** El botón y las alertas contextuales de error deben incorporar atributos accesibles (`aria-live="polite"`, `role="alert"` en fallos) y contraste cromático superior a 4.5:1 (conforme a WCAG 2.1 AA).
- **Precisión submétrica:** Las coordenadas capturadas con geolocalización deben formatearse con una precisión de 6 dígitos decimales (aproximación de 0.1 metros).
- **Privacidad y consentimiento explícito:** La captura de ubicación se ejecuta de manera estrictamente voluntaria bajo demanda activa del usuario mediante clic; no se efectuarán peticiones automáticas de posición en segundo plano.

## Casos límite
- Incidencia sin transiciones de estado previas: al consultar el historial de una incidencia recién creada, `GET /api/incidents/{id}/history` responderá con una lista vacía `[]` y el `Timeline` mostrará un estado informativo indicando que la incidencia permanece en su estado original de reporte.
- Coordenadas geográficas seleccionadas en los límites de la cartografía: validación de rangos válidos de latitud ([-90, 90]) y longitud ([-180, 180]).
- Token JWT expirado mientras el usuario interactúa con el mapa o envía el formulario: el interceptor de Axios intercepta el 401 y redirige de forma limpia a `/login`.
- Intentos de forzar estados no definidos en el enum del dominio: Hibernate y Bean Validation rechazan la entrada con código HTTP 400 antes de acceder a la base de datos.
- Descarte de selección: pulsar cancelar cierra el modal y no deja marcas residuales en el lienzo de Leaflet.
- Pulsación múltiple o concurrente sobre el botón de geolocalización: el botón permanece deshabilitado mientras se resuelve la posición del GPS, previniendo solicitudes duplicadas al hardware.
- Cancelación prematura del modal durante la consulta GPS: si el usuario cierra el formulario mientras la petición de geolocalización está pendiente, la promesa asíncrona descarta la actualización de estado de forma segura sin provocar advertencias de fuga de memoria.
- Contextos no seguros (sin HTTPS): el sistema detecta la ausencia o restricción de la API y presenta el control deshabilitado con texto explicativo.

## Fuera de alcance
- Adjuntos multimedia (fotografías o vídeos) vinculados a la incidencia o a las transiciones.
- Asignación de incidencias a técnicos y departamentos específicos (`assignments`) y vistas filtradas por técnico asignado.
- Algoritmos de deduplicación geográfica o inferencia automática de proximidad con activos urbanos (`urban_assets`).
- Pantalla global de volcado o exportación de auditoría completa para toda la base de datos.
- Modo de seguimiento continuo o rastreo en tiempo real de la posición del usuario a medida que se desplaza por la ciudad (HU14.4).
- Geocodificación inversa o deducción textual de la dirección física (nombre de calle o número de portal) (HU14.4).
- Inclusión del botón de ubicación actual fuera del formulario modal de alta (HU14.4).

## Criterios de aceptación
- [x] Endpoint `GET /api/incidents` implementado y protegido para devolver la lista de incidencias no terminadas (`status NOT IN ('CLOSED', 'REJECTED')`) para el mapa.
- [x] Endpoint `POST /api/incidents` implementado para dar de alta incidencias capturando el usuario desde el token JWT con estado inicial `REPORTED` y categoría de catálogo.
- [x] Endpoint `PATCH /api/incidents/{id}/status` actualizado para persistir atómicamente el registro en `status_changes` extrayendo el actor del JWT.
- [x] Endpoint `GET /api/incidents/{id}/history` implementado y protegido exclusivamente para el rol `ADMIN`, respondiendo 404 ante ids inexistentes.
- [x] Peticiones de usuarios sin rol `ADMIN` a `GET /api/incidents/{id}/history` son rechazadas con HTTP 403 Forbidden y DTO `ErrorResponse`.
- [x] Vista `/map` implementada con Leaflet en frontend, centrada en Málaga (`[36.7213, -4.4214]`, zoom 13), accesible para usuarios autenticados.
- [x] Marcadores interactivos en el mapa que despliegan Popup con datos básicos y botón "Ver detalle completo".
- [x] Modal de detalle que renderiza la información de la incidencia y, si el usuario es `ADMIN`, incorpora el componente `Timeline` con el historial cronológico.
- [x] Botón "Crear incidencia" / "Cancelar selección" que habilita/deshabilita el modo de captura geográfica con clic en el mapa.
- [x] Formulario de alta con selector desplegable de categoría predefinida y descarte limpio de coordenadas si se cancela.
- [x] Suite de pruebas unitarias y de integración de backend pasando satisfactoriamente (`mvn clean test`).
- [x] Verificación de estilo y sintaxis de frontend sin errores (`npm run lint`).
- [ ] Botón opcional "Usar mi ubicación actual" visible e interactivo en el bloque de coordenadas geográficas del formulario modal de alta (HU14.4).
- [ ] Estado de carga activo durante la petición con texto "Obteniendo ubicación...", icono animado y bloqueo de envío del formulario (HU14.4).
- [ ] Actualización exitosa de los campos de latitud y longitud con 6 decimales y despliegue del distintivo visual "Ubicación actual obtenida" (HU14.4).
- [ ] Manejo contextual de errores en línea bajo el bloque de coordenadas ante permisos denegados, superación del tiempo límite (10s) o señal inaccesible, manteniendo las coordenadas previas (HU14.4).
- [ ] Botón deshabilitado con texto aclaratorio cuando el entorno o navegador no ofrezca compatibilidad con geolocalización (HU14.4).
- [ ] Animación de centrado suave de la vista cartográfica hacia la nueva incidencia al guardar satisfactoriamente el reporte (HU14.4).
- [ ] Descarte limpio de las coordenadas capturadas al cancelar o cerrar el formulario modal (HU14.4).
- [ ] Cobertura completa de pruebas unitarias verificando todos los estados visuales (inactivo, cargando, éxito, error, no soportado) y verificación de lint en verde (HU14.4).

## Dudas abiertas
_Todas las aclaraciones y ambigüedades detectadas por QA han sido resueltas y consensuadas._

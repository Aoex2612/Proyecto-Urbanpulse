# Spec 003 — Mapa de Incidencias con Leaflet y Auditoría Transversal
**Estado:** propuesta

## Contexto y objetivo
Proporcionar a los usuarios de UrbanPulse una experiencia visual interactiva centrada en el territorio mediante un mapa basado en Leaflet, facilitando tanto la visualización de las incidencias reportadas en la ciudad como la captura geolocalizada precisa para dar de alta nuevos avisos. Asimismo, garantizar la trazabilidad transversal y rendición de cuentas del ciclo de vida de cada incidencia mediante el registro inmutable de sus transiciones de estado (`status_changes`) y la visualización de una línea temporal (Timeline) de auditoría restringida a perfiles de administración, diseñada de forma modular para admitir futura extensibilidad a otros roles operativos.

## Usuarios / actores
- **Usuario Autenticado (Ciudadano / Operador / Admin):** Puede acceder a la vista cartográfica `/map` para explorar incidencias existentes en la ciudad y consultar su información pública básica.
- **Ciudadano (`CITIZEN`):** Puede iniciar el reporte de una nueva incidencia pulsando el botón de creación, seleccionando la ubicación exacta en el mapa mediante clic y completando los datos básicos (título, descripción, categoría).
- **Operador (`OPERATOR`):** Puede inspeccionar incidencias en el mapa y ejecutar transiciones de estado en el flujo operativo de triaje.
- **Administrador (`ADMIN`):** Dispone de todas las capacidades anteriores y acceso exclusivo para auditar la traza cronológica completa de transiciones de cualquier incidencia a través del endpoint histórico y del componente visual de línea temporal (`Timeline`).

## Historias de usuario
- **H1 (Visualización Cartográfica con Leaflet):** Como usuario autenticado quiero acceder a una vista de mapa interactivo para ver los marcadores de las incidencias de la ciudad y consultar sus datos básicos al seleccionarlas.
- **H2 (Reporte Geolocalizado Asistido por Mapa):** Como ciudadano quiero activar el modo de creación de incidencia y marcar con un clic el punto exacto en el mapa para asociar automáticamente las coordenadas geográficas al formulario básico del aviso.
- **H3 (Auditoría Inmutable de Estados):** Como sistema quiero persistir un registro histórico auditable cada vez que una incidencia cambie de estado, vinculando automáticamente el usuario responsable a partir de su token JWT.
- **H4 (Consulta Histórica y Línea Temporal):** Como administrador quiero consultar el historial cronológico de cambios de estado de una incidencia para auditar quién realizó cada transición, en qué fecha y bajo qué justificación.

## Requisitos funcionales (criterios de aceptación en EARS)

### Visualización y Consulta en Mapa con Leaflet (H1)
- **RF-1:** CUANDO un usuario autenticado accede a la ruta `/map`, EL SISTEMA renderizará un mapa interactivo con Leaflet y teselas de OpenStreetMap centrado por defecto en la ciudad de Málaga (coordenadas de referencia `[36.7213, -4.4214]` y nivel de zoom urbano 13), obteniendo vía `GET /api/incidents` la lista de incidencias activas y posicionando un marcador en las coordenadas de cada una.
- **RF-2:** CUANDO el usuario hace clic sobre el marcador de una incidencia en el mapa, EL SISTEMA desplegará los detalles básicos del reporte (título, descripción, categoría y estado actual).
- **RF-3:** SI el mapa no puede cargar las teselas de OpenStreetMap por fallo de red, ENTONCES EL SISTEMA mantendrá la interfaz operativa mostrando un mensaje de advertencia no bloqueante sin interrumpir la sesión del usuario.

### Reporte Geolocalizado desde el Mapa (H2)
- **RF-4:** CUANDO el usuario autenticado pulsa el botón "Crear incidencia" en la vista del mapa, EL SISTEMA habilitará el modo de selección geográfica solicitando al usuario que marque el punto del incidente en el mapa.
- **RF-5:** CUANDO el usuario hace clic sobre el mapa en modo de selección geográfica, EL SISTEMA capturará la latitud y longitud del punto seleccionado y abrirá el formulario modal de alta con dichas coordenadas prefijadas.
- **RF-6:** CUANDO el usuario completa y envía el formulario con título, descripción, categoría y las coordenadas capturadas, EL SISTEMA emitirá una petición `POST /api/incidents` persistiendo la incidencia con estado inicial `REPORTED`, prioridad `MEDIUM` por defecto, fecha de creación actual y `citizen_id` obtenido del token JWT del usuario emisor.
- **RF-7:** CUANDO la creación de la incidencia concluye con éxito, EL SISTEMA responderá con código HTTP 201 Created, cerrará el formulario modal e incorporará de inmediato el nuevo marcador al mapa interactivo.
- **RF-8:** SI el usuario intenta enviar el formulario de reporte omitiendo el título, la descripción, la categoría o sin haber seleccionado coordenadas en el mapa, ENTONCES EL SISTEMA bloqueará el envío y notificará los errores de validación.

### Registro y Auditoría Transversal de Transiciones (H3)
- **RF-9:** CUANDO un usuario autorizado (`OPERATOR` o `ADMIN`) ejecuta una modificación de estado a través de `PATCH /api/incidents/{id}/status`, EL SISTEMA actualizará el estado de la incidencia e insertará de forma transaccional un registro en la tabla `status_changes` conteniendo el identificador de la incidencia, el estado previo (`from_status`), el estado resultante (`to_status`), el motivo o comentario justificado (`reason`), la fecha/hora actual y el identificador del usuario actor extraído de su token JWT (`changed_by_user_id`).
- **RF-10:** SI se invoca la actualización de estado sobre una incidencia inexistente en la base de datos, ENTONCES EL SISTEMA retornará código HTTP 404 (Not Found) con cuerpo normalizado `ErrorResponse`.

### Consulta Histórica y Componente Timeline (H4)
- **RF-11:** CUANDO un usuario con rol `ADMIN` solicita el historial de una incidencia mediante `GET /api/incidents/{id}/history`, EL SISTEMA retornará una lista cronológica ordenada ascendentemente por fecha con las transiciones de estado registradas en `status_changes` (id, estado anterior, estado nuevo, nombre/email del usuario actor, motivo y marca temporal) con código HTTP 200 OK.
- **RF-12:** SI un usuario sin rol `ADMIN` (como `CITIZEN` u `OPERATOR`) intenta consultar `GET /api/incidents/{id}/history`, ENTONCES EL SISTEMA denegará la petición respondiendo con código HTTP 403 Forbidden y cuerpo normalizado `ErrorResponse`.
- **RF-13:** CUANDO un usuario con rol `ADMIN` consulta la vista o modal de detalle de una incidencia en el frontend, EL SISTEMA solicitará su historial al backend y renderizará el componente de línea temporal (`Timeline`) mostrando las etapas cronológicas de cambio de estado.
- **RF-14:** MIENTRAS el usuario activo en sesión no disponga de rol `ADMIN`, EL SISTEMA omitirá la visualización del componente `Timeline` en la vista de detalle de la incidencia.
- **RF-15:** EL SISTEMA estructurará el componente `Timeline` en frontend y la capa de servicios en backend de forma desacoplada y modular, de tal forma que habilitar la visualización del historial al rol `OPERATOR` en futuras iteraciones requiera únicamente actualizar la política de autorización sin refactorizar la lógica central.

## Requisitos no funcionales
- **Seguridad perimetral y RBAC:** El endpoint de historial está protegido en Spring Security garantizando acceso exclusivo a la autoridad `ROLE_ADMIN`. La identidad del actor en cambios de estado se toma inmutablemente del `SecurityContextHolder` (JWT firmado), imposibilitando la suplantación de identidad (`changed_by_user_id`).
- **Inmutabilidad de auditoría:** Los registros de `status_changes` son estrictamente de solo lectura y append-only; no existen endpoints de modificación (`PUT`/`PATCH`) ni borrado (`DELETE`) sobre el historial.
- **Modularidad y extensibilidad:** El componente `Timeline` se concibe desacoplado del mapa, permitiendo su inserción en paneles de operador o vistas dedicadas de detalle con mínimo esfuerzo.
- **Rendimiento cartográfico:** El mapa gestiona marcadores de forma ligera sin bloquear el hilo principal del navegador ni provocar re-renderizados innecesarios del lienzo de Leaflet.

## Casos límite
- Incidencia sin transiciones de estado previas: al consultar el historial de una incidencia recién creada, `GET /api/incidents/{id}/history` responderá con una lista vacía `[]` y el `Timeline` mostrará un estado informativo indicando que la incidencia permanece en su estado original de reporte.
- Coordenadas geográficas seleccionadas en los límites de la cartografía: validación de rangos válidos de latitud ([-90, 90]) y longitud ([-180, 180]).
- Token JWT expirado mientras el usuario interactúa con el mapa o envía el formulario: el interceptor de Axios intercepta el 401 y redirige de forma limpia a `/login`.
- Intentos de forzar estados no definidos en el enum del dominio: Hibernate y Bean Validation rechazan la entrada con código HTTP 400 antes de acceder a la base de datos.

## Fuera de alcance
- Adjuntos multimedia (fotografías o vídeos) vinculados a la incidencia o a las transiciones.
- Asignación de incidencias a técnicos y departamentos específicos (`assignments`).
- Algoritmos de deduplicación geográfica o inferencia automática de proximidad con activos urbanos (`urban_assets`).
- Pantalla global de volcado o exportación de auditoría completa para toda la base de datos.

## Criterios de aceptación
- [ ] Endpoint `GET /api/incidents` implementado y protegido para devolver la lista de incidencias para el mapa.
- [ ] Endpoint `POST /api/incidents` implementado para dar de alta incidencias capturando el usuario desde el token JWT con estado inicial `REPORTED`.
- [ ] Endpoint `PATCH /api/incidents/{id}/status` actualizado para persistir atómicamente el registro en `status_changes` extrayendo el actor del JWT.
- [ ] Endpoint `GET /api/incidents/{id}/history` implementado y protegido exclusivamente para el rol `ADMIN`.
- [ ] Peticiones de usuarios sin rol `ADMIN` a `GET /api/incidents/{id}/history` son rechazadas con HTTP 403 Forbidden y DTO `ErrorResponse`.
- [ ] Vista `/map` implementada con Leaflet en frontend, accesible para usuarios autenticados desde la barra de navegación.
- [ ] Marcadores interactivos en el mapa que despliegan la información básica de cada incidencia.
- [ ] Botón "Crear incidencia" que habilita el modo de selección geográfica con clic en el mapa y abre el formulario de registro rápido.
- [ ] Componente visual de línea temporal (`Timeline`) integrado en la vista de detalle de la incidencia para usuarios con rol `ADMIN`.
- [ ] Suite de pruebas unitarias y de integración de backend pasando satisfactoriamente (`mvn clean test`).
- [ ] Verificación de estilo y sintaxis de frontend sin errores (`npm run lint`).

## Dudas abiertas
_Todas las dudas planteadas durante la entrevista inicial han sido consensuadas y resueltas con el usuario._

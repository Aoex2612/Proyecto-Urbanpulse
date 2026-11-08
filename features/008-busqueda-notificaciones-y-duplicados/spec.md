# Spec 008 — Búsqueda, Notificaciones y Duplicados
**Estado:** propuesta 📝

## Contexto y objetivo
Aportar capacidades avanzadas de localización de incidencias mediante búsqueda y filtrado multicriterio adaptado por roles, optimizar la carga de trabajo municipal mediante la detección automática y sugerencia de reportes duplicados, y mantener informados en tiempo real tanto a la ciudadanía como al personal operativo sobre la evolución del ciclo de vida de los avisos mediante un centro de notificaciones integrado.

## Usuarios / actores
- **Ciudadano (`CITIZEN`):** Puede buscar y filtrar incidencias en el mapa interactivo por texto libre, categoría y fechas (sin acceso al filtro por estado de triaje). Dispone de un centro de notificaciones con icono de campana en la barra de navegación para recibir avisos cuando sus incidencias cambian de estado, abrirlas directamente y marcarlas como leídas. Puede seguir reportando nuevas incidencias libremente incluso si alguna anterior fue clasificada como duplicada.
- **Operador Municipal (`OPERATOR`):** Puede aplicar búsqueda y filtrado multicriterio completo (incluyendo estado) en el mapa y en el panel de operador. Recibe notificaciones automáticas ante posibles duplicados y cambios de estado. Puede confirmar duplicados asignándoles el estado `REJECTED` con motivo de duplicidad.
- **Técnico (`TECHNICIAN`):** Accede al filtrado multicriterio completo en el mapa y en su panel de asignaciones. Recibe notificaciones de incidencias duplicadas y cambios de estado en las intervenciones donde participa.
- **Administrador (`ADMIN`) y Analista (`ANALYST`):** Acceden al conjunto completo de filtros multicriterio en todas las vistas de gestión y reciben notificaciones del sistema relativas a los estados de las incidencias.

## Historias de usuario
- **H1 (HU19 - Búsqueda y Filtrado Multicriterio):** Como usuario de la plataforma quiero filtrar y buscar incidencias por múltiples criterios combinables tanto en el mapa como en los paneles de gestión según mis permisos para localizar rápidamente avisos relevantes.
- **H2 (HU20 - Detección y Sugerencia de Incidencias Duplicadas):** Como operador o técnico quiero que el sistema me alerte de incidencias potencialmente duplicadas por proximidad espacial, ventana temporal y coincidencia semántica para evitar duplicidad de intervenciones y poder marcarlas como rechazadas.
- **H3 (HU21 - Notificaciones de Cambio de Estado):** Como usuario involucrado en una incidencia quiero recibir notificaciones en una bandeja desplegable cuando ocurra un cambio de estado para dar seguimiento a la resolución y poder marcar los avisos como leídos.

## Requisitos funcionales (criterios de aceptación en EARS)

### Búsqueda y Filtrado Multicriterio (H1 - HU19)
- **RF-1:** CUANDO el usuario introduce una cadena de texto en el buscador, EL SISTEMA filtrará las incidencias cuyo título o descripción contengan dicho texto ignorando mayúsculas y acentos.
- **RF-2:** CUANDO el usuario selecciona uno o varios criterios de filtro (categoría, rango de fechas de creación o texto), EL SISTEMA aplicará la intersección de todas las condiciones activas en la vista del mapa interactivo y en la tabla del panel de operador.
- **RF-3:** DONDE el usuario posea un rol interno (`OPERATOR`, `TECHNICIAN`, `ADMIN` o `ANALYST`), EL SISTEMA habilitará el selector de filtrado por estado de triaje de la incidencia.
- **RF-4:** DONDE el usuario posea el rol de ciudadano (`CITIZEN`), EL SISTEMA ocultará y bloqueará el filtro por estado de incidencia.
- **RF-5:** CUANDO el usuario pulsa la opción de restablecer filtros, EL SISTEMA restaurará la visualización completa de incidencias sin ningún criterio restrictivo aplicado.
- **RF-6:** SI ningún registro coincide con la combinación de filtros aplicada, ENTONCES EL SISTEMA mostrará un mensaje descriptivo de ausencia de resultados con una acción rápida para limpiar los filtros.

### Detección y Sugerencia de Duplicados (H2 - HU20)
- **RF-7:** CUANDO se registra o consulta una incidencia, EL SISTEMA evaluará si existen otras incidencias activas en un radio geográfico menor o igual a 60 metros, registradas en una ventana de tiempo inferior o igual a 48 horas (2 días), con la misma categoría y con coincidencia semántica en título o descripción.
- **RF-8:** CUANDO se detecta una coincidencia que cumpla los tres criterios de duplicidad (espacial $\le 60\text{ m}$, temporal $\le 48\text{ h}$ y semántico de categoría y texto), EL SISTEMA generará una notificación de sugerencia de duplicado dirigida exclusivamente a los roles de operador y técnico.
- **RF-9:** CUANDO un operador o técnico confirma una incidencia sugerida como duplicada, EL SISTEMA actualizará su estado a `REJECTED` registrando en la auditoría el motivo de rechazo por duplicidad y vinculando la referencia de la incidencia original.
- **RF-10:** EL SISTEMA permitirá que el ciudadano autor de una incidencia rechazada por duplicado continúe reportando nuevas incidencias con total normalidad sin aplicar bloqueos a su cuenta.
- **RF-11:** SI dos incidencias distan más de 60 metros, su diferencia de registro excede las 48 horas o no coinciden en categoría y texto, ENTONCES EL SISTEMA las tratará como incidencias independientes sin emitir alertas de duplicidad.

### Notificaciones de Cambio de Estado (H3 - HU21)
- **RF-12:** CUANDO se produzca una transición válida en el ciclo de vida de una incidencia (cambio de estado), EL SISTEMA creará y persistirá un registro de notificación para el ciudadano que reportó la incidencia y para todo el personal técnico y operativo asignado a ella.
- **RF-13:** EL SISTEMA mostrará en la barra de navegación superior un componente desplegable con icono de campana indicando mediante un badge numérico la cantidad de notificaciones pendientes de lectura del usuario autenticado.
- **RF-14:** CUANDO el usuario despliega la bandeja de notificaciones, EL SISTEMA listará los avisos recibidos ordenados cronológicamente de forma descendente indicando mensaje, fecha y estado de lectura.
- **RF-15:** CUANDO el usuario hace clic sobre una notificación en el desplegable, EL SISTEMA abrirá la ficha modal de detalle de la incidencia vinculada para su consulta completa.
- **RF-16:** CUANDO el usuario pulsa la acción de marcar como leída en una notificación individual, EL SISTEMA actualizará su estado a leída y decrementará en tiempo real el contador del badge.
- **RF-17:** SI el usuario no tiene ninguna notificación registrada en su bandeja, ENTONCES EL SISTEMA presentará un estado visual vacío con el mensaje "No tienes notificaciones pendientes".

## Requisitos no funcionales
- **Control de Acceso y Privacidad (RBAC):** Cada usuario solo puede consultar y marcar como leídas las notificaciones dirigidas a su propia cuenta de usuario. Las alertas de duplicados se aíslan estrictamente para operadores y técnicos.
- **Rendimiento de Búsqueda:** El filtrado multicriterio en el mapa interactivo y en el panel de operador debe resolver y refrescar los resultados visibles en un tiempo inferior a 400 ms.
- **Persistencia e Integridad:** Las notificaciones se modelan mediante una entidad relacional persistente en PostgreSQL vinculada a la incidencia y al usuario receptor, preservando trazabilidad incluso ante desconexión o recarga de página.
- **Usabilidad y Accesibilidad:** La bandeja desplegable de notificaciones y la barra de filtros multicriterio deben ser adaptables a pantallas móviles y de escritorio, cumpliendo contrastes legibles y atributos de accesibilidad (`aria-label`, roles semánticos).

## Casos límite
- **Múltiples incidencias en el mismo punto exacto:** Si existen más de dos incidencias dentro del radio de 60 metros y 48 horas con la misma categoría, el sistema detecta y asocia todas las coincidencias potenciales sin generar bucles infinitos.
- **Búsqueda con caracteres especiales o espacios vacíos:** Términos de búsqueda con espacios redundantes o signos de puntuación se normalizan y no provocan errores en el filtrado.
- **Incidencia eliminada o cancelada:** Si una incidencia es consultada desde una notificación y ya no está disponible, el sistema informa con un mensaje descriptivo sin bloquear la navegación.
- **Bandeja de notificaciones con volumen elevado:** El listado desplegable gestiona scroll interno con un límite visual razonable para evitar desbordamiento del viewport.

## Fuera de alcance
- Botón global para "Marcar todas como leídas" simultáneamente (diferido explícitamente a versiones posteriores).
- Fusión física destructiva de registros en base de datos (la incidencia duplicada se mantiene en el historial con estado `REJECTED`).
- Envío de notificaciones externas mediante SMS, WhatsApp o push móviles nativas (APNs/FCM).
- Algoritmos de inteligencia artificial con redes neuronales o embeddings vectoriales para la similitud semántica (se implementará concordancia léxica y normalización de texto).

## Criterios de aceptación
- [ ] La barra de filtros permite buscar por texto libre, categoría y fechas tanto en el mapa como en el panel de operador.
- [ ] El filtro por estado de incidencia está accesible para roles de gestión interna y completamente oculto para ciudadanos.
- [ ] Ante búsquedas sin coincidencias se muestra una vista vacía con botón para limpiar filtros.
- [ ] El sistema detecta y notifica sugerencias de duplicados exclusivamente a operadores y técnicos ante avisos a $\le 60\text{ m}$, $\le 48\text{ h}$, misma categoría y texto afín.
- [ ] El operador o técnico puede confirmar el duplicado marcando la incidencia como `REJECTED` con su motivo.
- [ ] La creación de reportes por parte del ciudadano no se bloquea tras un rechazo por duplicidad.
- [ ] Los cambios de estado de una incidencia generan notificaciones persistentes para el creador y el personal asignado.
- [ ] La campana de la barra de navegación muestra el badge de no leídas y despliega el listado cronológico.
- [ ] Pulsar en una notificación abre el modal de detalle de la incidencia.
- [ ] Cada notificación puede marcarse individualmente como leída actualizando el badge en tiempo real.
- [ ] Ausencia de notificaciones muestra el estado vacío informativo.

## Dudas abiertas
- Ninguna duda abierta tras la entrevista de requisitos. La especificación está lista para aprobación.

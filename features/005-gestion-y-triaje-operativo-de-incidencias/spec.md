# Spec 005 — Gestión y Triaje Operativo de Incidencias

**Estado:** propuesta

## Contexto y objetivo
Actualmente, las incidencias reportadas por la ciudadanía quedan en estado inicial sin un mecanismo formal para que el personal municipal evalúe su validez, determine su urgencia, coordine a los departamentos técnicos competentes y supervise su resolución. Esta funcionalidad proporciona a los operadores municipales y técnicos un flujo integral de triaje operativo, asignación departamental multidisciplinar, registro de intervenciones, gestión del ciclo de vida (desde validación hasta resolución y cierre) y colaboración mediante notas internas y avisos públicos con plena trazabilidad auditable.

## Usuarios / actores
- **Operador municipal (`OPERATOR`):** Responsable de evaluar reportes, validar o rechazar incidencias, estimar o ajustar prioridades, coordinar asignaciones a departamentos y técnicos, y supervisar el cierre o reapertura.
- **Técnico municipal (`TECHNICIAN`):** Especialista municipal que accede al panel de operaciones con vista restringida exclusivamente a las incidencias donde dispone de una asignación activa (`is_active = true`), registra el inicio de los trabajos en vía pública, documenta la intervención realizada, añade notas técnicas y declara la incidencia resuelta.
- **Administrador (`ADMIN`):** Dispone de la totalidad de permisos de supervisión, triaje, asignación, auditoría operativa y rectificación de cualquier incidencia o comentario. Posee facultades universales para ejecutar todas las transiciones del ciclo de vida, incluyendo iniciar trabajos técnicos (`start-work`) y resolver incidencias (`resolve`) sin requerir asignación técnica previa.
- **Ciudadano (`CITIZEN`):** Consulta el estado de sus incidencias y visualiza únicamente los comentarios marcados expresamente como públicos (`is_internal = false`); no tiene permisos para ejecutar acciones en el triaje ni añadir comentarios.

## Historias de usuario
- **HU07 (Validación o Rechazo de Reportes):** Como operador municipal, quiero validar o rechazar reportes ciudadanos dejando constancia del motivo, para filtrar reportes improcedentes o canalizar aquellos que son competencia municipal.
- **HU08 (Asignación a Departamento o Técnico):** Como operador municipal, quiero asignar una incidencia a uno o varios departamentos y técnicos correspondientes, para delegar formalmente la resolución de la incidencia.
- **HU09 (Intervención Técnica y Resolución):** Como técnico asignado o administrador, quiero cambiar el estado a "en progreso" al iniciar la actuación y a "resuelta" al culminarla, aportando el detalle de las actuaciones, para reflejar el avance real del trabajo operativo.
- **HU10 (Reapertura o Cierre Definitivo):** Como operador municipal, quiero verificar las incidencias resueltas para cerrarlas definitivamente o reabrirlas si la reparación fue insuficiente, garantizando la calidad del servicio.
- **HU11 (Muro de Comentarios y Notas Técnicas Internas):** Como operador o técnico, quiero registrar y rectificar notas internas de coordinación o avisos públicos, para mantener comunicación continua del avance operativo y mantener informado al ciudadano.
- **HU12 (Priorización Manual y Asistida):** Como operador municipal, quiero recibir una recomendación automática de prioridad basada en el impacto en la seguridad y cortes de vía con opción de ajuste manual, para agilizar la toma de decisiones objetiva ante situaciones críticas.

## Requisitos funcionales (criterios de aceptación en EARS)

### Priorización Manual y Asistida (HU12)
- RF-1: CUANDO el operador evalúa el impacto proporcionando el indicador de corte de calle (`true`/`false`) y el nivel discreto de afectación a la seguridad (`NONE`, `LOW`, `HIGH`), EL SISTEMA calculará la sugerencia de prioridad bajo la siguiente matriz determinista:
  - Corte de calle = `true` Y Seguridad = `HIGH` → Sugerencia: `CRITICAL`.
  - Corte de calle = `true` Y Seguridad ∈ (`LOW`, `NONE`) → Sugerencia: `HIGH`.
  - Corte de calle = `false` Y Seguridad = `HIGH` → Sugerencia: `HIGH`.
  - Corte de calle = `false` Y Seguridad = `LOW` → Sugerencia: `MEDIUM`.
  - Corte de calle = `false` Y Seguridad = `NONE` → Sugerencia: `LOW`.
  Acompañando siempre una justificación textual explicativa generada automáticamente.
- RF-2: CUANDO el operador confirma la sugerencia o define un valor distinto, EL SISTEMA fijará la prioridad definitiva (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) y registrará la justificación textual (`priority_reason`) en la incidencia.
- RF-3: SI un usuario sin rol `OPERATOR` ni `ADMIN` intenta calcular o modificar la prioridad de una incidencia, ENTONCES EL SISTEMA denegará la petición con un error 403 de autorización.

### Validación y Rechazo de Reportes (HU07)
- RF-4: CUANDO un operador valida un reporte en estado `REPORTED`, EL SISTEMA transicionará su estado a `VALIDATED` y registrará el cambio en la auditoría (`status_changes`) con el identificador del usuario, la marca temporal en UTC y el motivo opcional.
- RF-5: CUANDO un operador rechaza un reporte aportando un motivo explicativo de al menos 5 caracteres no vacíos, EL SISTEMA transicionará la incidencia a `REJECTED`, desactivará todas sus asignaciones activas si las hubiera (`is_active = false`) y guardará el motivo en `status_changes`.
- RF-6: SI un operador intenta rechazar una incidencia sin proporcionar motivo o con una longitud menor a 5 caracteres no vacíos, ENTONCES EL SISTEMA bloqueará la operación informando del error de validación y mantendrá el estado previo.
- RF-7: CUANDO un operador o administrador reevalúa una incidencia en estado `REJECTED` aportando el motivo de rectificación, EL SISTEMA transicionará la incidencia a `REPORTED` o a `VALIDATED` según decida el operador, registrando la reactivación en la auditoría.

### Asignación a Departamentos y Técnicos (HU08)
- RF-8: CUANDO un operador asigna una incidencia en estado `VALIDATED` o `REOPENED` a uno o varios departamentos, EL SISTEMA creará los registros correspondientes en `assignments` con `is_active = true` y transicionará el estado de la incidencia a `ASSIGNED`.
- RF-9: CUANDO un operador asigna uno o varios técnicos específicos a una incidencia, EL SISTEMA comprobará que cada técnico pertenezca a alguno de los departamentos actualmente asignados a la incidencia y vinculará al técnico a la asignación activa.
- RF-10: SI un operador intenta asignar a una incidencia un técnico que no pertenece a ninguno de los departamentos asignados a dicha incidencia, ENTONCES EL SISTEMA denegará la asignación indicando el error de inconsistencia de departamento.
- RF-11: CUANDO se desasigna un técnico o departamento de una incidencia, EL SISTEMA marcará dicha asignación como inactiva (`is_active = false`).
- RF-12: SI tras una desasignación la incidencia se queda sin ningún departamento activo asignado, ENTONCES EL SISTEMA revertirá automáticamente el estado de la incidencia de `ASSIGNED` a `VALIDATED`.

### Intervención Técnica y Resolución (HU09)
- RF-13: CUANDO un técnico con asignación activa sobre la incidencia O un usuario con rol `ADMIN` inicia las tareas operativas, EL SISTEMA transicionará el estado de la incidencia desde `ASSIGNED` o `REOPENED` hacia `IN_PROGRESS`, registrando el evento en `status_changes` con la identidad del usuario actuante.
- RF-14: SI un usuario que no figure como técnico asignado activo en la incidencia Y no posea rol `ADMIN` intenta transicionarla a `IN_PROGRESS`, ENTONCES EL SISTEMA denegará la acción informando de que no es un técnico asignado a esta incidencia ni posee privilegios de administración.
- RF-15: CUANDO un técnico con asignación activa O un usuario con rol `ADMIN` marca una incidencia como resuelta (`RESOLVED`) aportando una descripción de los trabajos ejecutados de al menos 10 caracteres no vacíos (o descripción opcional si la prioridad es `LOW`), EL SISTEMA cambiará el estado a `RESOLVED` y registrará la descripción y el cambio de estado en la auditoría.
- RF-16: SI un usuario (técnico asignado o administrador) intenta pasar a `RESOLVED` una incidencia con prioridad `MEDIUM`, `HIGH` o `CRITICAL` con una descripción de resolución vacía o de longitud menor a 10 caracteres no vacíos, ENTONCES EL SISTEMA bloqueará la transición requiriendo el detalle obligatorio de las actuaciones.

### Reapertura o Cierre Definitivo (HU10)
- RF-17: CUANDO un operador o administrador verifica la resolución satisfactoria de una incidencia en estado `RESOLVED`, EL SISTEMA transicionará su estado a `CLOSED`, desactivará todas las asignaciones activas existentes (`is_active = false`) y registrará el cierre en `status_changes`.
- RF-18: CUANDO un operador o administrador determina que una resolución en estado `RESOLVED` ha sido insatisfactoria y aporta un motivo de al menos 5 caracteres no vacíos, EL SISTEMA transicionará la incidencia a `REOPENED`, conservando el motivo en la auditoría y habilitando nuevas intervenciones operativas.
- RF-19: SI un usuario con rol `TECHNICIAN` o `CITIZEN` intenta ejecutar el cierre (`CLOSED`) o la reapertura (`REOPENED`) de una incidencia, ENTONCES EL SISTEMA denegará la operación por falta de privilegios de supervisión.

### Muro de Comentarios y Notas Técnicas (HU11)
- RF-20: MIENTRAS la incidencia NO se encuentre en estado terminal (`CLOSED` ni `REJECTED`), EL SISTEMA permitirá a usuarios con rol `OPERATOR`, `TECHNICIAN` o `ADMIN` publicar notas técnicas internas (`is_internal = true`) y comentarios públicos (`is_internal = false`).
- RF-21: EL SISTEMA restringirá la visualización de los comentarios con `is_internal = true` únicamente a personal municipal autenticado (`OPERATOR`, `TECHNICIAN`, `ADMIN`), impidiendo su entrega o acceso a usuarios con rol `CITIZEN`.
- RF-22: CUANDO el autor original de un comentario (o un usuario con rol `ADMIN`) edita el texto de un comentario en una incidencia no cerrada, EL SISTEMA actualizará el contenido del comentario, registrará la marca temporal de actualización (`updated_at`) y conservará el autor original del registro.
- RF-23: SI un usuario que no es el autor original ni posee rol `ADMIN` intenta editar un comentario, ENTONCES EL SISTEMA denegará la operación indicando error de permisos de edición.
- RF-24: SI un usuario intenta añadir o editar comentarios en una incidencia en estado `CLOSED` o `REJECTED`, ENTONCES EL SISTEMA rechazará la acción informando de que el expediente está finalizado.

### Visualización Segregada de Asignaciones Técnicas (HU09)
- RF-25: CUANDO un usuario con rol `TECHNICIAN` consulta el catálogo de incidencias o accede al panel de operaciones, EL SISTEMA filtrará y entregará únicamente aquellas incidencias en las que dicho técnico figure con asignación activa (`is_active = true`), impidiendo que visualice incidencias asignadas a otros técnicos o incidencias sin asignar.

## Requisitos no funcionales
- **Seguridad y Control de Acceso (RBAC):** Las operaciones de cambio de estado, asignación y comentarios internos deben estar estrictamente validadas en el backend en base a las cabeceras de autorización y el token JWT autenticado.
- **Trazabilidad e Inmutabilidad de Auditoría:** Cada cambio de estado de la incidencia debe generar un registro auditable e inmutable en `status_changes`, preservando estado previo, nuevo estado, usuario que lo ejecutó, motivo opcional/obligatorio y marca temporal en UTC.
- **Rendimiento:** Las operaciones de cálculo de prioridad asistida y cambio de estado deben responder en menos de 200 ms bajo condiciones normales de carga.
- **Internacionalización y Formato:** Todos los mensajes de validación y motivos de auditoría deben registrarse en español.

## Casos límite
- **Rechazo sin motivo:** Intento de rechazar una incidencia enviando cadenas vacías, nulas o con menos de 5 caracteres útiles; bloqueado por validación.
- **Asignación de técnico no perteneciente al departamento:** Asignar un técnico que no pertenece a ninguno de los departamentos asignados a la incidencia; bloqueado con código 400/422 y mensaje explicativo.
- **Desasignación completa:** Al retirar el último departamento activo de una incidencia en `ASSIGNED`, el sistema debe volver limpiamente al estado `VALIDATED`.
- **Cierre o rechazo con asignaciones preexistentes:** Al cerrar (`CLOSED`) o rechazar (`REJECTED`), todas las asignaciones activas se desactivan automáticamente (`is_active = false`).
- **Retorno a la intervención desde reapertura:** Un técnico asignado retoma una incidencia reabierta (`REOPENED`) transicionándola directamente a `IN_PROGRESS`.
- **Intento de comentar en incidencias cerradas o rechazadas:** El sistema bloquea altas y ediciones de comentarios si el estado es `CLOSED` o `REJECTED`.
- **Incidencias de prioridad menor (`LOW`) sin texto de resolución:** Transición de `IN_PROGRESS` a `RESOLVED` permitida sin descripción obligatoria para facilitar agilidad operativa en tareas simples.
- **Intervención técnica ejecutada por Administrador:** Un usuario con rol `ADMIN` puede iniciar trabajo (`IN_PROGRESS`) y resolver incidencias (`RESOLVED`) directamente sin requerir asignación técnica previa.
- **Técnico sin incidencias asignadas:** Si un técnico autenticado no cuenta con asignaciones activas en el sistema, la cola del panel operativo se muestra vacía indicando que no tiene actuaciones asignadas pendientes.

## Fuera de alcance
- Detección automática y semántica de incidencias duplicadas (planificado en Feature 008, HU19-HU21).
- Asociación a activos urbanos físicos o ingesta de sensores contextuales externos (planificado en Feature 007, HU16-HU18).
- Acciones interactivas o comentarios por parte del rol `CITIZEN` dentro del triaje.
- Envío de notificaciones externas (correo electrónico, SMS o Web Push) ante cambios de estado (diferido a subsistema de notificaciones).

## Criterios de aceptación
- [ ] La matriz determinista de prioridad asistida calcula y sugiere la prioridad correcta y genera la justificación para todas las combinaciones de corte de calle y nivel de seguridad.
- [ ] Un operador puede fijar o modificar la prioridad con su correspondiente motivo.
- [ ] Un operador puede validar una incidencia (`REPORTED` → `VALIDATED`) con registro auditable.
- [ ] Un operador puede rechazar una incidencia (`REPORTED` → `REJECTED`) con motivo obligatorio de ≥ 5 caracteres, desactivando asignaciones activas, y puede reevaluarla si fue un error.
- [ ] Una incidencia validada o reabierta puede asignarse a múltiples departamentos y a múltiples técnicos pertenecientes a los departamentos asignados.
- [ ] Si se desasignan todos los departamentos activos, la incidencia vuelve automáticamente a `VALIDATED`.
- [ ] Los técnicos asignados o los usuarios con rol `ADMIN` pueden pasar la incidencia a `IN_PROGRESS` (tanto desde `ASSIGNED` como desde `REOPENED`).
- [ ] Un técnico asignado o un usuario con rol `ADMIN` puede marcar la incidencia como `RESOLVED`, requiriendo descripción de resolución de ≥ 10 caracteres para prioridades `MEDIUM`, `HIGH` y `CRITICAL` (opcional en `LOW`).
- [ ] Solo operadores y administradores pueden cerrar (`CLOSED`) o reabrir (`REOPENED`) una incidencia resuelta, registrándose el motivo de reapertura (≥ 5 caracteres) en la auditoría y desactivando asignaciones al cerrar.
- [ ] Operadores y técnicos pueden publicar y editar comentarios internos y públicos mientras la incidencia no esté cerrada ni rechazada.
- [ ] Los comentarios internos nunca son devueltos ni visibles al rol `CITIZEN`.
- [ ] La edición de un comentario registra la marca temporal de actualización (`updated_at`) y preserva la autoría original.
- [ ] Los usuarios con rol `TECHNICIAN` visualizan en el panel operativo y reciben vía API únicamente aquellas incidencias en las que tienen una asignación activa (`is_active = true`).
- [ ] Todos los RF cuentan con tests unitarios y de integración en verde tanto en backend como en frontend.

## Dudas abiertas
_No quedan dudas abiertas tras la clarificación y resolución de ambigüedades._

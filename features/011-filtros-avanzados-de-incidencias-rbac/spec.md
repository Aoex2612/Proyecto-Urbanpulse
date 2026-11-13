# Spec 011 — Filtros avanzados de incidencias con control de acceso RBAC
**Estado:** aprobada

## Contexto y objetivo
Actualmente, el sistema permite filtrar incidencias por texto libre, categoría, rango de fechas y estado (este último restringido por rol). Sin embargo, los operadores y técnicos municipales necesitan localizar incidencias según su nivel de urgencia/prioridad técnica y el departamento municipal responsable, mientras que tanto ciudadanos como personal interno requieren acotar la búsqueda al distrito geográfico municipal. Esta funcionalidad añade los filtros de prioridad, departamento y distrito, manteniendo estrictamente el control de acceso por roles (RBAC) para preservar la confidencialidad operativa interna.

## Usuarios / actores
- **Ciudadano (`CITIZEN`) / Visitante anónimo**: Usuario público que consulta incidencias en el mapa o en su panel personal. Puede filtrar por texto, categoría, rango de fechas y distrito. Los usuarios no autenticados reciben idéntico trato y restricciones que `CITIZEN`.
- **Operador municipal (`OPERATOR`) / Administrador (`ADMIN`)**: Personal municipal que gestiona el triaje, clasificación y asignación de incidencias. Tiene acceso a todos los filtros, incluidos estado, prioridad técnica y departamento.
- **Técnico de cuadrilla (`TECHNICIAN`)**: Personal operativo asignado a la resolución de incidencias. Accede a los filtros avanzados para localizar las tareas de su departamento y prioridad.
- **Analista urbano (`ANALYST`)**: Personal encargado del seguimiento y métricas urbanas, con acceso a filtros operativos para auditoría.

## Historias de usuario
- **H1 (Distrito público)**: Como ciudadano u operador, quiero filtrar las incidencias por distrito municipal para consultar únicamente las incidencias de una zona geográfica concreta.
- **H2 (Prioridad operativa)**: Como operador municipal, quiero filtrar las incidencias por su nivel de prioridad para priorizar la atención de aquellas críticas o de alta urgencia.
- **H3 (Departamento municipal)**: Como operador o técnico, quiero filtrar incidencias por departamento asignado (o sin asignar) para organizar la carga de trabajo por competencia municipal.
- **H4 (Protección RBAC de filtros internos)**: Como responsable de seguridad y privacidad del sistema, quiero que los ciudadanos y visitantes no puedan visualizar ni utilizar los filtros de prioridad y departamento, ignorando silenciosamente estos criterios si son enviados forzosamente por API.
- **H5 (Limpieza integral de criterios)**: Como cualquier usuario de la plataforma, quiero un botón para limpiar todos los filtros activos y recuperar la vista completa original.

## Requisitos funcionales (criterios de aceptación en EARS)

### Filtrado por Distrito Geográfico (H1)
- **RF-1**: CUANDO un usuario selecciona un distrito en el selector desplegable de distritos, EL SISTEMA mostrará únicamente las incidencias cuyo distrito coincida exactamente (comparación insensible a mayúsculas y espacios exteriores) con el distrito seleccionado.
- **RF-2**: EL SISTEMA proporcionará una opción predeterminada "Todos los distritos" que desactiva el criterio de filtrado por distrito.

### Filtrado por Prioridad Operativa (H2, H4)
- **RF-3**: MIENTRAS el usuario autenticado posea un rol interno (`OPERATOR`, `TECHNICIAN`, `ADMIN`, `ANALYST`), EL SISTEMA mostrará un selector de prioridad con las opciones "Todas las prioridades", "Baja" (`LOW`), "Media" (`MEDIUM`), "Alta" (`HIGH`) y "Crítica" (`CRITICAL`).
- **RF-4**: CUANDO un usuario con rol interno selecciona un nivel de prioridad, EL SISTEMA mostrará únicamente las incidencias que tengan exactamente asignado dicho nivel de prioridad.
- **RF-5**: MIENTRAS el usuario activo sea anónimo o posea el rol `CITIZEN`, EL SISTEMA ocultará visualmente el selector de prioridad en la interfaz de usuario.
- **RF-6**: SI una petición de consulta de incidencias proviene de un usuario no autenticado o con rol `CITIZEN` e incluye el parámetro de prioridad, ENTONCES EL SISTEMA ignorará dicho parámetro silenciosamente y devolverá los resultados sin aplicar filtro de prioridad.

### Filtrado por Departamento Municipal (H3, H4)
- **RF-7**: MIENTRAS el usuario autenticado posea un rol interno (`OPERATOR`, `TECHNICIAN`, `ADMIN`, `ANALYST`), EL SISTEMA mostrará un selector de departamento con las opciones "Todos los departamentos", "Sin departamento asignado", y cada uno de los departamentos municipales activos del catálogo identificados por su identificador único.
- **RF-8**: CUANDO un usuario con rol interno selecciona un departamento específico por su identificador, EL SISTEMA mostrará las incidencias que tengan al menos una tarea de asignación activa asociada a dicho departamento.
- **RF-9**: CUANDO un usuario con rol interno selecciona la opción "Sin departamento asignado", EL SISTEMA mostrará únicamente las incidencias que no poseen ninguna tarea de asignación activa a departamentos.
- **RF-10**: MIENTRAS el usuario activo sea anónimo o posea el rol `CITIZEN`, EL SISTEMA ocultará visualmente el selector de departamento en la interfaz de usuario.
- **RF-11**: SI una petición de consulta de incidencias proviene de un usuario no autenticado o con rol `CITIZEN` e incluye el parámetro de departamento o sin departamento, ENTONCES EL SISTEMA ignorará dicho parámetro silenciosamente y devolverá los resultados sin aplicar filtro de departamento.

### Combinación Multicriterio, Navegación y Restablecimiento (H5)
- **RF-12**: CUANDO se combinan dos o más filtros simultáneamente (texto, categoría, fechas, estado, prioridad, distrito, departamento), EL SISTEMA aplicará la intersección lógica (operador lógico Y / `AND`) de todos los criterios activos.
- **RF-13**: CUANDO el usuario pulsa la acción "Limpiar filtros", EL SISTEMA restablecerá simultáneamente todos los controles de filtrado a su estado inicial vacío y presentará el conjunto de incidencias sin filtrar.
- **RF-14**: SI no existen incidencias que cumplan la combinación de criterios seleccionados, ENTONCES EL SISTEMA mostrará un mensaje indicando que no se han encontrado incidencias para los filtros aplicados.
- **RF-15**: MIENTRAS el usuario interactúa con la vista actual (abriendo o cerrando modales de detalle de incidencias), EL SISTEMA mantendrá el estado de los filtros activos sin resetearlos.

## Requisitos no funcionales
- **RNF-1 (Seguridad y RBAC)**: La restricción de visibilidad de filtros internos (prioridad y departamento) debe validarse tanto en la capa de presentación (UI) como en la capa de servicio backend.
- **RNF-2 (Rendimiento de consulta)**: La consulta multicriterio combinada no debe degradar el tiempo de respuesta del listado por encima de 500 ms bajo condiciones normales de carga.
- **RNF-3 (Compatibilidad y diseño responsivo)**: Los nuevos controles de filtrado deben integrarse de forma adaptativa en la barra de filtros existente (`IncidentFilterBar`), manteniéndose utilizables en dispositivos móviles y de escritorio.
- **RNF-4 (Accesibilidad)**: Todos los nuevos selectores de formulario deben disponer de etiquetas semánticas (`aria-label` o `label` asociado) compatibles con lectores de pantalla.

## Casos límite
- **C-1 (Incidencia sin distrito)**: Incidencias que tengan distrito nulo o vacío; no coincidirán al filtrar por un distrito específico, pero sí aparecerán al seleccionar "Todos los distritos".
- **C-2 (Incidencia con múltiples departamentos asignados)**: Si una incidencia tiene asignaciones a más de un departamento (ej. Limpieza y Alumbrado), aparecerá al filtrar por cualquiera de los departamentos en los que tenga asignación activa.
- **C-3 (Sin resultados con filtros acumulados)**: Si la combinación de texto + distrito + prioridad resulta en 0 registros, la interfaz no debe quedar bloqueada ni romper la paginación o el renderizado del mapa.
- **C-4 (Peticiones con identificador de departamento inexistente)**: Si un rol interno filtra por un identificador de departamento inexistente o inactivo, el sistema retornará una lista vacía de incidencias sin generar excepciones no controladas (HTTP 500).

## Fuera de alcance
- Filtrado por geolocalización basada en radio métrico/polígono dibujado libremente sobre el mapa.
- Filtrado por técnico u operario individual asignado (diferido al módulo de tareas de cuadrilla).
- Filtros guardados personalizados o preajustes guardados por usuario en base de datos.
- Modificación del esquema de la tabla de incidencias (se aprovechan las columnas y relaciones existentes).

## Criterios de aceptación
- [ ] El selector de distritos incluye los distritos oficiales de Málaga y la opción "Todos los distritos", accesible para todos los roles (incluido `CITIZEN` y anónimos).
- [ ] Los selectores de prioridad y departamento solo son visibles y operables para roles no `CITIZEN` (`OPERATOR`, `TECHNICIAN`, `ADMIN`, `ANALYST`).
- [ ] Las peticiones realizadas por un usuario con rol `CITIZEN` o anónimo ignoran cualquier parámetro de prioridad o departamento enviado por API sin producir error.
- [ ] Es posible filtrar incidencias con la opción explícita "Sin departamento asignado".
- [ ] La combinación de múltiples filtros se resuelve aplicando la conjunción lógica `AND`.
- [ ] El botón "Limpiar filtros" restablece todos los selectores a sus valores por defecto.
- [ ] Si la búsqueda combinada no arroja resultados, se presenta un estado visual informativo sin errores.
- [ ] Todos los requisitos funcionales cuentan con pruebas unitarias y de integración automatizadas en verde.

## Dudas abiertas
- Ninguna. Dudas clarificadas y corregidas en la especificación.

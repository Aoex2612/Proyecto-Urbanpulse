# Spec 009 — Analítica Urbana y Reportes

**Estado:** propuesta

## Contexto y objetivo
Actualmente, los operadores y responsables municipales carecen de un módulo centralizado que evalúe el desempeño global del servicio, supervise el cumplimiento de los tiempos de compromiso de resolución (SLAs) y detecte patrones territoriales o estacionales en la ciudad de Málaga. Esta funcionalidad dota a analistas, administradores y operadores de un panel interactivo de analítica urbana (`/analytics`) con gráficos visuales y métricas clave de rendimiento, desglose espacial y temporal por distritos y barrios, exportación de informes ejecutivos en formato PDF y un motor asistido de recomendación de departamentos con estimación de grado de confianza.

## Usuarios / actores
- **Analista urbano (`ANALYST`):** Consulta indicadores agregados de rendimiento, patrones espaciales, gráficos visuales de distribución y evolución temporal histórica para planificar mejoras en el servicio municipal.
- **Administrador municipal (`ADMIN`):** Supervisa el desempeño global de todos los departamentos y genera informes oficiales en PDF para auditoría y rendición de cuentas.
- **Operador municipal (`OPERATOR`):** Consulta los tiempos de resolución y cuellos de botella de los departamentos para optimizar el triaje y recibe sugerencias de departamento con grado de confianza en el modal de asignación.
- **Ciudadano (`CITIZEN`):** No tiene acceso a este módulo (acceso denegado con error 403).
- **Técnico municipal (`TECHNICIAN`):** No tiene acceso a este módulo (acceso denegado con error 403).

## Historias de usuario
- **H1 (HU22 - Métricas Operativas y Tiempos de Resolución / SLAs):** Como analista u operador municipal, quiero consultar los indicadores clave de rendimiento (tiempos medios de triaje, tiempos medios de resolución y tasa de cumplimiento de SLA según prioridad) para identificar cuellos de botella en la atención de incidencias.
- **H2 (HU23 - Análisis Territorial Agregado y Correlación Histórica):** Como analista urbano, quiero filtrar y comparar la concentración de incidencias por distrito, barrio y rangos temporales históricos mediante gráficos interactivos para detectar tendencias estacionales y áreas geográficas de alta recurrencia.
- **H3 (HU24 - Exportación de Informes Territoriales y Operativos):** Como administrador o analista, quiero exportar un informe en formato PDF con el resumen de métricas y el desglose de incidencias del periodo seleccionado para archivar registros oficiales y compartirlos con las áreas de gobierno.
- **H4 (HU25 - Asistencia Inteligente y Recomendaciones con Grado de Confianza):** Como operador municipal o analista, quiero que el sistema preseleccione el departamento responsable sugerido con su porcentaje de certeza y ver el rendimiento global de estas recomendaciones para agilizar la asignación operativa.

## Requisitos funcionales (criterios de aceptación en EARS)

### Métricas Operativas y SLAs (H1 / HU22)
- RF-1: CUANDO un usuario autorizado (`ANALYST`, `ADMIN`, `OPERATOR`) accede al panel de analítica, EL SISTEMA calculará y presentará el Tiempo Medio de Triaje (MTTT) como la diferencia entre la fecha de reporte inicial y la primera validación o rechazo.
- RF-2: CUANDO un usuario autorizado consulta el rendimiento resolutivo, EL SISTEMA calculará y presentará el Tiempo Medio de Resolución (MTTR) para incidencias cerradas o resueltas como la diferencia entre la fecha de asignación/validación y la fecha de paso a estado `RESOLVED`.
- RF-3: EL SISTEMA evaluará el cumplimiento del objetivo de nivel de servicio (SLA) de resolución bajo los siguientes umbrales máximos según la prioridad de la incidencia:
  - Prioridad `CRITICAL`: Plazo máximo de 3 días naturales (72 horas).
  - Prioridad `HIGH`: Plazo máximo de 7 días naturales.
  - Prioridad `MEDIUM`: Plazo máximo de 15 días naturales.
  - Prioridad `LOW`: Plazo máximo de 30 días naturales.
- RF-4: CUANDO una incidencia en curso supere su plazo máximo de SLA según su prioridad (o cuando una incidencia resuelta haya requerido más tiempo que dicho plazo), EL SISTEMA la contabilizará como incumplimiento de SLA.
- RF-5: EL SISTEMA calculará la Tasa de Cumplimiento de SLA como el porcentaje de incidencias resueltas dentro de plazo respecto al total de incidencias evaluadas en el periodo.
- RF-6: EL SISTEMA desglosará el volumen total de incidencias y su tasa de resolución agrupándolas por departamento municipal asignado y por categoría de incidencia.

### Análisis Territorial Agregado, Gráficos y Filtros Temporales (H2 / HU23)
- RF-7: CUANDO el usuario selecciona un nivel de agrupación territorial (por Distrito municipal o por Barrio), EL SISTEMA recalculará los totales e indicadores agregados para la demarcación geográfica seleccionada.
- RF-8: EL SISTEMA proporcionará filtros temporales preconfigurados que incluirán: "Hoy", "Últimos 7 días", "Últimos 30 días", "Últimos 3 meses", "Últimos 6 meses", "Último año" y selección de un año histórico específico.
- RF-9: CUANDO el usuario aplica un filtro territorial o temporal, EL SISTEMA actualizará de forma reactiva las métricas operativas y la distribución de incidencias del periodo seleccionado.
- RF-10: EL SISTEMA renderizará gráficos visuales interactivos en el panel de analítica mostrando la distribución de incidencias por demarcación territorial y el porcentaje de cumplimiento de SLA por departamento.
- RF-11: SI para un periodo temporal, distrito o barrio seleccionado no existe ninguna incidencia registrada, ENTONCES EL SISTEMA mostrará un estado visual informativo de ausencia de datos manteniendo las métricas en cero sin provocar errores de división nula.

### Exportación de Informes en PDF (H3 / HU24)
- RF-12: MIENTRAS existan incidencias registradas en los filtros seleccionados, EL SISTEMA mantendrá habilitada la opción de "Exportar Informe PDF".
- RF-13: CUANDO el usuario pulsa en "Exportar Informe PDF", EL SISTEMA generará y descargará directamente un documento PDF estructurado que contendrá:
  - Cabecera con fecha de generación, usuario que emite el informe y periodo/filtros aplicados.
  - Bloque ejecutivo con el resumen de KPIs (totales, tiempos medios y porcentaje de cumplimiento de SLA).
  - Tabla detallada con el listado de las incidencias filtradas (identificador, título, distrito, barrio, categoría, prioridad, estado y fechas clave).
- RF-14: SI no existen incidencias en los filtros activos (listado vacío), ENTONCES EL SISTEMA deshabilitará el botón de descarga del informe en PDF informando al usuario de que se requieren datos para exportar.

### Asistencia Inteligente y Recomendaciones con Grado de Confianza (H4 / HU25)
- RF-15: CUANDO un operador abre el modal de asignación de una incidencia, EL SISTEMA preseleccionará automáticamente el departamento responsable recomendado y mostrará una insignia visible indicando que es la opción sugerida junto con su grado de confianza porcentual entre 0% y 100%.
- RF-16: DONDE el operador modifique la opción preseleccionada en el desplegable de asignación, EL SISTEMA mantendrá visible la insignia informativa con el departamento originalmente sugerido para permitir su consulta o reversión manual.
- RF-17: SI el grado de confianza calculado para la recomendación es inferior al 50%, ENTONCES EL SISTEMA mostrará un distintivo visual de advertencia explícita indicando que la confianza es baja para requerir confirmación rigurosa del operador.
- RF-18: EL SISTEMA presentará en el panel de analítica una métrica consolidada del motor de asistencia con el grado de confianza promedio y la tasa de adopción de las sugerencias por parte de los operadores.

### Control de Acceso y Navegación (RBAC)
- RF-19: EL SISTEMA mostrará el enlace de navegación "Analítica" en la barra de navegación superior (`Navbar`) únicamente a usuarios autenticados con rol `ANALYST`, `ADMIN` u `OPERATOR`, dirigiéndolos a la ruta `/analytics`.
- RF-20: EL SISTEMA restringirá el acceso a la ruta `/analytics` y a las operaciones de cálculo y exportación de informes exclusivamente a usuarios autenticados con rol `ANALYST`, `ADMIN` u `OPERATOR`.
- RF-21: SI un usuario con rol `CITIZEN` o `TECHNICIAN` (o un usuario no autenticado) intenta acceder a las métricas o a la exportación de informes, ENTONCES EL SISTEMA denegará la petición con código de error HTTP 403 Forbidden y redirigirá con una advertencia de acceso denegado.

## Requisitos no funcionales
- **Seguridad (RBAC):** Autenticación mediante token JWT en todas las consultas del backend y validación estricta de permisos por rol.
- **Rendimiento:** Las consultas analíticas agregadas deben responder en menos de 500 ms sobre un catálogo de al menos 5.000 incidencias.
- **Autonomía y Disponibilidad:** El motor de asistencia y el cálculo de métricas operarán de forma local en el servidor, garantizando funcionamiento continuo sin dependencias de servicios externos ni costes por llamada.
- **Idioma y Localización:** Todas las métricas, etiquetas, fechas e informes PDF generados se presentarán en español.

## Casos límite
- **Filtro sin incidencias (cero registros):** La vista presenta una pantalla vacía con diseño amigable indicando que no hay datos para el rango, los promedios numéricos reflejan 0 sin lanzar excepciones matemáticas de división por cero y el botón de exportación se inhabilita.
- **Incidencias sin distrito o barrio asignado:** Se contabilizan en los totales globales y se agrupan bajo la etiqueta *"Sin distrito especificado"* en el desglose territorial para evitar pérdidas de información en las auditorías.
- **Incidencias sin resolución (en curso):** Contabilizan para el MTTT (si fueron validadas) y para la detección de superación de SLA en curso, pero quedan excluidas del cálculo de MTTR hasta alcanzar el estado `RESOLVED`.
- **Textos de reporte extremadamente breves o ambiguos:** El motor de asistencia preselecciona la opción con mayor ponderación pero asigna un grado de confianza bajo (< 50%) y despliega la advertencia correspondiente sin bloquear la asignación manual.

## Fuera de alcance
- Envío programado periódico de informes por correo electrónico o boletines (diferido al subsistema de notificaciones).
- Invocación de APIs externas de pago de modelos de lenguaje (OpenAI, Anthropic, etc.).
- Modificación directa del estado de las incidencias o asignaciones desde el panel de analítica (las acciones operativas se realizan desde sus respectivos paneles de triaje y mapa).
- Exportación a formatos distintos a PDF en esta iteración (como hojas de cálculo XLSX o CSV).

## Criterios de aceptación
- [ ] Un usuario con rol `ANALYST`, `ADMIN` u `OPERATOR` puede acceder mediante el enlace "Analítica" en `Navbar` a la ruta `/analytics` y visualizar MTTT, MTTR, cumplimiento de SLA y distribución por departamento y categoría.
- [ ] Las incidencias se evalúan contra los plazos de SLA por prioridad: 3 días (`CRITICAL`), 7 días (`HIGH`), 15 días (`MEDIUM`) y 30 días (`LOW`).
- [ ] El usuario puede filtrar y agrupar reactivamente los datos por Distrito, por Barrio y por los rangos temporales predefinidos, visualizándolos en tarjetas y gráficos visuales interactivos.
- [ ] Ante periodos o distritos sin incidencias, el sistema despliega el estado visual vacío y deshabilita la exportación a PDF.
- [ ] La exportación a PDF genera un documento estructurado con el resumen ejecutivo de KPIs y el listado tabular de incidencias filtradas.
- [ ] Al asignar una incidencia, el sistema preselecciona automáticamente el departamento sugerido, conserva una insignia con la recomendación original y su grado de confianza, y advierte explícitamente si este es menor al 50%.
- [ ] El panel de analítica refleja el grado de confianza promedio del asistente y la tasa de adopción de las recomendaciones.
- [ ] Peticiones no autenticadas o con rol `CITIZEN` / `TECHNICIAN` reciben HTTP 403 Forbidden y son bloqueadas por el frontend.
- [ ] Cobertura completa de pruebas unitarias y de integración en backend y frontend para todos los requisitos funcionales.

## Dudas abiertas
_No existen dudas abiertas. Todos los requisitos, interacción visual y casos límite han sido aclarados y acordados durante la entrevista previa._

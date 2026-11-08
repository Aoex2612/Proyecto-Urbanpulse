# Spec 007 — Activos Urbanos y Contexto Externo
**Estado:** propuesta 📝

## Contexto y objetivo
Transformar los avisos reportados de incidentes aislados en unidades de trabajo enriquecidas operativamente. Para ello, el sistema calcula la proximidad espacial con los elementos de mobiliario e infraestructura de la ciudad (semáforos, fuentes, contenedores, paradas de autobús, etc.) de Málaga Open Data para sugerir y asociar el activo urbano (`UrbanAsset`) afectado, e ingesta de manera automática las observaciones meteorológicas (temperatura, lluvia, viento) y de tráfico municipal vigentes en el instante del reporte (`UrbanContext`). Todo el flujo incorpora resiliencia y degradación controlada mediante un patrón Circuit Breaker y límites de espera estrictos para asegurar que la creación de incidencias nunca quede bloqueada ni falle ante problemas o latencias de las fuentes externas.

## Usuarios / actores
- **Ciudadano (`CITIZEN`):** Al reportar una incidencia geolocalizada en el mapa, el sistema procesa automáticamente en segundo plano la asociación del activo más próximo y el contexto ambiental/tráfico sin percibir bloqueos ni demoras en la creación del aviso.
- **Operador (`OPERATOR` / `ADMIN`):** Visualiza en el panel de triaje y en la ficha de detalle de la incidencia los activos urbanos vinculados (candidatos detectados, distancia en metros y puntuación de confianza), puede confirmar o desvincular la asociación de un activo, y examina las observaciones de contexto exterior registradas (clima y tráfico) al momento del reporte.
- **Técnico de Mantenimiento (`TECHNICIAN`):** Consulta en su orden de trabajo el activo urbano físico concreto asociado (código de activo, tipo, nombre) para acudir de manera directa a la infraestructura averiada.
- **Sistema Externo (APIs de Datos):** Portal Málaga Open Data (WFS / catálogos de equipamiento y tráfico de Málaga) y Servicio Meteorológico (Open-Meteo / AEMET) como proveedores de datos geoespaciales y atmosféricos.

## Historias de usuario
- **H1 (HU16 - Asociación con Activo Urbano):** Como operador o técnico, quiero que el sistema calcule por proximidad geográfica y vincule a la incidencia los activos urbanos municipales más cercanos (ej. semáforos, contenedores, fuentes) con su distancia y nivel de confianza, permitiendo su confirmación en el triaje para identificar con precisión la infraestructura afectada.
- **H2 (HU17 - Ingesta de Contexto Externo):** Como operador o analista, quiero que el sistema capture las condiciones meteorológicas (temperatura, precipitación, viento) y de tráfico en el entorno de la incidencia en el instante de su creación e incorpore dichas observaciones al contexto de la incidencia (`UrbanContext`), para disponer de un diagnóstico enriquecido de las condiciones del entorno.
- **H3 (HU18 - Degradación Controlada y Resiliencia):** Como ciudadano y operador, quiero que la plataforma aplique mecanismos de tolerancia a fallos (timeouts estrictos, Circuit Breaker y fallback silencioso) ante la indisponibilidad o lentitud de las APIs externas de activos o contexto, para que la creación de la incidencia concluya con éxito sin interrupción ni bloqueo del servicio.

## Requisitos funcionales (criterios de aceptación en EARS)

### Asociación con Activo Urbano (HU16)
- **RF-1:** CUANDO se registra una incidencia con coordenadas geográficas válidas, EL SISTEMA buscará los activos urbanos registrados dentro de un radio de proximidad configurable (por defecto 50 metros).
- **RF-2:** CUANDO se identifican activos candidatos dentro del radio de búsqueda, EL SISTEMA calculará la distancia geodésica en metros y la puntuación de confianza normalizada (entre 0.0 y 1.0) para cada candidato, vinculando la asociación a la incidencia en estado no confirmado (`is_confirmed = false`).
- **RF-3:** CUANDO un operador autenticado revisa una incidencia con activos vinculados, EL SISTEMA permitirá confirmar (`is_confirmed = true`) el activo urbano definitivo mediante el endpoint `POST /api/incidents/{incidentId}/assets/{assetId}/confirm`.
- **RF-4:** CUANDO un operador autenticado lo solicite, EL SISTEMA permitirá desvincular o eliminar la asociación de un activo urbano de una incidencia mediante el endpoint `DELETE /api/incidents/{incidentId}/assets/{assetId}`.
- **RF-5:** CUANDO se consulta el detalle de una incidencia mediante `GET /api/incidents/{id}`, EL SISTEMA incluirá la lista de activos asociados con su código (`assetCode`), tipo (`type`), nombre, distancia en metros, puntuación de confianza y si está confirmado.
- **RF-6:** SI no se encuentra ningún activo urbano dentro del radio máximo de búsqueda (50 metros), ENTONCES EL SISTEMA registrará la incidencia sin asociaciones de activos y sin arrojar ningún error.
- **RF-7:** SI un operador intenta confirmar una asociación de activo inexistente para dicha incidencia, ENTONCES EL SISTEMA responderá con código HTTP 404 Not Found y mensaje estructurado.

### Ingesta de Contexto Externo (HU17)
- **RF-8:** CUANDO se registra una incidencia con coordenadas geográficas válidas, EL SISTEMA consultará la API meteorológica externa para capturar las condiciones climáticas (temperatura en °C, precipitación/lluvia y velocidad del viento) en el punto o término municipal de Málaga en ese instante.
- **RF-9:** CUANDO se capturan las métricas meteorológicas o de tráfico exterior, EL SISTEMA persistirá dichas lecturas en la tabla de observaciones externas (`external_observations`) y las asociará al contexto de la incidencia (`incident_contexts`).
- **RF-10:** CUANDO un usuario autenticado consulta el detalle de una incidencia mediante `GET /api/incidents/{id}`, EL SISTEMA retornará el bloque de contexto externo (`urbanContext`) con las métricas recogidas (fuente, tipo de métrica, valor numérico o textual y fecha/hora de observación).
- **RF-11:** MIENTRAS el servicio de tráfico municipal provea datos en tiempo real para el tramo o distrito de la incidencia, EL SISTEMA registrará la observación correspondiente (nivel de congestión o fluidez) vinculada al aviso.

### Degradación Controlada y Resiliencia (HU18)
- **RF-12:** EL SISTEMA aplicará un límite máximo de espera (timeout estricto) de 3.000 ms a las peticiones dirigidas a APIs externas de clima, tráfico o activos.
- **RF-13:** SI una llamada a una API externa excede el timeout de 3.000 ms, falla por error de conexión de red o retorna un código de error HTTP (5xx / 4xx), ENTONCES EL SISTEMA activará el mecanismo de Fallback, registrando la advertencia en el log de auditoría y permitiendo que la creación de la incidencia finalice exitosamente.
- **RF-14:** SI la tasa de fallos de un servicio externo supera el umbral del Circuit Breaker (50% de fallos en una ventana móvil de 10 peticiones), ENTONCES EL SISTEMA abrirá el circuito durante un periodo de enfriamiento (ej. 30 segundos), ejecutando el fallback inmediato sin penalizar con latencia a las peticiones entrantes.
- **RF-15:** SI falla el servicio meteorológico o de tráfico al registrar una incidencia, ENTONCES EL SISTEMA persistirá la incidencia con la lista de contexto externo vacía (`urbanContext: []`), garantizando que la respuesta al usuario retorne HTTP 201 Created.

## Requisitos no funcionales
- **Tolerancia a fallos y desacoplamiento:** El enriquecimiento de contexto externo y asociación de activos no debe ser un punto único de fallo (SPOF) para el ciclo de creación de incidencias.
- **Tiempo de respuesta:** La respuesta total del endpoint de creación de incidencias no debe verse degradada en más de 500 ms en condiciones normales (o en un máximo de 3.000 ms en el peor escenario de timeout externo síncrono, o 0 ms de retraso si se orquesta de forma asíncrona mediante `@Async`/eventos de dominio).
- **Escalabilidad y consumo de APIs:** Uso de almacenamiento local / caché para el inventario de activos urbanos de Málaga (`urban_assets`) a fin de permitir cálculos espaciales rápidos en memoria o mediante consultas indexadas en PostgreSQL, sin invocar APIs externas pesadas en cada reporte.
- **Seguridad y permisos (RBAC):** La consulta de activos y contexto en la incidencia es accesible para cualquier usuario autenticado con permiso para ver la incidencia; la confirmación y desvinculación de activos queda restringida exclusivamente a roles `OPERATOR` y `ADMIN`.

## Casos límite
- **Coordenadas sin activos en las inmediaciones:** Si la incidencia está en una zona abierta o descampado sin elementos catalogados en un radio de 50m, la lista de asociaciones de activos se guarda vacía de forma transparente.
- **Múltiples activos a distancia similar:** Si hay 3 farolas o contenedores a distancia similar (ej. 4m, 5m y 6m), el sistema guarda los candidatos ordenados por distancia ascendente y mayor confianza, permitiendo al operador confirmar cuál es el exacto en la interfaz.
- **Caída total o timeout prolongado de la API de Open-Meteo / Tráfico:** El Circuit Breaker se abre y conmuta a fallback inmediatamente, logueando la incidencia sin contexto externo sin afectar la UX del ciudadano.
- **Respuestas con datos parciales de fuentes externas:** Si la API devuelve temperatura pero no velocidad de viento, el sistema persiste únicamente las observaciones válidas recibidas sin abortar la transacción.
- **Operador confirma un activo ya confirmado:** La operación es idempotente; actualiza el registro sin duplicar asociaciones.

## Fuera de alcance
- Descarga o sincronización en tiempo real de geometrías complejas GIS o polígonos 3D de activos (se utiliza representación puntual latitud/longitud).
- Edición o creación manual por parte de ciudadanos de nuevos activos en el inventario municipal.
- Control de semáforos o telemando de actuadores de la vía pública (UrbanPulse es solo una plataforma de gestión e información, no un sistema SCADA).
- Predicción climatológica futura (solo se capturan las observaciones vigentes al momento del reporte).

## Criterios de aceptación
- [ ] Cálculo de proximidad geográfica ejecutado al reportar una incidencia, detectando activos urbanos a menos de 50 metros.
- [ ] Generación de registro en `incident_asset_associations` con `distance_meters`, `confidence_score` y `is_confirmed = false`.
- [ ] Endpoint `POST /api/incidents/{incidentId}/assets/{assetId}/confirm` operativo para roles `OPERATOR`/`ADMIN`.
- [ ] Endpoint `DELETE /api/incidents/{incidentId}/assets/{assetId}` operativo para desvincular un activo.
- [ ] Ingesta automática de observaciones meteorológicas (temperatura, precipitación, viento) en `external_observations` y asociadas a la incidencia.
- [ ] Inclusión de `urbanAssets` y `urbanContext` en la respuesta JSON de `GET /api/incidents/{id}`.
- [ ] Patrón Circuit Breaker / Fallback implementado con timeout estricto de 3 segundos ante caídas o lentitud de APIs externas.
- [ ] Alta de incidencias exitosa (HTTP 201 Created) incluso ante la desconexión total o simulación de error 500/timeout de las APIs externas.
- [ ] Batería de pruebas unitarias y de integración que validen el cálculo de proximidad, fallback ante fallo externo y endpoints REST.

## Dudas abiertas
- Ninguna. Los requisitos funcionales y contratos se alinean con las directrices de `docs/DESCRIPCION.md`, la arquitectura C4 y el esquema relacional existente en PostgreSQL.

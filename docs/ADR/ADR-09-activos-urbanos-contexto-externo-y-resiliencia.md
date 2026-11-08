# ADR-09: Enriquecimiento Contextual Externo y Asociación de Activos con Resiliencia Circuit Breaker

- **Estado:** Aceptado
- **Fecha:** 2026-11-08
- **Autores:** UrbanPulse Architecture & Development Team

## Contexto

La plataforma UrbanPulse persigue transformar cada reporte de incidencia en una unidad operativa enriquecida mediante dos dimensiones principales:
1. **Asociación con Activos Urbanos (`UrbanAsset`):** Identificación e inferencia del mobiliario o equipamiento municipal más cercano (semáforos, fuentes, contenedores, paradas de autobús) a partir de las coordenadas del incidente con cálculo geodésico de proximidad y nivel de confianza.
2. **Ingesta de Contexto Externo (`UrbanContext` / `ExternalObservation`):** Captura de parámetros ambientales y meteorológicos (temperatura, precipitaciones, viento) y estado del tráfico en el momento exacto en que ocurre el reporte.

Dado que las consultas a servicios y APIs externas (Open-Meteo, AEMET, WFS de Málaga Open Data) dependen de redes públicas de terceros y pueden sufrir latencias imprevistas, saturación o caídas temporales de servicio, la arquitectura debe garantizar el principio de **Degradación Controlada** (establecido en la constitución del proyecto y en el ADR-06 original): la indisponibilidad de fuentes externas nunca debe bloquear ni abortar la creación del reporte ciudadano.

## Decisión

1. **Cálculo de Proximidad Geográfica de Activos:**
   - Se implementa el cálculo de distancia geodésica mediante la fórmula de Haversine optimizada en `UrbanAssetService` / consulta espacial en el repositorio `UrbanAssetRepository`.
   - Se establece un radio de búsqueda acotado a 50 metros. Para cada activo hallado, se calcula una puntuación de confianza normalizada en el rango $[0.0, 1.0]$ inversamente proporcional a la distancia: $\text{confianza} = \max(0.0, 1.0 - (\text{distancia} / 50.0))$.
   - La asociación se registra inicialmente en `incident_asset_associations` con `is_confirmed = false`. Los operadores y administradores disponen de endpoints REST para confirmar o desvincular un activo durante el triaje.

2. **Ingesta Contextual Meteorológica y de Tráfico:**
   - Se implementa `ExternalContextService` que invoca clientes HTTP ligeros basados en `RestTemplate` configurados con timeouts estrictos de conexión y lectura (3.000 ms).
   - Las lecturas normalizadas se registran en `external_observations` y se vinculan a la incidencia mediante `incident_contexts`.

3. **Patrón Circuit Breaker y Fallback:**
   - Se introduce un componente de resiliencia (`CircuitBreakerAspect` o gestor de estado con ventana deslizante en memoria) para proteger las llamadas a las APIs externas.
   - Parámetros: límite de timeout de 3.000 ms, ventana de 10 peticiones, umbral de fallo del 50% y tiempo de enfriamiento (open circuit state) de 30 segundos.
   - En caso de timeout, error HTTP 5xx o circuito abierto, se activa inmediatamente el método de **Fallback**: la incidencia se guarda con éxito, se omite el contexto exterior afectado sin penalizar al usuario con demoras innecesarias, y se registra la advertencia en el log de auditoría.

4. **Desacoplamiento mediante Mappers y DTOs:**
   - La respuesta detallada de incidencias (`IncidentResponse`) se extiende con los campos `urbanAssets` (`List<IncidentAssetResponse>`) y `urbanContext` (`List<ExternalObservationResponse>`), mapeados desde la capa de servicio a través de mappers dedicados, manteniendo intacto el aislamiento del dominio JPA.

## Consecuencias

### Positivas
- **Alta disponibilidad y cero fricción ciudadana:** La creación de incidencias siempre responde con éxito (HTTP 201 Created) incluso ante caídas de proveedores de datos externos.
- **Trazabilidad y mantenimiento guiado:** Los operarios y técnicos disponen de códigos de activos urbanos concretos para actuar sobre la infraestructura física.
- **Aislamiento de fallos:** El Circuit Breaker evita el agotamiento del pool de hilos del servidor HTTP ante servicios lentos o caídos.

### Negativas / Mitigaciones
- **Datos parciales en situaciones de fallback:** Ante caídas de la API meteorológica, las incidencias carecerán de datos climáticos puntuales. *Mitigación:* Se registra explícitamente en logs y se muestra como "No disponible" en la interfaz de usuario sin generar inconsistencias.
- **Sobrecarga de cómputo en búsquedas espaciales:** Calcular distancias con gran volumen de activos podría ralentizar el guardado. *Mitigación:* Pre-filtrado por caja delimitadora (bounding box de latitud/longitud en la consulta SQL) antes de aplicar el cálculo fino de Haversine.

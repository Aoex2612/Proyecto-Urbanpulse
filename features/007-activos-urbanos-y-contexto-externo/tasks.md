# 007 — Activos Urbanos y Contexto Externo — Tareas

_Checklist accionable derivada del `plan.md`. Tareas principales y subtareas pequeñas y concretas; marca `[x]` al completarlas._

## [ ] Tarea 1: Capa de Persistencia y Repositorios JPA para Activos y Contexto — dependencias: ninguna — RF: RF-1, RF-2, RF-9
- [x] Verificar y configurar `ActivoUrbanoRepository` con método `findByLatitudeBetweenAndLongitudeBetween` para prefiltrado espacial.
- [x] Implementar `AsociacionIncidenciaActivoRepository` con consultas por `incidentId` y borrado por identificador compuesto.
- [x] Implementar `ObservacionesExternasRepository` y `ContextoIncidenciaRepository` para el registro y consulta de observaciones de la incidencia.
- [x] Agregar fixtures de datos iniciales en BD/tests (activos de Málaga: semáforos, fuentes, contenedores).
- [ ] Validar contra los criterios de aceptación de `spec.md`.

## [x] Tarea 2: DTOs y Mappers Desacoplados — depende de Tarea 1 — RF: RF-5, RF-10
- [x] Crear `IncidentAssetResponse` con atributos de activo, distancia en metros, nivel de confianza y flag de confirmación.
- [x] Crear `ExternalObservationResponse` con atributos de fuente, métrica, valor y marca temporal.
- [x] Implementar `UrbanAssetMapper` y `UrbanContextMapper` en el paquete `org.bulbaseros.mappers`.
- [x] Extender `IncidentResponse` para incorporar las listas `urbanAssets` y `urbanContext`.
- [x] Tests unitarios de mappers para verificar la conversión bidireccional y el aislamiento del dominio JPA.
- [x] Validar contra los criterios de aceptación de `spec.md`.

## [x] Tarea 3: Lógica de Cálculo de Proximidad y Servicio de Activos (HU16) — depende de Tarea 1, Tarea 2 — RF: RF-1, RF-2, RF-3, RF-4, RF-6, RF-7
- [x] Implementar `UrbanAssetService` con cálculo geodésico de Haversine (`distanceInMeters`).
- [x] Implementar algoritmo de búsqueda de activos en radio de 50 metros y cálculo de `confidenceScore` normalizado entre 0.0 y 1.0.
- [x] Implementar métodos `associateNearbyAssets`, `confirmAssetAssociation` y `removeAssetAssociation`.
- [x] Manejo de casos límite: sin activos en el radio (lista vacía), activo inexistente (excepción estructurada 404).
- [x] Pruebas unitarias de `UrbanAssetServiceTest` (cálculo de distancias, ordenación por proximidad y fórmulas de confianza).
- [x] Validar contra los criterios de aceptación de `spec.md`.

## [x] Tarea 4: Patrón Circuit Breaker y Servicio de Ingesta de Contexto Externo con Fallback (HU17, HU18) — depende de Tarea 1, Tarea 2 — RF: RF-8, RF-9, RF-11, RF-12, RF-13, RF-14, RF-15
- [x] Implementar componente de resiliencia `CircuitBreaker` (estados CLOSED, OPEN, HALF_OPEN, ventana móvil y cooldown de 30s).
- [x] Implementar `ExternalContextService` con cliente `RestTemplate` configurado a 3.000 ms de timeout.
- [x] Implementar conector HTTP con Open-Meteo para temperatura, lluvia y viento.
- [x] Implementar conector/adaptador para tráfico municipal en el tramo de la incidencia.
- [x] Implementar método de fallback transparente ante fallos de conexión, timeout o circuito abierto (devolviendo lista vacía y registrando log).
- [x] Pruebas unitarias de `CircuitBreakerTest` y `ExternalContextServiceTest` (verificación de fallback y tolerancia a fallos).
- [x] Validar contra los criterios de aceptación de `spec.md`.

## [ ] Tarea 5: Integración en Creación y Consulta de Incidencias — depende de Tarea 3, Tarea 4 — RF: RF-1, RF-5, RF-8, RF-10, RF-15
- [ ] Modificar `IncidentService.createIncident` para orquestar la llamada a `UrbanAssetService` y `ExternalContextService`.
- [ ] Asegurar que el guardado de la incidencia (`HTTP 201 Created`) nunca falle si el enriquecimiento externo experimenta problemas.
- [ ] Modificar `IncidentService.getIncidentById` para enriquecer `IncidentResponse` con las asociaciones de activos y observaciones contextuales.
- [ ] Pruebas de integración para el alta de incidencia con y sin servicios externos disponibles.
- [ ] Validar contra los criterios de aceptación de `spec.md`.

## [ ] Tarea 6: Endpoints REST y Seguridad RBAC para Gestión de Activos — depende de Tarea 3, Tarea 5 — RF: RF-3, RF-4, RF-7
- [ ] Exponer endpoint `POST /api/incidents/{incidentId}/assets/{assetId}/confirm` restringido a roles `OPERATOR` y `ADMIN`.
- [ ] Exponer endpoint `DELETE /api/incidents/{incidentId}/assets/{assetId}` restringido a roles `OPERATOR` y `ADMIN`.
- [ ] Añadir manejo de excepciones específicas en `GlobalExceptionHandler`.
- [ ] Tests de integración de controladores con `MockMvc` validando códigos HTTP 200, 403 (para rol `CITIZEN`) y 404.
- [ ] Validar contra los criterios de aceptación de `spec.md`.

## [ ] Tarea 7: Componentes Frontend para Activos Urbanos y Contexto Externo — depende de Tarea 2, Tarea 6 — RF: RF-3, RF-4, RF-5, RF-10
- [ ] Actualizar cliente API en `frontend/src/services/incidentService.js` con funciones de confirmación y eliminación de asociaciones de activos.
- [ ] Crear componente `IncidentAssetCard.jsx` para mostrar activos vinculados (nombre, tipo, distancia en metros, confianza y botón de confirmación/desvinculación para operadores).
- [ ] Crear componente `IncidentContextCard.jsx` para mostrar datos meteorológicos (temperatura, precipitación, viento) y estado del tráfico.
- [ ] Integrar ambos componentes en el modal/vista de detalle `IncidentDetailModal.jsx`.
- [ ] Tests en Vitest para `IncidentAssetCard.test.jsx` e `IncidentContextCard.test.jsx`.
- [ ] Validar contra los criterios de aceptación de `spec.md`.

## [ ] Tarea 8: Verificación Global, Documentación y Cobertura — depende de Tarea 5, Tarea 6, Tarea 7 — RF: Todos
- [ ] Ejecutar suite completa de tests backend (`mvn clean test`) comprobando tasa de éxito al 100% y cobertura JaCoCo.
- [ ] Ejecutar linter y tests de frontend (`npm run lint`, `npm test`).
- [ ] Actualizar documentación técnica y diagramas en `docs/architecture.md` y `docs/api.md` con los nuevos endpoints y modelos.
- [ ] Validar contra todos los criterios de aceptación de `spec.md`.

---

## Mantenimiento (checklist recurrente)
- [ ] Verificar que los catálogos de activos en base de datos (`urban_assets`) mantienen índices adecuados sobre latitud y longitud.
- [ ] Monitorizar las tasas de apertura del Circuit Breaker y los tiempos de latencia reportados en los logs de producción.

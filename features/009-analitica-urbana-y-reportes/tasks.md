# 009 — Analítica Urbana y Reportes — Tareas

_Checklist accionable derivada del `plan.md`. Tareas principales y subtareas pequeñas y concretas; marca `[x]` al completarlas._

## [x] Tarea 1: DTOs, Enums de Filtrado y Mapeador de Dominio en Backend - RF: RF-1 al RF-9, RF-14, RF-15, RF-16, RF-18
- [x] 1.1 Crear enum `AnalyticsTimePreset` (`TODAY`, `LAST_7_DAYS`, `LAST_30_DAYS`, `LAST_3_MONTHS`, `LAST_6_MONTHS`, `LAST_YEAR`, `SPECIFIC_YEAR`) y DTO `AnalyticsFilterRequest` en `org.bulbaseros.dto` con validaciones de Bean Validation.
- [x] 1.2 Crear DTOs de métricas operativas y territoriales `OperationalMetricsResponse`, `TerritorialMetricResponse`, `DepartmentMetricResponse` y `CategoryMetricResponse`.
- [x] 1.3 Crear DTOs de asistencia inteligente `DepartmentRecommendationResponse` (con campo `isLowConfidenceWarning`) y `AssistantPerformanceResponse`.
- [x] 1.4 Crear DTO consolidado `AnalyticsSummaryResponse` que agrupe todos los bloques analíticos y el listado de incidencias filtradas para la tabla y exportación.
- [x] 1.5 Crear componente mapeador `AnalyticsMapper` en `org.bulbaseros.mappers` extendiendo `BaseMapper` con Javadoc completo.
- [x] 1.6 Crear pruebas unitarias verificando conversiones bidireccionales y restricciones en `AnalyticsMapperTest` y `DtoAnalyticsValidationTest`.

## [x] Tarea 2: Métodos de Consulta y Agregación en Capa de Repositorios en Backend - Depende de Tarea 1 - RF: RF-1, RF-2, RF-6, RF-7, RF-8, RF-11
- [x] 2.1 Añadir consultas en `IncidenciaRepository` para filtrado combinado por rango temporal (`createdAt BETWEEN`), distrito y barrio, con agregaciones por categoría.
- [x] 2.2 Añadir consultas en `CambiosEstadoRepository` para obtener marcas temporales de primera validación/rechazo (`REPORTED` → `VALIDATED`/`REJECTED`) y resolución (`RESOLVED`).
- [x] 2.3 Añadir consultas en `TareasRepository` para computar incidencias activas e históricas por departamento municipal.
- [x] 2.4 Crear pruebas `@DataJpaTest` en `AnalyticsRepositoriesTest` validando consultas temporales, agregaciones por distrito y comportamiento ante conjuntos vacíos.

## [x] Tarea 3: Servicio de Asistencia Inteligente y Recomendación con Grado de Confianza - Depende de Tareas 1 y 2 - RF: RF-14, RF-15, RF-16, RF-17, RF-18
- [x] 3.1 Implementar `IntelligentRecommendationService` en `org.bulbaseros.service` con motor heurístico léxico ponderado y cálculo de grado de certeza porcentual (0% a 100%).
- [x] 3.2 Implementar lógica de activación del distintivo de advertencia explícita para recomendaciones con confianza inferior al 50%.
- [x] 3.3 Implementar cálculo de confianza promedio y tasa de adopción de sugerencias departamentales por los operadores.
- [x] 3.4 Crear pruebas unitarias con Mockito en `IntelligentRecommendationServiceTest` evaluando alta confianza, baja confianza (< 50%) y textos ambiguos o escuetos.

## [x] Tarea 4: Servicio Central de Analítica, Evaluación de SLAs y Agregaciones - Depende de Tareas 1, 2 y 3 - RF: RF-1 al RF-11, RF-18
- [x] 4.1 Implementar en `AnalyticsService` el cálculo de Tiempo Medio de Triaje (MTTT) y Tiempo Medio de Resolución (MTTR) en horas a partir de las marcas temporales de auditoría.
- [x] 4.2 Implementar la matriz determinista de evaluación de cumplimiento de SLA por prioridad: `CRITICAL` (3 días), `HIGH` (7 días), `MEDIUM` (15 días) y `LOW` (30 días), detectando tanto incidencias en curso fuera de plazo como resueltas fuera de plazo.
- [x] 4.3 Implementar cálculo de agregaciones dinámicas por Distrito, por Barrio, por Departamento y por Categoría según el filtro aplicado.
- [x] 4.4 Garantizar la resiliencia ante conjuntos sin incidencias (cero registros), retornando métricas a 0 sin divisiones por cero (`NaN` / `Infinity`).
- [x] 4.5 Crear pruebas unitarias exhaustivas con Mockito en `AnalyticsServiceTest` cubriendo todos los umbrales de SLA, filtros temporales y casos límite.

## [x] Tarea 5: Controlador REST y Seguridad RBAC en Backend - Depende de Tareas 1 a 4 - RF: RF-1 al RF-11, RF-14, RF-15, RF-16, RF-19, RF-20, RF-21
- [x] 5.1 Crear `AnalyticsController` en `org.bulbaseros.controller` exponiendo `GET /api/analytics/summary` con parámetros de filtro opcionales y `GET /api/analytics/recommend-department`.
- [x] 5.2 Actualizar `SecurityConfig` para proteger las rutas `/api/analytics/**` permitiendo acceso a `ANALYST`, `ADMIN` y `OPERATOR`, y denegando a `CITIZEN` y `TECHNICIAN`.
- [x] 5.3 Crear pruebas de integración en `AnalyticsControllerTest` con `MockMvc` y `@WithMockUser` verificando respuestas 200 OK, payload consolidado y código 403 Forbidden para roles no autorizados.

## [x] Tarea 6: Servicios API y Módulo de Exportación a PDF en Frontend - Depende de Tarea 5 - RF: RF-12, RF-13, RF-14
- [x] 6.1 Crear servicio cliente `frontend/src/services/analyticsService.js` con métodos Axios para interactuar con los endpoints de analítica y recomendación.
- [x] 6.2 Crear módulo utilitario `frontend/src/utils/reportUtils.js` para compilar y generar el documento PDF estructurado (portada/cabecera oficial, resumen ejecutivo de KPIs y tabla de incidencias filtradas).
- [x] 6.3 Crear componente `ExportPdfButton.jsx` con botón de descarga interactivo, feedback visual durante la generación y bloqueo reactivo (`disabled`) si no hay datos.
- [x] 6.4 Crear pruebas unitarias con Vitest en `analyticsService.test.js` y `reportUtils.test.js`.

## [ ] Tarea 7: Componentes del Panel de Analítica y Gráficos Visuales en Frontend - Depende de Tarea 6 - RF: RF-1 al RF-11, RF-18
- [ ] 7.1 Crear componente `AnalyticsKpiCards.jsx` y su hoja de estilos con tarjetas visuales para Total de incidencias, MTTT, MTTR y % Cumplimiento de SLA.
- [ ] 7.2 Crear componente `AnalyticsDateFilter.jsx` con botones de filtro rápido (Hoy, 7d, 30d, 3m, 6m, Año, Año específico) y selectores de Distrito y Barrio.
- [ ] 7.3 Crear componentes gráficos `TerritorialChart.jsx` y `SlaComplianceChart.jsx` renderizando gráficos visuales interactivos en SVG nativo.
- [ ] 7.4 Crear componente `EmptyAnalyticsState.jsx` con estado visual amigable e informativo para rangos sin incidencias registradas.
- [ ] 7.5 Crear página principal `AnalyticsPage.jsx` y estilos asociados en `AnalyticsPage.css` integrando los componentes anteriores y la tabla de datos.
- [ ] 7.6 Crear pruebas unitarias con Vitest en `AnalyticsPage.test.jsx`, `AnalyticsKpiCards.test.jsx` y `EmptyAnalyticsState.test.jsx`.

## [ ] Tarea 8: Integración de Asistencia Inteligente en Triaje, Navegación y Verificación Global - Depende de Tareas 5, 6 y 7 - RF: Todos (RF-1 al RF-21)
- [ ] 8.1 Actualizar `AssignmentModal.jsx` para invocar el servicio de recomendación, preseleccionar automáticamente el departamento sugerido, mostrar badge persistente de opción recomendada y advertencia para confianza < 50%.
- [ ] 8.2 Actualizar `Navbar.jsx` con el enlace visible "Analítica" hacia `/analytics` únicamente para usuarios con rol `ANALYST`, `ADMIN` u `OPERATOR`.
- [ ] 8.3 Registrar la ruta protegida `/analytics` en `App.jsx` protegida por `ProtectedRoute` y `RoleGuard`.
- [ ] 8.4 Ejecutar suite de pruebas de Backend (`cd Backend && mvn clean test`) verificando 100% de tests en verde y reporte JaCoCo.
- [ ] 8.5 Ejecutar suite de pruebas y linter de Frontend (`cd frontend && npm test` y `npm run lint`) garantizando 0 errores y advertencias.
- [ ] 8.6 Redactar documento técnico y guía de pruebas de la feature en `docs/009-analitica-urbana-y-reportes/qa-note.md` para la posterior fase de validación (`sdd-validate`).

---
## Mantenimiento (checklist recurrente)

- [ ] Verificar que ninguna operación matemática de promedios o porcentajes admita divisiones por cero ante conjuntos vacíos.
- [ ] Comprobar que los endpoints analíticos nunca sean accesibles por usuarios con rol `CITIZEN` o `TECHNICIAN`.
- [ ] Asegurar que el informe PDF generado mantenga consistencia con los datos visualizados en pantalla para los mismos filtros aplicados.
- [ ] Validar que el recomendador inteligente mantenga la bandera de baja confianza si la probabilidad calculada no supera el umbral del 50%.

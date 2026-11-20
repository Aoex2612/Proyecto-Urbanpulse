# Tarea 1: DTOs, Enums de Filtrado y Mapeador de Dominio en Backend

**Feature:** `009-analitica-urbana-y-reportes`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-1 al RF-9, RF-14, RF-15, RF-16, RF-18  

---

## 1. Resumen de la Tarea

En esta primera tarea se ha implementado la base estructural y desacoplada del subsistema analítico de UrbanPulse, conformada por los contratos de transferencia de datos (DTOs), enumeraciones estandarizadas de filtrado y el componente mapeador de agregación sobre el modelo de dominio:

### 1.1 Enumeración de Presets Temporales y Solicitud de Filtrado (`org.bulbaseros.dto`)
- **`AnalyticsTimePreset`**: Enum con los 7 rangos predefinidos exigidos en RF-8:
  - `TODAY`: Día en curso.
  - `LAST_7_DAYS`: Últimos 7 días naturales.
  - `LAST_30_DAYS`: Últimos 30 días naturales.
  - `LAST_3_MONTHS`: Últimos 3 meses naturales.
  - `LAST_6_MONTHS`: Últimos 6 meses naturales.
  - `LAST_YEAR`: Último año natural (365 días).
  - `SPECIFIC_YEAR`: Año natural histórico específico.
- **`AnalyticsFilterRequest`**: DTO que encapsula los parámetros de filtrado temporal (`preset`, `startDate`, `endDate`, `year`) y delimitación territorial (`district`, `neighborhood`). Incluye anotaciones de Bean Validation (`@Size(max = 100)` para distrito y barrio, y `@Min(2000) @Max(2100)` para el año).

### 1.2 DTOs de Métricas Operativas, Territoriales y Departamentales (`org.bulbaseros.dto`)
- **`OperationalMetricsResponse`**: Consolida los indicadores clave de rendimiento (KPIs) operacionales de RF-1 a RF-5: `totalIncidents`, `resolvedIncidents`, `pendingIncidents`, `mtttHours` (Tiempo Medio de Triaje), `mttrHours` (Tiempo Medio de Resolución), `slaComplianceRate` (% de cumplimiento), `totalSlaBreached` e `totalSlaCompliant`.
- **`TerritorialMetricResponse`**: Agrega métricas para demarcaciones territoriales (distrito o barrio según RF-7 y RF-10): `territoryName`, `totalIncidents`, `resolvedCount`, `pendingCount`, `slaComplianceRate`.
- **`DepartmentMetricResponse`**: Modela el desempeño y carga de trabajo asignada a cada departamento municipal (RF-6 y RF-10): `departmentId`, `departmentName`, `totalAssigned`, `resolvedCount`, `avgResolutionHours`, `slaComplianceRate`.
- **`CategoryMetricResponse`**: Distribución y tasa de resolución por categoría temática (RF-6): `category`, `totalIncidents`, `resolvedCount`, `slaComplianceRate`.

### 1.3 DTOs de Asistencia Inteligente de Recomendación (`org.bulbaseros.dto`)
- **`DepartmentRecommendationResponse`**: Proporciona la sugerencia asistida de departamento responsable para el modal de triaje (RF-15 a RF-17): `suggestedDepartmentId`, `suggestedDepartmentName`, `confidencePercentage` (0.0 a 100.0), `isLowConfidenceWarning` (bandera activa si confianza < 50%) y `reasoning` (justificación sintética de la heurística léxica).
- **`AssistantPerformanceResponse`**: Agrupa métricas de auditoría del motor inteligente (RF-18): `totalRecommendations`, `acceptedRecommendations`, `adoptionRate` (% de adopción) y `averageConfidence` (grado de certeza promedio).

### 1.4 DTO Consolidado de Resumen Analítico (`org.bulbaseros.dto`)
- **`AnalyticsSummaryResponse`**: Objeto unificado que ensambla todos los bloques analíticos (`operationalMetrics`, `territorialMetrics`, `departmentMetrics`, `categoryMetrics`, `assistantPerformance`), el listado de incidencias filtradas (`filteredIncidents`), los filtros aplicados (`appliedFilters`) y la marca temporal de generación (`generatedAt`), dando soporte directo a la visualización reactiva y a la compilación del informe en PDF (RF-13).

### 1.5 Componente Mapeador de Agregación (`org.bulbaseros.mappers.AnalyticsMapper`)
- Componente `@Component` de Spring que extiende la clase abstracta genérica `BaseMapper<Incidencia, AnalyticsSummaryResponse>`.
- Proporciona métodos null-safe con Javadoc completo:
  - `toDto(Incidencia entity)`: Conversión unitaria de una incidencia a su resumen representativo.
  - `toSummaryResponse(List<Incidencia> incidents, AnalyticsFilterRequest appliedFilters)`: Agregación autónoma sobre colecciones de incidencias.
  - `toSummaryResponse(incidents, operationalMetrics, territorialMetrics, ...)`: Ensamblado flexible de bloques precomputados.
  - `toOperationalMetrics(incidents, mtttHours, mttrHours, ...)`: Cálculo seguro de totales y tasas con protección ante denominadores cero.
  - `toTerritorialMetricList(incidents, groupByNeighborhood)`: Agrupación territorial dinámica asignando `"Sin distrito especificado"` o `"Sin barrio especificado"` ante valores nulos (respetando casos límite de la especificación).
  - `toCategoryMetricList(incidents)`: Agrupación temática asignando `"Sin categoría"` ante valores nulos.
  - `toIncidentResponseList(incidents)`: Conversión segura delegada en `IncidentMapper`.
  - `toDepartmentMetricResponse(...)`: Fábrica segura de métricas departamentales con valores por defecto.

### 1.6 Batería de Pruebas Unitarias
- **`AnalyticsDtoValidationTest`** (14 tests): Valida enums, Bean Validation, restricciones de tamaño, rangos de años (2000-2100), inmutabilidad/constructores y contratos `equals`/`hashCode`/`toString` de los 8 DTOs y el enum.
- **`AnalyticsMapperTest`** (19 tests): Valida casos nulos, listas vacías, conversiones unitarias y de listas, agregaciones por distrito y barrio con etiquetas por defecto ante nulos, agregaciones por categoría, ensamblado completo y resiliencia de constructores.

---

## 2. Decisiones Técnicas y Arquitectónicas

1. **Extensión limpia de `BaseMapper<Incidencia, AnalyticsSummaryResponse>`:** Se mantiene total consistencia con el diseño de mapeadores de UrbanPulse (`BaseMapper`), permitiendo reutilizar `toDtoList()` para transformaciones lote unitarias y ofreciendo métodos de agregación dimensional específicos.
2. **Resiliencia ante conjuntos vacíos y nulos (Sin divisiones por cero):** Todos los métodos de `AnalyticsMapper` salvaguardan denominadores cero (`total > 0 ? ... : 0.0`), asegurando que periodos sin registros devuelvan contadores a 0 y listas vacías inmutables en vez de lanzar excepciones matemáticas o `NullPointerException`.
3. **Mapeo explícito de demarcaciones sin asignar:** De acuerdo con la especificación y casos límite (línea 69 de `spec.md`), las incidencias sin distrito o barrio se agrupan deterministamente bajo las constantes `DEFAULT_UNKNOWN_DISTRICT` (*"Sin distrito especificado"*) y `DEFAULT_UNKNOWN_NEIGHBORHOOD` (*"Sin barrio especificado"*), impidiendo fugas de datos en auditorías municipales.

---

## 3. Verificación y Resultados de Tests

Se ejecutó la suite completa de pruebas del backend en Java 21 con Maven (`mvn test`):

```text
[INFO] Running org.bulbaseros.dto.AnalyticsDtoValidationTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running org.bulbaseros.mappers.AnalyticsMapperTest
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
...
[INFO] Results:
[INFO] Tests run: 504, Failures: 0, Errors: 0, Skipped: 1
[INFO] BUILD SUCCESS
```

- **Tests específicos de Tarea 1:** 33 pruebas unitarias (14 en `AnalyticsDtoValidationTest` y 19 en `AnalyticsMapperTest`).
- **Tests totales del Backend:** 504 pruebas (100% pasando en verde, 0 fallos, 0 errores, 1 test de integración con contenedor PostgreSQL omitido intencionalmente).
- **Cobertura JaCoCo:** Reporte actualizado satisfactoriamente en `Backend/target/site/jacoco/index.html`.

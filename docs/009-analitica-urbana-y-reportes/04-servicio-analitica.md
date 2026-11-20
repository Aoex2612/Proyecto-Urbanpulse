# Tarea 4: Servicio Central de Analítica, Evaluación de SLAs y Agregaciones

**Feature:** `009-analitica-urbana-y-reportes`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-1, RF-2, RF-3, RF-4, RF-5, RF-6, RF-7, RF-8, RF-9, RF-10, RF-11, RF-18  

---

## 1. Resumen de la Tarea

En esta tarea se ha diseñado e implementado el servicio central de analítica urbana (`AnalyticsService`) en el backend Spring Boot 3 con Java 21, responsable de orquestar el cálculo de indicadores clave de rendimiento (KPIs), la evaluación determinista de acuerdos de nivel de servicio (SLA) por prioridad, las agregaciones multidimensionales (departamental, territorial y temática) y la integración del rendimiento del asistente inteligente de recomendación.

### 1.1 Componente Central Desarrollado (`org.bulbaseros.service.AnalyticsService`)
- Componente `@Service` con inyección explícita mediante `@Autowired` de:
  - `IncidenciaRepository`: Filtrado combinado por intervalo de tiempo y delimitación territorial.
  - `CambiosEstadoRepository`: Auditoría cronológica de transiciones de estado para triaje y resolución.
  - `TareasRepository`: Asignaciones operativas históricas y vigentes por departamento.
  - `DepartamentoRepository`: Catálogo municipal de departamentos ordenados alfabéticamente.
  - `IntelligentRecommendationService`: Evaluación de desempeño y adopción de sugerencias.
  - `AnalyticsMapper`: Mapeador de agregaciones y serialización de DTOs consolidados.
  - `Clock`: Reloj temporal parametrizable para pruebas unitarias deterministas.

---

## 2. Métricas Operativas y Cálculo de Tiempos Medios

### 2.1 Tiempo Medio de Triaje (MTTT, RF-1)
- **Definición:** Diferencia temporal en horas entre la creación de la incidencia (`createdAt`) y la primera transición de estado hacia `VALIDATED` o `REJECTED`.
- **Algoritmo:**
  1. Se recuperan en bloque (`findByIncidentIdInOrderByCreatedAtAsc`) las transiciones de estado de las incidencias filtradas.
  2. Para cada incidencia, se identifica el primer cambio de estado con `toStatus` igual a `VALIDATED` o `REJECTED`.
  3. Delta en horas: $\text{deltaHours} = \max(0.0, \frac{\text{segundos}(t_{\text{triaje}} - t_{\text{creación}})}{3600.0})$.
  4. Promedio en horas redondeado a 1 decimal: $\text{MTTT} = \frac{\sum \text{deltaHours}}{N_{\text{triadas}}}$.
  5. Si ninguna incidencia ha sido triada, retorna de forma segura `0.0`.

### 2.2 Tiempo Medio de Resolución (MTTR, RF-2)
- **Definición:** Diferencia temporal en horas entre la validación o primera asignación y la resolución formal (`RESOLVED` o `CLOSED`).
- **Población Objetivo:** Exclusivamente incidencias con estado `RESOLVED` o `CLOSED`.
- **Algoritmo:**
  1. Marca de inicio de resolución ($t_{\text{inicio}}$):
     - Primer cambio de estado con `toStatus = "VALIDATED"`.
     - Si no hubo cambio explícito a `VALIDATED`, se utiliza la primera asignación departamental registrada en `Tareas` (`assignedAt` mínimo).
     - Si no existen tareas registradas, se emplea la fecha de creación `createdAt`.
  2. Marca de resolución ($t_{\text{resolución}}$):
     - Primer cambio con `toStatus = "RESOLVED"`.
     - Si no existe, primer cambio con `toStatus = "CLOSED"`.
     - Si no existen transiciones explícitas, se toma `updatedAt`.
  3. Delta en horas: $\text{deltaHours} = \max(0.0, \frac{\text{segundos}(t_{\text{resolución}} - t_{\text{inicio}})}{3600.0})$.
  4. Promedio en horas redondeado a 1 decimal: $\text{MTTR} = \frac{\sum \text{deltaHours}}{N_{\text{resueltas}}}$.
  5. Si no hay incidencias resueltas, retorna de forma segura `0.0`.

---

## 3. Matriz Determinista de Evaluación de SLAs por Prioridad (RF-3, RF-4, RF-5)

### 3.1 Umbrales Máximos de Resolución
Conforme a la normativa y a los requisitos RF-3, se definen los siguientes límites en horas:
| Prioridad | Plazo Máximo Natural | Umbral en Horas (`SLA Threshold`) |
| :--- | :--- | :--- |
| `CRITICAL` | 3 días naturales | **72.0 horas** |
| `HIGH` | 7 días naturales | **168.0 horas** |
| `MEDIUM` | 15 días naturales | **360.0 horas** |
| `LOW` | 30 días naturales | **720.0 horas** |
| *Nulo* | Fallback a `MEDIUM` | **360.0 horas** |

### 3.2 Lógica de Evaluación por Incidencia
1. **Incidencias Resueltas (`RESOLVED` o `CLOSED`):**
   - Tiempo total transcurrido: $\Delta t = t_{\text{resolución}} - t_{\text{creación}}$.
   - Si $\Delta t \le \text{Umbral}$: Se computa como **cumplida** (`totalSlaCompliant++`).
   - Si $\Delta t > \text{Umbral}$: Se computa como **fuera de plazo** (`totalSlaBreached++`).
2. **Incidencias en Curso (`REPORTED`, `VALIDATED`, `ASSIGNED`, `IN_PROGRESS`, `REOPENED`):**
   - Tiempo transcurrido actual: $\Delta t = t_{\text{now}} - t_{\text{creación}}$.
   - Si $\Delta t > \text{Umbral}$: Se detecta y contabiliza como **incumplimiento en curso** (`totalSlaBreached++`).
   - Si $\Delta t \le \text{Umbral}$: Se contabiliza como **en plazo vigente** (`totalSlaCompliant++`).
3. **Incidencias Rechazadas (`REJECTED`):**
   - Descartadas del cómputo de SLA de resolución por no requerir ejecución técnica.
4. **Tasa Global de Cumplimiento de SLA (RF-5):**
   $$\text{slaComplianceRate} = \begin{cases} 
   \text{roundOneDecimal}\left(\frac{\text{totalSlaCompliant} \times 100.0}{\text{totalEvaluated}}\right) & \text{si } \text{totalEvaluated} > 0 \\ 
   0.0 & \text{si } \text{totalEvaluated} = 0 
   \end{cases}$$

---

## 4. Agregaciones Multidimensionales y Desgloses

### 4.1 Desglose Departamental (RF-6, RF-10)
- Para cada departamento del catálogo municipal (`departamentoRepository.findAllByOrderByNameAsc()`):
  - Se obtienen las incidencias filtradas que poseen asignación activa o histórica a dicho departamento (`tareasRepository.findByIncidentIdIn`).
  - Se computan:
    - `totalAssigned`: Total de incidencias del filtro asignadas.
    - `resolvedCount`: Incidencias resueltas o cerradas del departamento.
    - `avgResolutionHours`: Tiempo medio de resolución específico del departamento.
    - `slaComplianceRate`: Tasa de cumplimiento de SLA específica de las incidencias del departamento.
  - Para departamentos sin incidencias asignadas en el filtro, se garantiza retorno limpio con contadores a 0 y porcentajes en 0.0.

### 4.2 Desglose Territorial (RF-7, RF-10)
- **Criterio de agrupación dinámico:**
  - Si el filtro contiene un `district` específico y no especifica `neighborhood`: se agrupa por Barrio (`groupByNeighborhood = true`).
  - En cualquier otro caso: se agrupa por Distrito (`groupByNeighborhood = false`).
- Las incidencias sin demarcación geográfica se agrupan bajo *"Sin distrito especificado"* o *"Sin barrio especificado"*.

### 4.3 Desglose Categórico (RF-6)
- Agrupación por categoría temática calculando total de incidencias, incidencias resueltas y porcentaje de resolución.
- Ordenación descendente por volumen total de incidencias.

### 4.4 Integración con Asistencia Inteligente (RF-18)
- Delegación en `intelligentRecommendationService.evaluatePerformance(incidents)` para consolidar:
  - `totalRecommendations`: Total de sugerencias emitidas.
  - `acceptedRecommendations`: Sugerencias coincidentes con la asignación real.
  - `adoptionRate`: Tasa porcentual de adopción por los operadores.
  - `averageConfidence`: Confianza media estimada por el analizador heurístico.

---

## 5. Resolución de Rangos Temporales y Resiliencia (RF-8, RF-9, RF-11)

### 5.1 Resolución de Intervalos Temporales (`resolveDateRange`)
- Prioridad 1: Fechas explícitas `startDate` y `endDate` en la solicitud.
- Prioridad 2: `preset` configurado en `AnalyticsFilterRequest`:
  - `TODAY`: Desde las 00:00:00 UTC de la fecha actual hasta las 23:59:59.999999999 UTC.
  - `LAST_7_DAYS`: `now.minusDays(7)` a `now`.
  - `LAST_30_DAYS`: `now.minusDays(30)` a `now`.
  - `LAST_3_MONTHS`: `now.minusMonths(3)` a `now`.
  - `LAST_6_MONTHS`: `now.minusMonths(6)` a `now`.
  - `LAST_YEAR`: `now.minusYears(1)` a `now`.
  - `SPECIFIC_YEAR`: 1 de enero 00:00:00 UTC al 31 de diciembre 23:59:59.999999999 UTC del año solicitado (o año actual si es nulo).
- Prioridad 3: Si no se definen fechas ni preset, aplica por defecto `LAST_30_DAYS`.

### 5.2 Resiliencia ante Conjuntos Vacíos (RF-11)
- Si el repositorio retorna lista vacía (`Collections.emptyList()`) o `null`:
  - Se retorna de forma inmediata `analyticsMapper.toSummaryResponse(Collections.emptyList(), filterRequest)`.
  - Se omiten consultas innecesarias de cambios de estado y tareas.
  - Todas las métricas numéricas reflejan `0.0`, contadores en `0L` y colecciones vacías (`[]`), previniendo errores de división por cero (`NaN` / `Infinity`).

---

## 6. Pruebas Unitarias y Verificación

Se ha creado la suite exhaustiva `AnalyticsServiceTest` con JUnit 5 y Mockito, estructurada en 7 clases anidadas (`@Nested`):

1. **`MtttAndMttrCalculations` (5 tests):**
   - Cálculo correcto de MTTT con eventos de validación y rechazo.
   - Retorno de 0.0 cuando ninguna incidencia ha sido triada.
   - Cálculo de MTTR en incidencias resueltas (`RESOLVED` y `CLOSED`).
   - Fallback a asignación o creación ante ausencia de cambio a `VALIDATED`.
   - Retorno de 0.0 cuando no hay incidencias resueltas.
2. **`SlaEvaluations` (5 tests):**
   - 100% cumplimiento con todas las prioridades en tiempo.
   - 0% cumplimiento con todas las prioridades fuera de plazo.
   - Detección de incumplimientos e incidencias en plazo para reportes en curso contra `now`.
   - Fallback determinista de prioridad nula a `MEDIUM` (360 horas).
   - Exclusión de incidencias rechazadas del cómputo de SLA de resolución.
3. **`EmptyAndResilienceCases` (2 tests):**
   - Retorno de métricas a cero sin divisiones por cero ante lista vacía.
   - Manejo seguro ante retorno nulo del repositorio.
4. **`DatePresetResolutions` (10 tests):**
   - Verificación de límites de fecha para `TODAY`, `LAST_7_DAYS`, `LAST_30_DAYS`, `LAST_3_MONTHS`, `LAST_6_MONTHS`, `LAST_YEAR` y `SPECIFIC_YEAR`.
   - Precedencia de fechas manuales personalizadas.
   - Aplicación de `LAST_30_DAYS` por defecto ante parámetros ausentes.
5. **`DepartmentalMetrics` (1 test):**
   - Cálculo correcto para departamentos con incidencias asignadas y departamentos sin carga operativa.
6. **`TerritorialAndCategoryMetrics` (3 tests):**
   - Agrupación por distrito cuando no se especifica delimitación.
   - Agrupación reactiva por barrio cuando se especifica distrito.
   - Agrupación y ordenación por categoría temática.
7. **`AssistantPerformanceIntegration` (1 test):**
   - Integración consolidada con `IntelligentRecommendationService`.

### Resultados de Ejecución:
```
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
Total time: 11.902 s (BUILD SUCCESS)

Suite completa Backend:
Tests run: 556, Failures: 0, Errors: 0, Skipped: 1
Total time: 22.799 s (BUILD SUCCESS)
```

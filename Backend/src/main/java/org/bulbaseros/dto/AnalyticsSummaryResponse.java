package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Objeto de transferencia de datos (DTO) consolidado que agrupa todos los bloques analíticos,
 * métricas operacionales, desgloses espaciales y temáticos, rendimiento de la asistencia inteligente
 * y el listado de incidencias filtradas para la visualización en panel y la exportación en PDF.
 * <p>
 * Representa la respuesta integral entregada por el endpoint principal de analítica urbana
 * {@code GET /api/analytics/summary} en cumplimiento de los requisitos RF-1 a RF-14.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryResponse {

    /**
     * Bloque de métricas operacionales agregadas, tiempos medios (MTTT, MTTR) y cumplimiento de SLAs.
     */
    private OperationalMetricsResponse operationalMetrics;

    /**
     * Desglose de métricas agregadas por demarcación territorial (distrito municipal o barrio).
     */
    private List<TerritorialMetricResponse> territorialMetrics;

    /**
     * Desglose de volumen de trabajo, tiempos de resolución y cumplimiento de SLA por departamento municipal.
     */
    private List<DepartmentMetricResponse> departmentMetrics;

    /**
     * Distribución cuantitativa y tasa de resolución de incidencias por categoría temática.
     */
    private List<CategoryMetricResponse> categoryMetrics;

    /**
     * Métricas consolidadas de desempeño, fiabilidad y adopción del motor de asistencia inteligente.
     */
    private AssistantPerformanceResponse assistantPerformance;

    /**
     * Listado detallado de las incidencias que satisfacen los filtros temporales y territoriales aplicados.
     */
    private List<IncidentResponse> filteredIncidents;

    /**
     * Parámetros y criterios de filtrado aplicados en la consulta.
     */
    private AnalyticsFilterRequest appliedFilters;

    /**
     * Marca temporal exacta de generación de la respuesta analítica.
     */
    private OffsetDateTime generatedAt;
}

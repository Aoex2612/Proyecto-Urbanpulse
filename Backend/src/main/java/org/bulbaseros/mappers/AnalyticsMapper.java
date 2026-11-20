package org.bulbaseros.mappers;

import org.bulbaseros.dto.AnalyticsFilterRequest;
import org.bulbaseros.dto.AnalyticsSummaryResponse;
import org.bulbaseros.dto.AssistantPerformanceResponse;
import org.bulbaseros.dto.CategoryMetricResponse;
import org.bulbaseros.dto.DepartmentMetricResponse;
import org.bulbaseros.dto.IncidentResponse;
import org.bulbaseros.dto.OperationalMetricsResponse;
import org.bulbaseros.dto.TerritorialMetricResponse;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Componente mapeador desacoplado de Spring para la transformación y agregación de entidades
 * {@link Incidencia} hacia los objetos de transferencia de datos (DTOs) de analítica urbana.
 * <p>
 * Extiende la clase abstracta {@link BaseMapper} parametrizada para la conversión de {@link Incidencia}
 * a {@link AnalyticsSummaryResponse}, proveyendo métodos auxiliares completamente seguros ante nulos
 * ({@code null-safe}) para sintetizar métricas operativas, agregaciones territoriales por distrito o barrio,
 * métricas temáticas por categoría y consolidación final para la exportación de informes oficiales.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Component
public class AnalyticsMapper extends BaseMapper<Incidencia, AnalyticsSummaryResponse> {

    /**
     * Etiqueta por defecto para incidencias sin distrito municipal asignado.
     */
    public static final String DEFAULT_UNKNOWN_DISTRICT = "Sin distrito especificado";

    /**
     * Etiqueta por defecto para incidencias sin barrio municipal asignado.
     */
    public static final String DEFAULT_UNKNOWN_NEIGHBORHOOD = "Sin barrio especificado";

    /**
     * Etiqueta por defecto para incidencias sin categoría temática asignada.
     */
    public static final String DEFAULT_UNKNOWN_CATEGORY = "Sin categoría";

    private final IncidentMapper incidentMapper;

    /**
     * Constructor por defecto para pruebas unitarias desacopladas sin contexto Spring.
     */
    public AnalyticsMapper() {
        this(new IncidentMapper());
    }

    /**
     * Constructor principal con inyección opcional del mapeador de incidencias individuales.
     *
     * @param incidentMapper Mapeador de entidades {@link Incidencia} a {@link IncidentResponse}.
     */
    @Autowired
    public AnalyticsMapper(@Autowired(required = false) IncidentMapper incidentMapper) {
        this.incidentMapper = (incidentMapper != null) ? incidentMapper : new IncidentMapper();
    }

    /**
     * Transforma una única entidad {@link Incidencia} en un DTO consolidado {@link AnalyticsSummaryResponse}.
     * <p>
     * Si la entidad proporcionada es nula, retorna de forma segura {@code null}. En caso contrario,
     * encapsula la incidencia en un resumen analítico con sus agregaciones elementales computadas.
     * </p>
     *
     * @param entity Entidad de incidencia origen, o {@code null}.
     * @return DTO {@link AnalyticsSummaryResponse} representativo, o {@code null} si la entidad es nula.
     */
    @Override
    public AnalyticsSummaryResponse toDto(Incidencia entity) {
        if (entity == null) {
            return null;
        }
        return toSummaryResponse(Collections.singletonList(entity), (AnalyticsFilterRequest) null);
    }

    /**
     * Genera un DTO consolidado {@link AnalyticsSummaryResponse} calculando de manera autónoma
     * las agregaciones operativas, territoriales y categóricas a partir de una lista de incidencias.
     *
     * @param incidents      Lista de incidencias filtradas, o {@code null}.
     * @param appliedFilters Criterios de filtrado aplicados en la consulta, o {@code null}.
     * @return DTO {@link AnalyticsSummaryResponse} consolidado, nunca {@code null}.
     */
    public AnalyticsSummaryResponse toSummaryResponse(List<Incidencia> incidents, AnalyticsFilterRequest appliedFilters) {
        if (incidents == null || incidents.isEmpty()) {
            return AnalyticsSummaryResponse.builder()
                    .operationalMetrics(toOperationalMetrics(Collections.emptyList(), 0.0, 0.0, 0.0, 0L, 0L))
                    .territorialMetrics(Collections.emptyList())
                    .departmentMetrics(Collections.emptyList())
                    .categoryMetrics(Collections.emptyList())
                    .assistantPerformance(AssistantPerformanceResponse.builder()
                            .totalRecommendations(0L)
                            .acceptedRecommendations(0L)
                            .adoptionRate(0.0)
                            .averageConfidence(0.0)
                            .build())
                    .filteredIncidents(Collections.emptyList())
                    .appliedFilters(appliedFilters)
                    .generatedAt(OffsetDateTime.now())
                    .build();
        }

        OperationalMetricsResponse operational = toOperationalMetrics(incidents, 0.0, 0.0, 100.0, 0L, (long) incidents.size());
        List<TerritorialMetricResponse> territorial = toTerritorialMetricList(incidents, false);
        List<CategoryMetricResponse> categories = toCategoryMetricList(incidents);
        List<IncidentResponse> incidentDtos = toIncidentResponseList(incidents);

        return AnalyticsSummaryResponse.builder()
                .operationalMetrics(operational)
                .territorialMetrics(territorial)
                .departmentMetrics(Collections.emptyList())
                .categoryMetrics(categories)
                .assistantPerformance(AssistantPerformanceResponse.builder()
                        .totalRecommendations(0L)
                        .acceptedRecommendations(0L)
                        .adoptionRate(0.0)
                        .averageConfidence(0.0)
                        .build())
                .filteredIncidents(incidentDtos)
                .appliedFilters(appliedFilters)
                .generatedAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Ensambla un {@link AnalyticsSummaryResponse} consolidando bloques precalculados
     * de métricas operacionales, espaciales, departamentales y de asistencia inteligente.
     *
     * @param incidents            Lista de entidades de incidencia asociadas al informe.
     * @param operationalMetrics   Bloque de métricas operacionales agregadas.
     * @param territorialMetrics   Desglose métrico por distrito o barrio municipal.
     * @param departmentMetrics    Rendimiento por área o departamento técnico.
     * @param categoryMetrics      Distribución temática por categoría.
     * @param assistantPerformance Indicadores de desempeño del asistente de recomendaciones.
     * @param appliedFilters       Filtros aplicados en la consulta.
     * @return DTO consolidado listo para presentación o serialización.
     */
    public AnalyticsSummaryResponse toSummaryResponse(
            List<Incidencia> incidents,
            OperationalMetricsResponse operationalMetrics,
            List<TerritorialMetricResponse> territorialMetrics,
            List<DepartmentMetricResponse> departmentMetrics,
            List<CategoryMetricResponse> categoryMetrics,
            AssistantPerformanceResponse assistantPerformance,
            AnalyticsFilterRequest appliedFilters) {

        OperationalMetricsResponse safeOperational = (operationalMetrics != null)
                ? operationalMetrics
                : toOperationalMetrics(incidents, 0.0, 0.0, 0.0, 0L, 0L);

        List<TerritorialMetricResponse> safeTerritorial = (territorialMetrics != null)
                ? territorialMetrics
                : Collections.emptyList();

        List<DepartmentMetricResponse> safeDepartments = (departmentMetrics != null)
                ? departmentMetrics
                : Collections.emptyList();

        List<CategoryMetricResponse> safeCategories = (categoryMetrics != null)
                ? categoryMetrics
                : Collections.emptyList();

        AssistantPerformanceResponse safeAssistant = (assistantPerformance != null)
                ? assistantPerformance
                : AssistantPerformanceResponse.builder()
                .totalRecommendations(0L)
                .acceptedRecommendations(0L)
                .adoptionRate(0.0)
                .averageConfidence(0.0)
                .build();

        List<IncidentResponse> safeFilteredIncidents = toIncidentResponseList(incidents);

        return AnalyticsSummaryResponse.builder()
                .operationalMetrics(safeOperational)
                .territorialMetrics(safeTerritorial)
                .departmentMetrics(safeDepartments)
                .categoryMetrics(safeCategories)
                .assistantPerformance(safeAssistant)
                .filteredIncidents(safeFilteredIncidents)
                .appliedFilters(appliedFilters)
                .generatedAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Construye un objeto {@link OperationalMetricsResponse} calculando totales de incidencias
     * resueltas y pendientes a partir del listado suministrado, asignando métricas avanzadas.
     *
     * @param incidents          Lista de incidencias evaluadas, o {@code null}.
     * @param mtttHours          Tiempo medio de triaje calculado en horas.
     * @param mttrHours          Tiempo medio de resolución calculado en horas.
     * @param slaComplianceRate  Porcentaje de cumplimiento de SLA (0.0 a 100.0).
     * @param totalSlaBreached   Número total de incidencias fuera de plazo.
     * @param totalSlaCompliant  Número total de incidencias cumplidas en plazo.
     * @return DTO {@link OperationalMetricsResponse} estructurado, nunca {@code null}.
     */
    public OperationalMetricsResponse toOperationalMetrics(
            List<Incidencia> incidents,
            Double mtttHours,
            Double mttrHours,
            Double slaComplianceRate,
            Long totalSlaBreached,
            Long totalSlaCompliant) {

        if (incidents == null || incidents.isEmpty()) {
            return OperationalMetricsResponse.builder()
                    .totalIncidents(0L)
                    .resolvedIncidents(0L)
                    .pendingIncidents(0L)
                    .mtttHours(0.0)
                    .mttrHours(0.0)
                    .slaComplianceRate(0.0)
                    .totalSlaBreached(0L)
                    .totalSlaCompliant(0L)
                    .build();
        }

        long total = incidents.stream().filter(Objects::nonNull).count();
        long resolved = incidents.stream()
                .filter(Objects::nonNull)
                .filter(i -> i.getStatus() == EstadoIncidencia.RESOLVED || i.getStatus() == EstadoIncidencia.CLOSED)
                .count();
        long pending = Math.max(0L, total - resolved);

        double safeMttt = (mtttHours != null) ? mtttHours : 0.0;
        double safeMttr = (mttrHours != null) ? mttrHours : 0.0;
        long safeBreached = (totalSlaBreached != null) ? totalSlaBreached : 0L;
        long safeCompliant = (totalSlaCompliant != null) ? totalSlaCompliant : Math.max(0L, total - safeBreached);
        double safeSlaRate = (slaComplianceRate != null)
                ? slaComplianceRate
                : (total > 0 ? (safeCompliant * 100.0 / total) : 0.0);

        return OperationalMetricsResponse.builder()
                .totalIncidents(total)
                .resolvedIncidents(resolved)
                .pendingIncidents(pending)
                .mtttHours(safeMttt)
                .mttrHours(safeMttr)
                .slaComplianceRate(safeSlaRate)
                .totalSlaBreached(safeBreached)
                .totalSlaCompliant(safeCompliant)
                .build();
    }

    /**
     * Agrupa una lista de incidencias por territorio (Distrito o Barrio) generando una lista
     * ordenada de {@link TerritorialMetricResponse}.
     * <p>
     * Asigna automáticamente el texto descriptivo por defecto para incidencias sin territorio especificado.
     * </p>
     *
     * @param incidents            Lista de incidencias a agregar.
     * @param groupByNeighborhood  {@code true} para agrupar por Barrio, {@code false} para agrupar por Distrito.
     * @return Lista agregada de {@link TerritorialMetricResponse}, nunca {@code null}.
     */
    public List<TerritorialMetricResponse> toTerritorialMetricList(List<Incidencia> incidents, boolean groupByNeighborhood) {
        if (incidents == null || incidents.isEmpty()) {
            return Collections.emptyList();
        }

        String defaultName = groupByNeighborhood ? DEFAULT_UNKNOWN_NEIGHBORHOOD : DEFAULT_UNKNOWN_DISTRICT;

        Map<String, List<Incidencia>> grouped = incidents.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(i -> {
                    String val = groupByNeighborhood ? i.getNeighborhood() : i.getDistrict();
                    return (val != null && !val.trim().isEmpty()) ? val.trim() : defaultName;
                }));

        return grouped.entrySet().stream()
                .map(entry -> {
                    String territoryName = entry.getKey();
                    List<Incidencia> groupIncidents = entry.getValue();
                    long total = groupIncidents.size();
                    long resolved = groupIncidents.stream()
                            .filter(i -> i.getStatus() == EstadoIncidencia.RESOLVED || i.getStatus() == EstadoIncidencia.CLOSED)
                            .count();
                    long pending = Math.max(0L, total - resolved);
                    double rate = total > 0 ? (resolved * 100.0 / total) : 0.0;

                    return TerritorialMetricResponse.builder()
                            .territoryName(territoryName)
                            .totalIncidents(total)
                            .resolvedCount(resolved)
                            .pendingCount(pending)
                            .slaComplianceRate(rate)
                            .build();
                })
                .sorted(Comparator.comparing(TerritorialMetricResponse::getTotalIncidents).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Agrupa una colección de incidencias por categoría temática produciendo una lista
     * ordenada de {@link CategoryMetricResponse}.
     *
     * @param incidents Lista de incidencias a agregar.
     * @return Lista de DTOs {@link CategoryMetricResponse}, nunca {@code null}.
     */
    public List<CategoryMetricResponse> toCategoryMetricList(List<Incidencia> incidents) {
        if (incidents == null || incidents.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<Incidencia>> grouped = incidents.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(i -> {
                    String cat = i.getCategory();
                    return (cat != null && !cat.trim().isEmpty()) ? cat.trim() : DEFAULT_UNKNOWN_CATEGORY;
                }));

        return grouped.entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    List<Incidencia> groupIncidents = entry.getValue();
                    long total = groupIncidents.size();
                    long resolved = groupIncidents.stream()
                            .filter(i -> i.getStatus() == EstadoIncidencia.RESOLVED || i.getStatus() == EstadoIncidencia.CLOSED)
                            .count();
                    double rate = total > 0 ? (resolved * 100.0 / total) : 0.0;

                    return CategoryMetricResponse.builder()
                            .category(category)
                            .totalIncidents(total)
                            .resolvedCount(resolved)
                            .slaComplianceRate(rate)
                            .build();
                })
                .sorted(Comparator.comparing(CategoryMetricResponse::getTotalIncidents).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Convierte una lista de entidades {@link Incidencia} en una lista de DTOs {@link IncidentResponse}
     * delegando de forma segura en {@link IncidentMapper}.
     *
     * @param incidents Lista de entidades de persistencia, o {@code null}.
     * @return Lista de {@link IncidentResponse}, nunca {@code null}.
     */
    public List<IncidentResponse> toIncidentResponseList(List<Incidencia> incidents) {
        if (incidents == null || incidents.isEmpty()) {
            return Collections.emptyList();
        }
        return incidents.stream()
                .filter(Objects::nonNull)
                .map(incidentMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Método fábrica para construir una instancia de {@link DepartmentMetricResponse}
     * asegurando valores por defecto ante parámetros nulos.
     *
     * @param departmentId       Identificador del departamento.
     * @param departmentName     Nombre oficial del departamento.
     * @param totalAssigned      Total de incidencias asignadas.
     * @param resolvedCount      Total de incidencias resueltas.
     * @param avgResolutionHours Tiempo medio de resolución en horas.
     * @param slaComplianceRate  Porcentaje de cumplimiento de SLA.
     * @return DTO {@link DepartmentMetricResponse} construido.
     */
    public DepartmentMetricResponse toDepartmentMetricResponse(
            Long departmentId,
            String departmentName,
            Long totalAssigned,
            Long resolvedCount,
            Double avgResolutionHours,
            Double slaComplianceRate) {

        return DepartmentMetricResponse.builder()
                .departmentId(departmentId)
                .departmentName(departmentName)
                .totalAssigned((totalAssigned != null) ? totalAssigned : 0L)
                .resolvedCount((resolvedCount != null) ? resolvedCount : 0L)
                .avgResolutionHours((avgResolutionHours != null) ? avgResolutionHours : 0.0)
                .slaComplianceRate((slaComplianceRate != null) ? slaComplianceRate : 0.0)
                .build();
    }
}

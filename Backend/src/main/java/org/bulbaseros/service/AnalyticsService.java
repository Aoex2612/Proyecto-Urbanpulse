package org.bulbaseros.service;

import lombok.extern.slf4j.Slf4j;
import org.bulbaseros.dto.AnalyticsFilterRequest;
import org.bulbaseros.dto.AnalyticsSummaryResponse;
import org.bulbaseros.dto.AnalyticsTimePreset;
import org.bulbaseros.dto.AssistantPerformanceResponse;
import org.bulbaseros.dto.CategoryMetricResponse;
import org.bulbaseros.dto.DepartmentMetricResponse;
import org.bulbaseros.dto.OperationalMetricsResponse;
import org.bulbaseros.dto.TerritorialMetricResponse;
import org.bulbaseros.entity.CambiosEstado;
import org.bulbaseros.entity.Departamento;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.bulbaseros.entity.Tareas;
import org.bulbaseros.mappers.AnalyticsMapper;
import org.bulbaseros.repository.CambiosEstadoRepository;
import org.bulbaseros.repository.DepartamentoRepository;
import org.bulbaseros.repository.IncidenciaRepository;
import org.bulbaseros.repository.TareasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Servicio central de analítica urbana, cálculo de indicadores clave de rendimiento (KPIs),
 * evaluación determinista de acuerdos de nivel de servicio (SLA) y agregaciones multifactoriales.
 * <p>
 * Implementa el cálculo de tiempos medios operacionales como el Tiempo Medio de Triaje (MTTT, RF-1)
 * y el Tiempo Medio de Resolución (MTTR, RF-2) a partir de trazas inmutables de auditoría de estado.
 * </p>
 * <p>
 * Evalúa el cumplimiento de SLAs por nivel de prioridad según los umbrales reglamentarios municipales
 * (CRITICAL: 72h / 3d, HIGH: 168h / 7d, MEDIUM: 360h / 15d, LOW: 720h / 30d) detectando incumplimientos
 * tanto en incidencias cerradas como en incidencias en curso (RF-3, RF-4, RF-5).
 * </p>
 * <p>
 * Asimismo, genera desgloses agregados por departamento municipal (RF-6, RF-10), por territorio
 * (distrito o barrio, RF-7), por categoría temática (RF-6) e integra el rendimiento del asistente
 * inteligente de recomendación (RF-18), garantizando resiliencia absoluta ante conjuntos vacíos (RF-11).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Service
@Slf4j
public class AnalyticsService {

    /**
     * Umbral máximo de resolución SLA para incidencias de prioridad CRITICAL (3 días naturales = 72 horas).
     */
    public static final double SLA_CRITICAL_HOURS = 72.0;

    /**
     * Umbral máximo de resolución SLA para incidencias de prioridad HIGH (7 días naturales = 168 horas).
     */
    public static final double SLA_HIGH_HOURS = 168.0;

    /**
     * Umbral máximo de resolución SLA para incidencias de prioridad MEDIUM (15 días naturales = 360 horas).
     */
    public static final double SLA_MEDIUM_HOURS = 360.0;

    /**
     * Umbral máximo de resolución SLA para incidencias de prioridad LOW (30 días naturales = 720 horas).
     */
    public static final double SLA_LOW_HOURS = 720.0;

    private final IncidenciaRepository incidenciaRepository;
    private final CambiosEstadoRepository cambiosEstadoRepository;
    private final TareasRepository tareasRepository;
    private final DepartamentoRepository departamentoRepository;
    private final IntelligentRecommendationService intelligentRecommendationService;
    private final AnalyticsMapper analyticsMapper;
    private final Clock clock;

    /**
     * Constructor principal con inyección de dependencias de Spring.
     *
     * @param incidenciaRepository              Repositorio de incidencias urbanas.
     * @param cambiosEstadoRepository           Repositorio de historial de cambios de estado.
     * @param tareasRepository                  Repositorio de tareas y asignaciones operativas.
     * @param departamentoRepository            Repositorio de departamentos municipales.
     * @param intelligentRecommendationService  Servicio de recomendación de asistencia inteligente.
     * @param analyticsMapper                   Mapeador de dominio analítico y agregaciones.
     */
    @Autowired
    public AnalyticsService(
            IncidenciaRepository incidenciaRepository,
            CambiosEstadoRepository cambiosEstadoRepository,
            TareasRepository tareasRepository,
            DepartamentoRepository departamentoRepository,
            IntelligentRecommendationService intelligentRecommendationService,
            AnalyticsMapper analyticsMapper) {
        this(incidenciaRepository, cambiosEstadoRepository, tareasRepository,
                departamentoRepository, intelligentRecommendationService, analyticsMapper, Clock.systemUTC());
    }

    /**
     * Constructor con inyección parametrizada de reloj temporal para facilitar pruebas unitarias deterministas.
     *
     * @param incidenciaRepository              Repositorio de incidencias urbanas.
     * @param cambiosEstadoRepository           Repositorio de historial de cambios de estado.
     * @param tareasRepository                  Repositorio de tareas y asignaciones operativas.
     * @param departamentoRepository            Repositorio de departamentos municipales.
     * @param intelligentRecommendationService  Servicio de recomendación de asistencia inteligente.
     * @param analyticsMapper                   Mapeador de dominio analítico y agregaciones.
     * @param clock                             Reloj temporal del sistema.
     */
    public AnalyticsService(
            IncidenciaRepository incidenciaRepository,
            CambiosEstadoRepository cambiosEstadoRepository,
            TareasRepository tareasRepository,
            DepartamentoRepository departamentoRepository,
            IntelligentRecommendationService intelligentRecommendationService,
            AnalyticsMapper analyticsMapper,
            Clock clock) {
        this.incidenciaRepository = incidenciaRepository;
        this.cambiosEstadoRepository = cambiosEstadoRepository;
        this.tareasRepository = tareasRepository;
        this.departamentoRepository = departamentoRepository;
        this.intelligentRecommendationService = intelligentRecommendationService;
        this.analyticsMapper = analyticsMapper;
        this.clock = (clock != null) ? clock : Clock.systemUTC();
    }

    /**
     * Estructura inmutable que encapsula un intervalo temporal acotado por una fecha de inicio y una fecha de fin.
     *
     * @param startDate Fecha y hora inicial del intervalo (inclusive).
     * @param endDate   Fecha y hora final del intervalo (inclusive).
     */
    public record DateRange(OffsetDateTime startDate, OffsetDateTime endDate) {}

    /**
     * Estructura inmutable que encapsula el resultado cuantitativo de una evaluación de compromisos SLA.
     *
     * @param totalCompliant Número total de incidencias que cumplen satisfactoriamente el SLA.
     * @param totalBreached  Número total de incidencias que incumplen el SLA establecido.
     * @param complianceRate Porcentaje de cumplimiento alcanzado (0.0 a 100.0).
     */
    public record SlaEvaluationResult(long totalCompliant, long totalBreached, double complianceRate) {}

    /**
     * Genera el resumen consolidado de analítica urbana para los parámetros de filtro especificados.
     * <p>
     * Resuelve el rango temporal solicitado (o aplica el preset por defecto de últimos 30 días), consulta
     * las incidencias acotadas territorial y temporalmente, y sintetiza los bloques de KPIs operacionales,
     * distribución territorial (por distrito o barrio), desempeño departamental, desglose por categoría,
     * y evaluación del asistente inteligente.
     * </p>
     *
     * @param filterRequest Criterios de filtrado temporal y territorial aplicados, o {@code null}.
     * @return DTO consolidado {@link AnalyticsSummaryResponse} con todos los bloques analíticos calculados.
     */
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getAnalyticsSummary(AnalyticsFilterRequest filterRequest) {
        log.info("Calculando resumen analítico urbano con filtros: {}", filterRequest);

        DateRange dateRange = resolveDateRange(filterRequest);
        String district = (filterRequest != null && filterRequest.getDistrict() != null && !filterRequest.getDistrict().trim().isEmpty())
                ? filterRequest.getDistrict().trim()
                : null;
        String neighborhood = (filterRequest != null && filterRequest.getNeighborhood() != null && !filterRequest.getNeighborhood().trim().isEmpty())
                ? filterRequest.getNeighborhood().trim()
                : null;

        List<Incidencia> incidents = incidenciaRepository.findByDateRangeAndTerritory(
                dateRange.startDate(),
                dateRange.endDate(),
                district,
                neighborhood
        );

        if (incidents == null || incidents.isEmpty()) {
            log.info("No se registraron incidencias para los filtros aplicados. Retornando resumen vacío resiliente.");
            return analyticsMapper.toSummaryResponse(Collections.emptyList(), filterRequest);
        }

        List<Long> incidentIds = incidents.stream()
                .map(Incidencia::getId)
                .filter(Objects::nonNull)
                .toList();

        List<CambiosEstado> allStatusChanges = incidentIds.isEmpty()
                ? Collections.emptyList()
                : cambiosEstadoRepository.findByIncidentIdInOrderByCreatedAtAsc(incidentIds);

        Map<Long, List<CambiosEstado>> statusChangesByIncident = allStatusChanges.stream()
                .filter(c -> c.getIncident() != null && c.getIncident().getId() != null)
                .collect(Collectors.groupingBy(c -> c.getIncident().getId()));

        List<Tareas> allTasks = incidentIds.isEmpty()
                ? Collections.emptyList()
                : tareasRepository.findByIncidentIdIn(incidentIds);

        Map<Long, List<Tareas>> tasksByIncident = allTasks.stream()
                .filter(t -> t.getIncident() != null && t.getIncident().getId() != null)
                .collect(Collectors.groupingBy(t -> t.getIncident().getId()));

        OffsetDateTime now = OffsetDateTime.now(clock);

        // 1. Cálculo de MTTT (RF-1)
        Double mtttHours = calculateMttt(incidents, statusChangesByIncident);

        // 2. Cálculo de MTTR (RF-2)
        Double mttrHours = calculateMttr(incidents, statusChangesByIncident, tasksByIncident);

        // 3. Evaluación global de SLA (RF-3, RF-4, RF-5)
        SlaEvaluationResult slaResult = evaluateSla(incidents, statusChangesByIncident, now);

        // 4. Desglose departamental (RF-6, RF-10)
        List<Departamento> allDepartments = departamentoRepository.findAllByOrderByNameAsc();
        List<DepartmentMetricResponse> departmentMetrics = calculateDepartmentMetrics(
                incidents,
                allDepartments,
                tasksByIncident,
                statusChangesByIncident,
                now
        );

        // 5. Desglose territorial (RF-7, RF-10)
        boolean hasDistrict = (district != null);
        boolean hasNeighborhood = (neighborhood != null);
        boolean groupByNeighborhood = hasDistrict && !hasNeighborhood;
        List<TerritorialMetricResponse> territorialMetrics = analyticsMapper.toTerritorialMetricList(incidents, groupByNeighborhood);

        // 6. Desglose por categoría temática (RF-6)
        List<CategoryMetricResponse> categoryMetrics = analyticsMapper.toCategoryMetricList(incidents);

        // 7. Rendimiento de la asistencia inteligente (RF-18)
        AssistantPerformanceResponse assistantPerformance = intelligentRecommendationService.evaluatePerformance(incidents);

        // 8. Construcción de métricas operacionales agregadas
        OperationalMetricsResponse operationalMetrics = analyticsMapper.toOperationalMetrics(
                incidents,
                mtttHours,
                mttrHours,
                slaResult.complianceRate(),
                slaResult.totalBreached(),
                slaResult.totalCompliant()
        );

        // 9. Consolidación final
        return analyticsMapper.toSummaryResponse(
                incidents,
                operationalMetrics,
                territorialMetrics,
                departmentMetrics,
                categoryMetrics,
                assistantPerformance,
                filterRequest
        );
    }

    /**
     * Resuelve los límites temporales de inicio y fin a partir de los parámetros de la solicitud.
     * <p>
     * Si se suministran explícitamente {@code startDate} y {@code endDate}, se respetan íntegramente.
     * En caso contrario, resuelve el rango preconfigurado según el valor de {@link AnalyticsTimePreset}.
     * Si ni fechas ni preset son provistos, aplica por defecto {@link AnalyticsTimePreset#LAST_30_DAYS}.
     * </p>
     *
     * @param filterRequest Parámetros de filtro temporal recibidos, o {@code null}.
     * @return Intervalo {@link DateRange} resuelto con marcas temporales UTC, nunca {@code null}.
     */
    public DateRange resolveDateRange(AnalyticsFilterRequest filterRequest) {
        OffsetDateTime now = OffsetDateTime.now(clock);

        if (filterRequest != null && filterRequest.getStartDate() != null && filterRequest.getEndDate() != null) {
            return new DateRange(filterRequest.getStartDate(), filterRequest.getEndDate());
        }

        AnalyticsTimePreset preset = (filterRequest != null) ? filterRequest.getPreset() : null;
        if (preset == null) {
            if (filterRequest != null && filterRequest.getStartDate() != null) {
                return new DateRange(filterRequest.getStartDate(), now);
            }
            if (filterRequest != null && filterRequest.getEndDate() != null) {
                return new DateRange(filterRequest.getEndDate().minusDays(30), filterRequest.getEndDate());
            }
            preset = AnalyticsTimePreset.LAST_30_DAYS;
        }

        return switch (preset) {
            case TODAY -> {
                LocalDate today = now.toLocalDate();
                OffsetDateTime start = today.atStartOfDay().atOffset(ZoneOffset.UTC);
                OffsetDateTime end = today.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);
                yield new DateRange(start, end);
            }
            case LAST_7_DAYS -> new DateRange(now.minusDays(7), now);
            case LAST_30_DAYS -> new DateRange(now.minusDays(30), now);
            case LAST_3_MONTHS -> new DateRange(now.minusMonths(3), now);
            case LAST_6_MONTHS -> new DateRange(now.minusMonths(6), now);
            case LAST_YEAR -> new DateRange(now.minusYears(1), now);
            case SPECIFIC_YEAR -> {
                int targetYear = (filterRequest != null && filterRequest.getYear() != null)
                        ? filterRequest.getYear()
                        : now.getYear();
                OffsetDateTime start = LocalDate.of(targetYear, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
                OffsetDateTime end = LocalDate.of(targetYear, 12, 31).atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);
                yield new DateRange(start, end);
            }
        };
    }

    /**
     * Calcula el Tiempo Medio de Triaje (MTTT) en horas (RF-1).
     * <p>
     * Para cada incidencia de la colección, localiza la primera transición de estado hacia
     * {@link EstadoIncidencia#VALIDATED} o {@link EstadoIncidencia#REJECTED}. La diferencia
     * temporal entre el alta ({@code createdAt}) y dicha validación/rechazo constituye el tiempo de triaje.
     * </p>
     *
     * @param incidents               Lista de incidencias filtradas.
     * @param statusChangesByIncident Mapa de cambios de estado indexados por identificador de incidencia.
     * @return Promedio de MTTT en horas redondeado a 1 decimal, o {@code 0.0} si ninguna fue triada.
     */
    public Double calculateMttt(List<Incidencia> incidents, Map<Long, List<CambiosEstado>> statusChangesByIncident) {
        if (incidents == null || incidents.isEmpty()) {
            return 0.0;
        }

        double totalHours = 0.0;
        long triagedCount = 0L;

        for (Incidencia incident : incidents) {
            if (incident == null || incident.getId() == null || incident.getCreatedAt() == null) {
                continue;
            }

            List<CambiosEstado> changes = (statusChangesByIncident != null)
                    ? statusChangesByIncident.getOrDefault(incident.getId(), Collections.emptyList())
                    : Collections.emptyList();

            OffsetDateTime triageTime = findTriageTime(changes);
            if (triageTime != null) {
                double deltaSeconds = Duration.between(incident.getCreatedAt(), triageTime).toSeconds();
                double deltaHours = Math.max(0.0, deltaSeconds / 3600.0);
                totalHours += deltaHours;
                triagedCount++;
            }
        }

        return (triagedCount > 0) ? roundOneDecimal(totalHours / triagedCount) : 0.0;
    }

    /**
     * Calcula el Tiempo Medio de Resolución (MTTR) en horas (RF-2).
     * <p>
     * Se evalúa exclusivamente para incidencias que han alcanzado el estado de resolución ({@code RESOLVED} o {@code CLOSED}).
     * Computa el tiempo transcurrido desde la validación (o primera asignación / creación) hasta la primera transición a {@code RESOLVED}.
     * </p>
     *
     * @param incidents               Lista de incidencias filtradas.
     * @param statusChangesByIncident Mapa de cambios de estado agrupados por identificador de incidencia.
     * @param tasksByIncident         Mapa de asignaciones operativas agrupadas por identificador de incidencia.
     * @return Promedio de MTTR en horas redondeado a 1 decimal, o {@code 0.0} si no hay incidencias resueltas.
     */
    public Double calculateMttr(
            List<Incidencia> incidents,
            Map<Long, List<CambiosEstado>> statusChangesByIncident,
            Map<Long, List<Tareas>> tasksByIncident) {

        if (incidents == null || incidents.isEmpty()) {
            return 0.0;
        }

        double totalHours = 0.0;
        long resolvedCount = 0L;

        for (Incidencia incident : incidents) {
            if (incident == null || incident.getId() == null) {
                continue;
            }
            if (incident.getStatus() != EstadoIncidencia.RESOLVED && incident.getStatus() != EstadoIncidencia.CLOSED) {
                continue;
            }

            List<CambiosEstado> changes = (statusChangesByIncident != null)
                    ? statusChangesByIncident.getOrDefault(incident.getId(), Collections.emptyList())
                    : Collections.emptyList();
            List<Tareas> tasks = (tasksByIncident != null)
                    ? tasksByIncident.getOrDefault(incident.getId(), Collections.emptyList())
                    : Collections.emptyList();

            OffsetDateTime resolveTime = findResolutionTime(incident, changes);
            OffsetDateTime startTime = findResolutionStartTime(incident, changes, tasks);

            if (startTime != null && resolveTime != null) {
                double deltaSeconds = Duration.between(startTime, resolveTime).toSeconds();
                double deltaHours = Math.max(0.0, deltaSeconds / 3600.0);
                totalHours += deltaHours;
                resolvedCount++;
            }
        }

        return (resolvedCount > 0) ? roundOneDecimal(totalHours / resolvedCount) : 0.0;
    }

    /**
     * Determina el plazo máximo límite de resolución en horas según el nivel de prioridad asignado (RF-3).
     * <p>
     * Umbrales determinados:
     * <ul>
     *   <li>{@link PrioridadIncidencia#CRITICAL}: 72 horas (3 días naturales).</li>
     *   <li>{@link PrioridadIncidencia#HIGH}: 168 horas (7 días naturales).</li>
     *   <li>{@link PrioridadIncidencia#MEDIUM}: 360 horas (15 días naturales).</li>
     *   <li>{@link PrioridadIncidencia#LOW}: 720 horas (30 días naturales).</li>
     *   <li>Nulo: Por defecto aplica {@link PrioridadIncidencia#MEDIUM} (360 horas).</li>
     * </ul>
     * </p>
     *
     * @param priority Nivel de prioridad asignado a la incidencia, o {@code null}.
     * @return Umbral máximo permitido en horas para la resolución satisfactoria.
     */
    public double getSlaThresholdHours(PrioridadIncidencia priority) {
        if (priority == null) {
            return SLA_MEDIUM_HOURS;
        }
        return switch (priority) {
            case CRITICAL -> SLA_CRITICAL_HOURS;
            case HIGH -> SLA_HIGH_HOURS;
            case MEDIUM -> SLA_MEDIUM_HOURS;
            case LOW -> SLA_LOW_HOURS;
        };
    }

    /**
     * Evalúa deterministamente el cumplimiento de los compromisos de SLA para un conjunto de incidencias (RF-3, RF-4, RF-5).
     * <p>
     * - Incidencias resueltas ({@code RESOLVED} o {@code CLOSED}): tiempo total desde {@code createdAt} hasta la resolución.
     *   Si {@code tiempo <= umbral} cumple; si {@code tiempo > umbral} es incumplimiento (breached).
     * - Incidencias en curso ({@code REPORTED}, {@code VALIDATED}, {@code IN_PROGRESS}, {@code ASSIGNED}, {@code REOPENED}):
     *   tiempo transcurrido desde {@code createdAt} hasta la fecha actual ({@code now}).
     *   Si {@code tiempo > umbral} es incumplimiento en curso (breached); si {@code tiempo <= umbral} cumple plazo actual.
     * - Incidencias rechazadas ({@code REJECTED}): excluidas de la evaluación de SLA de resolución.
     * </p>
     *
     * @param incidents               Lista de incidencias a evaluar.
     * @param statusChangesByIncident Mapa de transiciones de estado por incidencia.
     * @param now                     Marca temporal actual de evaluación.
     * @return Estructura {@link SlaEvaluationResult} con totales y tasa porcentual calculada.
     */
    public SlaEvaluationResult evaluateSla(
            List<Incidencia> incidents,
            Map<Long, List<CambiosEstado>> statusChangesByIncident,
            OffsetDateTime now) {

        if (incidents == null || incidents.isEmpty()) {
            return new SlaEvaluationResult(0L, 0L, 0.0);
        }

        long compliant = 0L;
        long breached = 0L;

        for (Incidencia incident : incidents) {
            if (incident == null || incident.getCreatedAt() == null) {
                continue;
            }
            if (incident.getStatus() == EstadoIncidencia.REJECTED) {
                continue;
            }

            double threshold = getSlaThresholdHours(incident.getPriority());
            boolean isResolved = (incident.getStatus() == EstadoIncidencia.RESOLVED || incident.getStatus() == EstadoIncidencia.CLOSED);

            if (isResolved) {
                List<CambiosEstado> changes = (statusChangesByIncident != null && incident.getId() != null)
                        ? statusChangesByIncident.getOrDefault(incident.getId(), Collections.emptyList())
                        : Collections.emptyList();
                OffsetDateTime resolveTime = findResolutionTime(incident, changes);
                double hours = Math.max(0.0, Duration.between(incident.getCreatedAt(), resolveTime).toSeconds() / 3600.0);
                if (hours <= threshold) {
                    compliant++;
                } else {
                    breached++;
                }
            } else {
                double hours = Math.max(0.0, Duration.between(incident.getCreatedAt(), now).toSeconds() / 3600.0);
                if (hours <= threshold) {
                    compliant++;
                } else {
                    breached++;
                }
            }
        }

        long totalEvaluated = compliant + breached;
        double rate = (totalEvaluated > 0) ? roundOneDecimal((double) compliant * 100.0 / totalEvaluated) : 0.0;
        return new SlaEvaluationResult(compliant, breached, rate);
    }

    /**
     * Computa el desempeño resolutivo y volumen de trabajo para cada departamento municipal registrado (RF-6, RF-10).
     *
     * @param incidents               Lista global de incidencias filtradas.
     * @param departments             Catálogo de departamentos municipales.
     * @param tasksByIncident         Mapa de asignaciones indexadas por identificador de incidencia.
     * @param statusChangesByIncident Mapa de cambios de estado indexados por identificador de incidencia.
     * @param now                     Marca temporal actual para la evaluación de SLAs en curso.
     * @return Lista estructurada de {@link DepartmentMetricResponse} para cada departamento existente.
     */
    public List<DepartmentMetricResponse> calculateDepartmentMetrics(
            List<Incidencia> incidents,
            List<Departamento> departments,
            Map<Long, List<Tareas>> tasksByIncident,
            Map<Long, List<CambiosEstado>> statusChangesByIncident,
            OffsetDateTime now) {

        if (departments == null || departments.isEmpty()) {
            return Collections.emptyList();
        }

        List<DepartmentMetricResponse> result = new ArrayList<>();

        for (Departamento dept : departments) {
            if (dept == null || dept.getId() == null) {
                continue;
            }
            Long deptId = dept.getId();

            List<Incidencia> deptIncidents = (incidents != null)
                    ? incidents.stream()
                    .filter(Objects::nonNull)
                    .filter(i -> {
                        if (i.getId() == null || tasksByIncident == null) {
                            return false;
                        }
                        List<Tareas> tasks = tasksByIncident.get(i.getId());
                        if (tasks == null) {
                            return false;
                        }
                        return tasks.stream().anyMatch(t -> t.getDepartment() != null && deptId.equals(t.getDepartment().getId()));
                    })
                    .toList()
                    : Collections.emptyList();

            long totalAssigned = deptIncidents.size();
            List<Incidencia> resolvedDeptIncidents = deptIncidents.stream()
                    .filter(i -> i.getStatus() == EstadoIncidencia.RESOLVED || i.getStatus() == EstadoIncidencia.CLOSED)
                    .toList();
            long resolvedCount = resolvedDeptIncidents.size();

            double avgResolutionHours = 0.0;
            if (resolvedCount > 0) {
                double totalHours = 0.0;
                for (Incidencia inc : resolvedDeptIncidents) {
                    List<CambiosEstado> changes = (statusChangesByIncident != null && inc.getId() != null)
                            ? statusChangesByIncident.getOrDefault(inc.getId(), Collections.emptyList())
                            : Collections.emptyList();
                    List<Tareas> incTasks = (tasksByIncident != null && inc.getId() != null)
                            ? tasksByIncident.getOrDefault(inc.getId(), Collections.emptyList())
                            : Collections.emptyList();

                    OffsetDateTime resolveTime = findResolutionTime(inc, changes);
                    OffsetDateTime startTime = findResolutionStartTime(inc, changes, incTasks);

                    if (startTime != null && resolveTime != null) {
                        double delta = Math.max(0.0, Duration.between(startTime, resolveTime).toSeconds() / 3600.0);
                        totalHours += delta;
                    }
                }
                avgResolutionHours = roundOneDecimal(totalHours / resolvedCount);
            }

            SlaEvaluationResult deptSla = evaluateSla(deptIncidents, statusChangesByIncident, now);

            DepartmentMetricResponse metric = analyticsMapper.toDepartmentMetricResponse(
                    deptId,
                    dept.getName(),
                    totalAssigned,
                    resolvedCount,
                    avgResolutionHours,
                    deptSla.complianceRate()
            );
            result.add(metric);
        }

        return result;
    }

    /**
     * Localiza la marca temporal del primer evento de triaje formal (transición a {@code VALIDATED} o {@code REJECTED}).
     *
     * @param changes Lista ordenada cronológicamente de transiciones de estado de la incidencia.
     * @return Marca temporal del triaje, o {@code null} si la incidencia no ha sido aún triada.
     */
    private OffsetDateTime findTriageTime(List<CambiosEstado> changes) {
        if (changes == null || changes.isEmpty()) {
            return null;
        }
        for (CambiosEstado change : changes) {
            String toStatus = change.getToStatus();
            if ("VALIDATED".equalsIgnoreCase(toStatus) || "REJECTED".equalsIgnoreCase(toStatus)) {
                return change.getCreatedAt();
            }
        }
        return null;
    }

    /**
     * Localiza la marca temporal del primer evento formal de resolución (transición a {@code RESOLVED} o {@code CLOSED}).
     *
     * @param incident Incidencia auditada.
     * @param changes  Lista ordenada de transiciones de estado asociadas a la incidencia.
     * @return Marca temporal de resolución de la incidencia, nunca {@code null}.
     */
    private OffsetDateTime findResolutionTime(Incidencia incident, List<CambiosEstado> changes) {
        if (changes != null) {
            for (CambiosEstado change : changes) {
                if ("RESOLVED".equalsIgnoreCase(change.getToStatus())) {
                    return change.getCreatedAt();
                }
            }
            for (CambiosEstado change : changes) {
                if ("CLOSED".equalsIgnoreCase(change.getToStatus())) {
                    return change.getCreatedAt();
                }
            }
        }
        return (incident.getUpdatedAt() != null) ? incident.getUpdatedAt() : incident.getCreatedAt();
    }

    /**
     * Localiza la marca temporal de inicio de los trabajos de resolución (transición a {@code VALIDATED},
     * primera asignación departamental, o creación inicial).
     *
     * @param incident Incidencia auditada.
     * @param changes  Lista ordenada de transiciones de estado asociadas.
     * @param tasks    Lista de asignaciones asociadas a la incidencia.
     * @return Marca temporal de inicio de la fase operativa de resolución, nunca {@code null}.
     */
    private OffsetDateTime findResolutionStartTime(Incidencia incident, List<CambiosEstado> changes, List<Tareas> tasks) {
        if (changes != null) {
            for (CambiosEstado change : changes) {
                if ("VALIDATED".equalsIgnoreCase(change.getToStatus())) {
                    return change.getCreatedAt();
                }
            }
        }
        if (tasks != null && !tasks.isEmpty()) {
            OffsetDateTime earliestAssigned = tasks.stream()
                    .map(Tareas::getAssignedAt)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            if (earliestAssigned != null) {
                return earliestAssigned;
            }
        }
        return incident.getCreatedAt();
    }

    /**
     * Redondea un valor numérico de coma flotante a un único decimal de precisión.
     *
     * @param value Valor numérico de entrada.
     * @return Valor numérico redondeado a 1 decimal.
     */
    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}

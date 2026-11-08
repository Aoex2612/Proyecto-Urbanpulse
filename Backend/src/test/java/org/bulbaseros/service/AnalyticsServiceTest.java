package org.bulbaseros.service;

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
import org.bulbaseros.entity.Usuario;
import org.bulbaseros.mappers.AnalyticsMapper;
import org.bulbaseros.mappers.IncidentMapper;
import org.bulbaseros.repository.CambiosEstadoRepository;
import org.bulbaseros.repository.DepartamentoRepository;
import org.bulbaseros.repository.IncidenciaRepository;
import org.bulbaseros.repository.TareasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias exhaustivas para {@link AnalyticsService} con Mockito y JUnit 5.
 * <p>
 * Verifica el cálculo riguroso del Tiempo Medio de Triaje (MTTT, RF-1), Tiempo Medio de Resolución (MTTR, RF-2),
 * evaluación determinista de SLAs por prioridad en incidencias resueltas y en curso (RF-3, RF-4, RF-5),
 * desgloses agregados departamentales, territoriales y categóricos (RF-6, RF-7, RF-10),
 * resiliencia ante conjuntos sin incidencias (RF-11) y resolución de todos los rangos temporales predefinidos.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private IncidenciaRepository incidenciaRepository;

    @Mock
    private CambiosEstadoRepository cambiosEstadoRepository;

    @Mock
    private TareasRepository tareasRepository;

    @Mock
    private DepartamentoRepository departamentoRepository;

    @Mock
    private IntelligentRecommendationService intelligentRecommendationService;

    @Spy
    private AnalyticsMapper analyticsMapper = new AnalyticsMapper(new IncidentMapper());

    private Clock fixedClock;
    private OffsetDateTime fixedNow;
    private AnalyticsService analyticsService;

    private Usuario testCitizen;
    private Departamento deptVias;
    private Departamento deptAlumbrado;

    @BeforeEach
    void setUp() {
        // Fijamos un instante determinista: 2026-11-07T12:00:00Z
        fixedNow = OffsetDateTime.of(2026, 11, 7, 12, 0, 0, 0, ZoneOffset.UTC);
        fixedClock = Clock.fixed(fixedNow.toInstant(), ZoneOffset.UTC);

        analyticsService = new AnalyticsService(
                incidenciaRepository,
                cambiosEstadoRepository,
                tareasRepository,
                departamentoRepository,
                intelligentRecommendationService,
                analyticsMapper,
                fixedClock
        );

        testCitizen = new Usuario();
        testCitizen.setId(1L);
        testCitizen.setEmail("ciudadano@test.com");
        testCitizen.setFullName("Ciudadano Prueba");

        deptVias = new Departamento();
        deptVias.setId(10L);
        deptVias.setName("Vías Públicas");

        deptAlumbrado = new Departamento();
        deptAlumbrado.setId(20L);
        deptAlumbrado.setName("Alumbrado");
    }

    private Incidencia createIncident(
            Long id,
            String title,
            EstadoIncidencia status,
            PrioridadIncidencia priority,
            String district,
            String neighborhood,
            String category,
            OffsetDateTime createdAt) {
        Incidencia inc = new Incidencia();
        inc.setId(id);
        inc.setTitle(title);
        inc.setDescription("Descripción de prueba para " + title);
        inc.setStatus(status);
        inc.setPriority(priority);
        inc.setDistrict(district);
        inc.setNeighborhood(neighborhood);
        inc.setCategory(category);
        inc.setLatitude(36.7213);
        inc.setLongitude(-4.4214);
        inc.setCitizen(testCitizen);
        inc.setCreatedAt(createdAt);
        inc.setUpdatedAt(createdAt);
        return inc;
    }

    private CambiosEstado createStatusChange(Long id, Incidencia incident, String fromStatus, String toStatus, OffsetDateTime createdAt) {
        CambiosEstado change = new CambiosEstado();
        change.setId(id);
        change.setIncident(incident);
        change.setFromStatus(fromStatus);
        change.setToStatus(toStatus);
        change.setCreatedAt(createdAt);
        change.setChangedByUser(testCitizen);
        return change;
    }

    private Tareas createTask(Long id, Incidencia incident, Departamento department, OffsetDateTime assignedAt) {
        Tareas task = new Tareas();
        task.setId(id);
        task.setIncident(incident);
        task.setDepartment(department);
        task.setAssignedAt(assignedAt);
        task.setIsActive(true);
        task.setAssignedBy(testCitizen);
        return task;
    }

    @Nested
    @DisplayName("Cálculo de MTTT y MTTR (RF-1, RF-2)")
    class MtttAndMttrCalculations {

        @Test
        @DisplayName("calculateMttt: calcula promedio en horas de triaje con validación y rechazo")
        void calculateMttt_conCambiosValidacionYRechazo_calculaPromedioCorrectamente() {
            // Incidencia 1: validada a las 2 horas de su creación
            OffsetDateTime t0 = fixedNow.minusDays(5);
            Incidencia inc1 = createIncident(1L, "Bache", EstadoIncidencia.VALIDATED, PrioridadIncidencia.HIGH, "Centro", "Soho", "Vías", t0);
            CambiosEstado change1 = createStatusChange(101L, inc1, "REPORTED", "VALIDATED", t0.plusHours(2));

            // Incidencia 2: rechazada a las 4 horas de su creación
            OffsetDateTime t1 = fixedNow.minusDays(4);
            Incidencia inc2 = createIncident(2L, "Foco roto", EstadoIncidencia.REJECTED, PrioridadIncidencia.LOW, "Este", "El Palo", "Alumbrado", t1);
            CambiosEstado change2 = createStatusChange(102L, inc2, "REPORTED", "REJECTED", t1.plusHours(4));

            Map<Long, List<CambiosEstado>> changes = Map.of(
                    1L, List.of(change1),
                    2L, List.of(change2)
            );

            Double mttt = analyticsService.calculateMttt(List.of(inc1, inc2), changes);

            // (2.0 + 4.0) / 2 = 3.0 horas
            assertNotNull(mttt);
            assertEquals(3.0, mttt, 0.05);
        }

        @Test
        @DisplayName("calculateMttt: retorna 0.0 si ninguna incidencia ha sido triada")
        void calculateMttt_sinIncidenciasTriadas_retornaCero() {
            Incidencia inc1 = createIncident(1L, "Sin triar 1", EstadoIncidencia.REPORTED, PrioridadIncidencia.MEDIUM, "Centro", "Soho", "Vías", fixedNow.minusHours(5));
            Incidencia inc2 = createIncident(2L, "Sin triar 2", EstadoIncidencia.REPORTED, PrioridadIncidencia.MEDIUM, "Centro", "Soho", "Vías", fixedNow.minusHours(2));

            Double mttt = analyticsService.calculateMttt(List.of(inc1, inc2), Collections.emptyMap());

            assertNotNull(mttt);
            assertEquals(0.0, mttt);
        }

        @Test
        @DisplayName("calculateMttr: calcula promedio en horas de resolución para incidencias resueltas")
        void calculateMttr_conIncidenciasResueltasYValidadas_calculaPromedioCorrectamente() {
            // Incidencia 1: RESOLVED. VALIDATED a las 2h, RESOLVED a las 8h -> delta = 6h
            OffsetDateTime t0 = fixedNow.minusDays(10);
            Incidencia inc1 = createIncident(1L, "Bache", EstadoIncidencia.RESOLVED, PrioridadIncidencia.MEDIUM, "Centro", "Soho", "Vías", t0);
            CambiosEstado c1Val = createStatusChange(1L, inc1, "REPORTED", "VALIDATED", t0.plusHours(2));
            CambiosEstado c1Res = createStatusChange(2L, inc1, "VALIDATED", "RESOLVED", t0.plusHours(8));

            // Incidencia 2: CLOSED. VALIDATED a las 4h, RESOLVED a las 14h -> delta = 10h
            OffsetDateTime t1 = fixedNow.minusDays(8);
            Incidencia inc2 = createIncident(2L, "Farola", EstadoIncidencia.CLOSED, PrioridadIncidencia.HIGH, "Este", "El Palo", "Alumbrado", t1);
            CambiosEstado c2Val = createStatusChange(3L, inc2, "REPORTED", "VALIDATED", t1.plusHours(4));
            CambiosEstado c2Res = createStatusChange(4L, inc2, "VALIDATED", "RESOLVED", t1.plusHours(14));

            // Incidencia 3: En curso (IN_PROGRESS), no debe computar para MTTR
            Incidencia inc3 = createIncident(3L, "En curso", EstadoIncidencia.IN_PROGRESS, PrioridadIncidencia.LOW, "Centro", "Soho", "Vías", fixedNow.minusHours(20));

            Map<Long, List<CambiosEstado>> changes = Map.of(
                    1L, List.of(c1Val, c1Res),
                    2L, List.of(c2Val, c2Res)
            );

            Double mttr = analyticsService.calculateMttr(List.of(inc1, inc2, inc3), changes, Collections.emptyMap());

            // (6.0 + 10.0) / 2 = 8.0 horas
            assertNotNull(mttr);
            assertEquals(8.0, mttr, 0.05);
        }

        @Test
        @DisplayName("calculateMttr: usa primera asignación o fecha de creación si no hubo cambio a VALIDATED")
        void calculateMttr_fallbackAAsignacionOCreacionCuandoNoHayCambioValidacion() {
            // Incidencia 1: Sin cambio VALIDATED, pero asignada a las 3h, resuelta a las 13h -> delta = 10h
            OffsetDateTime t0 = fixedNow.minusDays(3);
            Incidencia inc1 = createIncident(1L, "Sin cambio val", EstadoIncidencia.RESOLVED, PrioridadIncidencia.MEDIUM, "Centro", "Soho", "Vías", t0);
            CambiosEstado c1Res = createStatusChange(1L, inc1, "REPORTED", "RESOLVED", t0.plusHours(13));
            Tareas task1 = createTask(1L, inc1, deptVias, t0.plusHours(3));

            // Incidencia 2: Sin cambio VALIDATED ni tareas, creada en t1 y resuelta a las 6h -> delta = 6h
            OffsetDateTime t1 = fixedNow.minusDays(2);
            Incidencia inc2 = createIncident(2L, "Sin nada", EstadoIncidencia.RESOLVED, PrioridadIncidencia.LOW, "Centro", "Soho", "Vías", t1);
            CambiosEstado c2Res = createStatusChange(2L, inc2, "REPORTED", "RESOLVED", t1.plusHours(6));

            Map<Long, List<CambiosEstado>> changes = Map.of(
                    1L, List.of(c1Res),
                    2L, List.of(c2Res)
            );
            Map<Long, List<Tareas>> tasks = Map.of(
                    1L, List.of(task1)
            );

            Double mttr = analyticsService.calculateMttr(List.of(inc1, inc2), changes, tasks);

            // (10.0 + 6.0) / 2 = 8.0 horas
            assertNotNull(mttr);
            assertEquals(8.0, mttr, 0.05);
        }

        @Test
        @DisplayName("calculateMttr: retorna 0.0 si no existen incidencias resueltas o cerradas")
        void calculateMttr_sinIncidenciasResueltas_retornaCero() {
            Incidencia inc1 = createIncident(1L, "Reportada", EstadoIncidencia.REPORTED, PrioridadIncidencia.MEDIUM, "Centro", "Soho", "Vías", fixedNow.minusHours(5));
            Incidencia inc2 = createIncident(2L, "En progreso", EstadoIncidencia.IN_PROGRESS, PrioridadIncidencia.HIGH, "Centro", "Soho", "Vías", fixedNow.minusHours(10));

            Double mttr = analyticsService.calculateMttr(List.of(inc1, inc2), Collections.emptyMap(), Collections.emptyMap());

            assertNotNull(mttr);
            assertEquals(0.0, mttr);
        }
    }

    @Nested
    @DisplayName("Evaluación de SLAs por Prioridad (RF-3, RF-4, RF-5)")
    class SlaEvaluations {

        @Test
        @DisplayName("evaluateSla: todas las prioridades en plazo alcanzan 100% de cumplimiento")
        void evaluateSla_todasLasPrioridadesEnTiempo_cumplimientoCienPorCien() {
            // CRITICAL: 72h max. Resuelta en 24h
            OffsetDateTime tCrit = fixedNow.minusHours(30);
            Incidencia incCrit = createIncident(1L, "Crítica", EstadoIncidencia.RESOLVED, PrioridadIncidencia.CRITICAL, "Centro", "Soho", "Vías", tCrit);
            CambiosEstado chCrit = createStatusChange(1L, incCrit, "REPORTED", "RESOLVED", tCrit.plusHours(24));

            // HIGH: 168h max. Resuelta en 100h
            OffsetDateTime tHigh = fixedNow.minusHours(120);
            Incidencia incHigh = createIncident(2L, "Alta", EstadoIncidencia.RESOLVED, PrioridadIncidencia.HIGH, "Centro", "Soho", "Vías", tHigh);
            CambiosEstado chHigh = createStatusChange(2L, incHigh, "REPORTED", "RESOLVED", tHigh.plusHours(100));

            // MEDIUM: 360h max. Resuelta en 200h
            OffsetDateTime tMed = fixedNow.minusHours(250);
            Incidencia incMed = createIncident(3L, "Media", EstadoIncidencia.RESOLVED, PrioridadIncidencia.MEDIUM, "Centro", "Soho", "Vías", tMed);
            CambiosEstado chMed = createStatusChange(3L, incMed, "REPORTED", "RESOLVED", tMed.plusHours(200));

            // LOW: 720h max. Resuelta en 500h
            OffsetDateTime tLow = fixedNow.minusHours(600);
            Incidencia incLow = createIncident(4L, "Baja", EstadoIncidencia.RESOLVED, PrioridadIncidencia.LOW, "Centro", "Soho", "Vías", tLow);
            CambiosEstado chLow = createStatusChange(4L, incLow, "REPORTED", "RESOLVED", tLow.plusHours(500));

            Map<Long, List<CambiosEstado>> changes = Map.of(
                    1L, List.of(chCrit),
                    2L, List.of(chHigh),
                    3L, List.of(chMed),
                    4L, List.of(chLow)
            );

            AnalyticsService.SlaEvaluationResult result = analyticsService.evaluateSla(
                    List.of(incCrit, incHigh, incMed, incLow),
                    changes,
                    fixedNow
            );

            assertEquals(4L, result.totalCompliant());
            assertEquals(0L, result.totalBreached());
            assertEquals(100.0, result.complianceRate());
        }

        @Test
        @DisplayName("evaluateSla: todas las prioridades fuera de plazo resultan en 0% de cumplimiento")
        void evaluateSla_todasLasPrioridadesFueraDePlazo_cumplimientoCero() {
            // CRITICAL: 72h max. Resuelta en 80h (> 72h) -> Breached
            OffsetDateTime tCrit = fixedNow.minusHours(100);
            Incidencia incCrit = createIncident(1L, "Crítica", EstadoIncidencia.RESOLVED, PrioridadIncidencia.CRITICAL, "Centro", "Soho", "Vías", tCrit);
            CambiosEstado chCrit = createStatusChange(1L, incCrit, "REPORTED", "RESOLVED", tCrit.plusHours(80));

            // HIGH: 168h max. Resuelta en 170h (> 168h) -> Breached
            OffsetDateTime tHigh = fixedNow.minusHours(200);
            Incidencia incHigh = createIncident(2L, "Alta", EstadoIncidencia.RESOLVED, PrioridadIncidencia.HIGH, "Centro", "Soho", "Vías", tHigh);
            CambiosEstado chHigh = createStatusChange(2L, incHigh, "REPORTED", "RESOLVED", tHigh.plusHours(170));

            // MEDIUM: 360h max. Resuelta en 365h (> 360h) -> Breached
            OffsetDateTime tMed = fixedNow.minusHours(400);
            Incidencia incMed = createIncident(3L, "Media", EstadoIncidencia.RESOLVED, PrioridadIncidencia.MEDIUM, "Centro", "Soho", "Vías", tMed);
            CambiosEstado chMed = createStatusChange(3L, incMed, "REPORTED", "RESOLVED", tMed.plusHours(365));

            // LOW: 720h max. Resuelta en 750h (> 720h) -> Breached
            OffsetDateTime tLow = fixedNow.minusHours(800);
            Incidencia incLow = createIncident(4L, "Baja", EstadoIncidencia.RESOLVED, PrioridadIncidencia.LOW, "Centro", "Soho", "Vías", tLow);
            CambiosEstado chLow = createStatusChange(4L, incLow, "REPORTED", "RESOLVED", tLow.plusHours(750));

            Map<Long, List<CambiosEstado>> changes = Map.of(
                    1L, List.of(chCrit),
                    2L, List.of(chHigh),
                    3L, List.of(chMed),
                    4L, List.of(chLow)
            );

            AnalyticsService.SlaEvaluationResult result = analyticsService.evaluateSla(
                    List.of(incCrit, incHigh, incMed, incLow),
                    changes,
                    fixedNow
            );

            assertEquals(0L, result.totalCompliant());
            assertEquals(4L, result.totalBreached());
            assertEquals(0.0, result.complianceRate());
        }

        @Test
        @DisplayName("evaluateSla: incidencias en curso detectan incumplimiento frente a la fecha actual")
        void evaluateSla_incidenciasEnCurso_detectaBreachedYCompliantSegunNow() {
            // Incidencia 1: CRITICAL (72h max). En curso (IN_PROGRESS), creada hace 80h -> Breached en curso
            OffsetDateTime t1 = fixedNow.minusHours(80);
            Incidencia inc1 = createIncident(1L, "En curso vencida", EstadoIncidencia.IN_PROGRESS, PrioridadIncidencia.CRITICAL, "Centro", "Soho", "Vías", t1);

            // Incidencia 2: CRITICAL (72h max). En curso (VALIDATED), creada hace 20h -> Compliant en curso
            OffsetDateTime t2 = fixedNow.minusHours(20);
            Incidencia inc2 = createIncident(2L, "En curso a tiempo", EstadoIncidencia.VALIDATED, PrioridadIncidencia.CRITICAL, "Centro", "Soho", "Vías", t2);

            AnalyticsService.SlaEvaluationResult result = analyticsService.evaluateSla(
                    List.of(inc1, inc2),
                    Collections.emptyMap(),
                    fixedNow
            );

            assertEquals(1L, result.totalCompliant());
            assertEquals(1L, result.totalBreached());
            assertEquals(50.0, result.complianceRate());
        }

        @Test
        @DisplayName("evaluateSla: incidencia con prioridad nula aplica fallback a MEDIUM (360h)")
        void evaluateSla_prioridadNula_aplicaFallbackMedium360Horas() {
            // Resuelta en 100 horas con prioridad nula -> <= 360h -> Compliant
            OffsetDateTime t0 = fixedNow.minusHours(150);
            Incidencia inc = createIncident(1L, "Prioridad nula", EstadoIncidencia.RESOLVED, null, "Centro", "Soho", "Vías", t0);
            CambiosEstado ch = createStatusChange(1L, inc, "REPORTED", "RESOLVED", t0.plusHours(100));

            AnalyticsService.SlaEvaluationResult result = analyticsService.evaluateSla(
                    List.of(inc),
                    Map.of(1L, List.of(ch)),
                    fixedNow
            );

            assertEquals(1L, result.totalCompliant());
            assertEquals(0L, result.totalBreached());
            assertEquals(100.0, result.complianceRate());
        }

        @Test
        @DisplayName("evaluateSla: incidencias rechazadas quedan excluidas del cómputo de SLA")
        void evaluateSla_incidenciaRechazada_excluidaDeEvaluacion() {
            OffsetDateTime t0 = fixedNow.minusHours(100);
            Incidencia inc = createIncident(1L, "Rechazada", EstadoIncidencia.REJECTED, PrioridadIncidencia.CRITICAL, "Centro", "Soho", "Vías", t0);

            AnalyticsService.SlaEvaluationResult result = analyticsService.evaluateSla(
                    List.of(inc),
                    Collections.emptyMap(),
                    fixedNow
            );

            assertEquals(0L, result.totalCompliant());
            assertEquals(0L, result.totalBreached());
            assertEquals(0.0, result.complianceRate());
        }
    }

    @Nested
    @DisplayName("Resiliencia y casos de conjunto vacío (RF-11)")
    class EmptyAndResilienceCases {

        @Test
        @DisplayName("getAnalyticsSummary: ante cero incidencias registradas retorna métricas en 0 sin errores")
        void getAnalyticsSummary_listaVaciaDeIncidencias_retornaMetricasEnCeroSinDivisionPorCero() {
            when(incidenciaRepository.findByDateRangeAndTerritory(any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            AnalyticsFilterRequest request = AnalyticsFilterRequest.builder()
                    .preset(AnalyticsTimePreset.LAST_30_DAYS)
                    .build();

            AnalyticsSummaryResponse response = analyticsService.getAnalyticsSummary(request);

            assertNotNull(response);
            OperationalMetricsResponse op = response.getOperationalMetrics();
            assertNotNull(op);
            assertEquals(0L, op.getTotalIncidents());
            assertEquals(0L, op.getResolvedIncidents());
            assertEquals(0L, op.getPendingIncidents());
            assertEquals(0.0, op.getMtttHours());
            assertEquals(0.0, op.getMttrHours());
            assertEquals(0.0, op.getSlaComplianceRate());
            assertEquals(0L, op.getTotalSlaBreached());
            assertEquals(0L, op.getTotalSlaCompliant());

            assertTrue(response.getTerritorialMetrics().isEmpty());
            assertTrue(response.getDepartmentMetrics().isEmpty());
            assertTrue(response.getCategoryMetrics().isEmpty());
            assertTrue(response.getFilteredIncidents().isEmpty());
            assertNotNull(response.getAssistantPerformance());
            assertEquals(0L, response.getAssistantPerformance().getTotalRecommendations());

            // No debe haber consultas secundarias si no hay incidencias
            verify(cambiosEstadoRepository, never()).findByIncidentIdInOrderByCreatedAtAsc(anyCollection());
            verify(tareasRepository, never()).findByIncidentIdIn(anyCollection());
        }

        @Test
        @DisplayName("getAnalyticsSummary: si el repositorio retorna null, maneja como lista vacía")
        void getAnalyticsSummary_listaNulaDeIncidencias_retornaMetricasEnCero() {
            when(incidenciaRepository.findByDateRangeAndTerritory(any(), any(), any(), any()))
                    .thenReturn(null);

            AnalyticsSummaryResponse response = analyticsService.getAnalyticsSummary(null);

            assertNotNull(response);
            assertEquals(0L, response.getOperationalMetrics().getTotalIncidents());
        }
    }

    @Nested
    @DisplayName("Resolución de Rangos Temporales (RF-8, RF-9)")
    class DatePresetResolutions {

        @Test
        @DisplayName("resolveDateRange: preset TODAY abarca desde las 00:00:00 UTC hasta fin del día")
        void resolveDateRange_presetToday() {
            AnalyticsFilterRequest req = AnalyticsFilterRequest.builder()
                    .preset(AnalyticsTimePreset.TODAY)
                    .build();

            AnalyticsService.DateRange range = analyticsService.resolveDateRange(req);

            LocalDate today = fixedNow.toLocalDate();
            assertEquals(today.atStartOfDay().atOffset(ZoneOffset.UTC), range.startDate());
            assertEquals(today.atTime(23, 59, 59, 999999999).atOffset(ZoneOffset.UTC), range.endDate());
        }

        @Test
        @DisplayName("resolveDateRange: preset LAST_7_DAYS abarca 7 días previos exactos")
        void resolveDateRange_presetLast7Days() {
            AnalyticsFilterRequest req = AnalyticsFilterRequest.builder()
                    .preset(AnalyticsTimePreset.LAST_7_DAYS)
                    .build();

            AnalyticsService.DateRange range = analyticsService.resolveDateRange(req);

            assertEquals(fixedNow.minusDays(7), range.startDate());
            assertEquals(fixedNow, range.endDate());
        }

        @Test
        @DisplayName("resolveDateRange: preset LAST_30_DAYS abarca 30 días previos exactos")
        void resolveDateRange_presetLast30Days() {
            AnalyticsFilterRequest req = AnalyticsFilterRequest.builder()
                    .preset(AnalyticsTimePreset.LAST_30_DAYS)
                    .build();

            AnalyticsService.DateRange range = analyticsService.resolveDateRange(req);

            assertEquals(fixedNow.minusDays(30), range.startDate());
            assertEquals(fixedNow, range.endDate());
        }

        @Test
        @DisplayName("resolveDateRange: preset LAST_3_MONTHS abarca 3 meses previos")
        void resolveDateRange_presetLast3Months() {
            AnalyticsFilterRequest req = AnalyticsFilterRequest.builder()
                    .preset(AnalyticsTimePreset.LAST_3_MONTHS)
                    .build();

            AnalyticsService.DateRange range = analyticsService.resolveDateRange(req);

            assertEquals(fixedNow.minusMonths(3), range.startDate());
            assertEquals(fixedNow, range.endDate());
        }

        @Test
        @DisplayName("resolveDateRange: preset LAST_6_MONTHS abarca 6 meses previos")
        void resolveDateRange_presetLast6Months() {
            AnalyticsFilterRequest req = AnalyticsFilterRequest.builder()
                    .preset(AnalyticsTimePreset.LAST_6_MONTHS)
                    .build();

            AnalyticsService.DateRange range = analyticsService.resolveDateRange(req);

            assertEquals(fixedNow.minusMonths(6), range.startDate());
            assertEquals(fixedNow, range.endDate());
        }

        @Test
        @DisplayName("resolveDateRange: preset LAST_YEAR abarca 1 año previo")
        void resolveDateRange_presetLastYear() {
            AnalyticsFilterRequest req = AnalyticsFilterRequest.builder()
                    .preset(AnalyticsTimePreset.LAST_YEAR)
                    .build();

            AnalyticsService.DateRange range = analyticsService.resolveDateRange(req);

            assertEquals(fixedNow.minusYears(1), range.startDate());
            assertEquals(fixedNow, range.endDate());
        }

        @Test
        @DisplayName("resolveDateRange: preset SPECIFIC_YEAR con año específico 2024 abarca año natural completo")
        void resolveDateRange_presetSpecificYear_conAnioIndicado() {
            AnalyticsFilterRequest req = AnalyticsFilterRequest.builder()
                    .preset(AnalyticsTimePreset.SPECIFIC_YEAR)
                    .year(2024)
                    .build();

            AnalyticsService.DateRange range = analyticsService.resolveDateRange(req);

            OffsetDateTime expectedStart = LocalDate.of(2024, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime expectedEnd = LocalDate.of(2024, 12, 31).atTime(23, 59, 59, 999999999).atOffset(ZoneOffset.UTC);

            assertEquals(expectedStart, range.startDate());
            assertEquals(expectedEnd, range.endDate());
        }

        @Test
        @DisplayName("resolveDateRange: preset SPECIFIC_YEAR sin año especificado toma el año de fixedNow")
        void resolveDateRange_presetSpecificYear_sinAnio_tomaAnioActual() {
            AnalyticsFilterRequest req = AnalyticsFilterRequest.builder()
                    .preset(AnalyticsTimePreset.SPECIFIC_YEAR)
                    .build();

            AnalyticsService.DateRange range = analyticsService.resolveDateRange(req);

            int currentYear = fixedNow.getYear();
            OffsetDateTime expectedStart = LocalDate.of(currentYear, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime expectedEnd = LocalDate.of(currentYear, 12, 31).atTime(23, 59, 59, 999999999).atOffset(ZoneOffset.UTC);

            assertEquals(expectedStart, range.startDate());
            assertEquals(expectedEnd, range.endDate());
        }

        @Test
        @DisplayName("resolveDateRange: fechas manuales personalizadas tienen precedencia sobre cualquier preset")
        void resolveDateRange_fechasManualesPersonalizadas() {
            OffsetDateTime customStart = fixedNow.minusDays(15);
            OffsetDateTime customEnd = fixedNow.minusDays(2);

            AnalyticsFilterRequest req = AnalyticsFilterRequest.builder()
                    .preset(AnalyticsTimePreset.LAST_YEAR) // Debería ser ignorado
                    .startDate(customStart)
                    .endDate(customEnd)
                    .build();

            AnalyticsService.DateRange range = analyticsService.resolveDateRange(req);

            assertEquals(customStart, range.startDate());
            assertEquals(customEnd, range.endDate());
        }

        @Test
        @DisplayName("resolveDateRange: sin preset ni fechas aplica por defecto LAST_30_DAYS")
        void resolveDateRange_sinParametros_aplicaPorDefectoLast30Days() {
            AnalyticsService.DateRange range = analyticsService.resolveDateRange(null);

            assertEquals(fixedNow.minusDays(30), range.startDate());
            assertEquals(fixedNow, range.endDate());
        }
    }

    @Nested
    @DisplayName("Desglose Departamental (RF-6, RF-10)")
    class DepartmentalMetrics {

        @Test
        @DisplayName("calculateDepartmentMetrics: computa departamentos con incidencias y sin incidencias asignadas")
        void calculateDepartmentMetrics_departamentosConYSinIncidenciasAsignadas() {
            OffsetDateTime t0 = fixedNow.minusDays(3);
            Incidencia inc1 = createIncident(1L, "Bache", EstadoIncidencia.RESOLVED, PrioridadIncidencia.HIGH, "Centro", "Soho", "Vías", t0);
            CambiosEstado c1Val = createStatusChange(1L, inc1, "REPORTED", "VALIDATED", t0.plusHours(2));
            CambiosEstado c1Res = createStatusChange(2L, inc1, "VALIDATED", "RESOLVED", t0.plusHours(10));
            Tareas task1 = createTask(1L, inc1, deptVias, t0.plusHours(2));

            Incidencia inc2 = createIncident(2L, "Acera rota", EstadoIncidencia.IN_PROGRESS, PrioridadIncidencia.HIGH, "Centro", "Soho", "Vías", t0.plusHours(5));
            Tareas task2 = createTask(2L, inc2, deptVias, t0.plusHours(6));

            Map<Long, List<Tareas>> tasks = Map.of(
                    1L, List.of(task1),
                    2L, List.of(task2)
            );
            Map<Long, List<CambiosEstado>> changes = Map.of(
                    1L, List.of(c1Val, c1Res)
            );

            List<Departamento> depts = List.of(deptAlumbrado, deptVias);

            List<DepartmentMetricResponse> metrics = analyticsService.calculateDepartmentMetrics(
                    List.of(inc1, inc2),
                    depts,
                    tasks,
                    changes,
                    fixedNow
            );

            assertEquals(2, metrics.size());

            // Dept Alumbrado: 0 asignadas
            DepartmentMetricResponse alumbradoMetric = metrics.stream()
                    .filter(m -> m.getDepartmentId().equals(20L))
                    .findFirst()
                    .orElseThrow();
            assertEquals(0L, alumbradoMetric.getTotalAssigned());
            assertEquals(0L, alumbradoMetric.getResolvedCount());
            assertEquals(0.0, alumbradoMetric.getAvgResolutionHours());
            assertEquals(0.0, alumbradoMetric.getSlaComplianceRate());

            // Dept Vías: 2 asignadas, 1 resuelta en 8h (VALIDATED t0+2h a RESOLVED t0+10h), 1 en curso
            DepartmentMetricResponse viasMetric = metrics.stream()
                    .filter(m -> m.getDepartmentId().equals(10L))
                    .findFirst()
                    .orElseThrow();
            assertEquals(2L, viasMetric.getTotalAssigned());
            assertEquals(1L, viasMetric.getResolvedCount());
            assertEquals(8.0, viasMetric.getAvgResolutionHours(), 0.05);
            // Incidencia 1: resuelta en 10h (<= 168h HIGH) -> Compliant
            // Incidencia 2: en curso hace ~67h (<= 168h HIGH) -> Compliant
            assertEquals(100.0, viasMetric.getSlaComplianceRate(), 0.05);
        }
    }

    @Nested
    @DisplayName("Desglose Territorial y por Categoría (RF-6, RF-7, RF-10)")
    class TerritorialAndCategoryMetrics {

        @Test
        @DisplayName("getAnalyticsSummary: agrupa por distrito si no se indica distrito específico")
        void getAnalyticsSummary_agrupacionPorDistrito_cuandoNoSeEspecificaDistrito() {
            OffsetDateTime t0 = fixedNow.minusDays(2);
            Incidencia inc1 = createIncident(1L, "Inc 1", EstadoIncidencia.RESOLVED, PrioridadIncidencia.MEDIUM, "Centro", "Soho", "Alumbrado", t0);
            Incidencia inc2 = createIncident(2L, "Inc 2", EstadoIncidencia.REPORTED, PrioridadIncidencia.MEDIUM, "Este", "El Palo", "Vías", t0);

            when(incidenciaRepository.findByDateRangeAndTerritory(any(), any(), eq(null), eq(null)))
                    .thenReturn(List.of(inc1, inc2));
            when(cambiosEstadoRepository.findByIncidentIdInOrderByCreatedAtAsc(anyCollection()))
                    .thenReturn(Collections.emptyList());
            when(tareasRepository.findByIncidentIdIn(anyCollection()))
                    .thenReturn(Collections.emptyList());
            when(departamentoRepository.findAllByOrderByNameAsc())
                    .thenReturn(Collections.emptyList());
            when(intelligentRecommendationService.evaluatePerformance(anyList()))
                    .thenReturn(AssistantPerformanceResponse.builder().build());

            AnalyticsFilterRequest req = AnalyticsFilterRequest.builder()
                    .preset(AnalyticsTimePreset.LAST_7_DAYS)
                    .build();

            AnalyticsSummaryResponse response = analyticsService.getAnalyticsSummary(req);

            assertNotNull(response);
            List<TerritorialMetricResponse> territorial = response.getTerritorialMetrics();
            assertEquals(2, territorial.size());
            assertTrue(territorial.stream().anyMatch(t -> "Centro".equals(t.getTerritoryName())));
            assertTrue(territorial.stream().anyMatch(t -> "Este".equals(t.getTerritoryName())));
        }

        @Test
        @DisplayName("getAnalyticsSummary: agrupa por barrio cuando se especifica distrito sin barrio")
        void getAnalyticsSummary_agrupacionPorBarrio_cuandoSeEspecificaDistritoSinBarrio() {
            OffsetDateTime t0 = fixedNow.minusDays(2);
            Incidencia inc1 = createIncident(1L, "Inc 1", EstadoIncidencia.RESOLVED, PrioridadIncidencia.MEDIUM, "Centro", "Soho", "Alumbrado", t0);
            Incidencia inc2 = createIncident(2L, "Inc 2", EstadoIncidencia.REPORTED, PrioridadIncidencia.MEDIUM, "Centro", "La Merced", "Vías", t0);

            when(incidenciaRepository.findByDateRangeAndTerritory(any(), any(), eq("Centro"), eq(null)))
                    .thenReturn(List.of(inc1, inc2));
            when(cambiosEstadoRepository.findByIncidentIdInOrderByCreatedAtAsc(anyCollection()))
                    .thenReturn(Collections.emptyList());
            when(tareasRepository.findByIncidentIdIn(anyCollection()))
                    .thenReturn(Collections.emptyList());
            when(departamentoRepository.findAllByOrderByNameAsc())
                    .thenReturn(Collections.emptyList());
            when(intelligentRecommendationService.evaluatePerformance(anyList()))
                    .thenReturn(AssistantPerformanceResponse.builder().build());

            AnalyticsFilterRequest req = AnalyticsFilterRequest.builder()
                    .preset(AnalyticsTimePreset.LAST_7_DAYS)
                    .district("Centro")
                    .build();

            AnalyticsSummaryResponse response = analyticsService.getAnalyticsSummary(req);

            assertNotNull(response);
            List<TerritorialMetricResponse> territorial = response.getTerritorialMetrics();
            assertEquals(2, territorial.size());
            assertTrue(territorial.stream().anyMatch(t -> "Soho".equals(t.getTerritoryName())));
            assertTrue(territorial.stream().anyMatch(t -> "La Merced".equals(t.getTerritoryName())));
        }

        @Test
        @DisplayName("getAnalyticsSummary: calcula agregación por categoría temática")
        void getAnalyticsSummary_agrupacionPorCategoria_calculaTotalesCorrectamente() {
            OffsetDateTime t0 = fixedNow.minusDays(2);
            Incidencia inc1 = createIncident(1L, "Inc 1", EstadoIncidencia.RESOLVED, PrioridadIncidencia.MEDIUM, "Centro", "Soho", "Alumbrado", t0);
            Incidencia inc2 = createIncident(2L, "Inc 2", EstadoIncidencia.REPORTED, PrioridadIncidencia.MEDIUM, "Centro", "Soho", "Alumbrado", t0);
            Incidencia inc3 = createIncident(3L, "Inc 3", EstadoIncidencia.RESOLVED, PrioridadIncidencia.MEDIUM, "Centro", "Soho", "Vías", t0);

            when(incidenciaRepository.findByDateRangeAndTerritory(any(), any(), any(), any()))
                    .thenReturn(List.of(inc1, inc2, inc3));
            when(cambiosEstadoRepository.findByIncidentIdInOrderByCreatedAtAsc(anyCollection()))
                    .thenReturn(Collections.emptyList());
            when(tareasRepository.findByIncidentIdIn(anyCollection()))
                    .thenReturn(Collections.emptyList());
            when(departamentoRepository.findAllByOrderByNameAsc())
                    .thenReturn(Collections.emptyList());
            when(intelligentRecommendationService.evaluatePerformance(anyList()))
                    .thenReturn(AssistantPerformanceResponse.builder().build());

            AnalyticsSummaryResponse response = analyticsService.getAnalyticsSummary(null);

            assertNotNull(response);
            List<CategoryMetricResponse> catMetrics = response.getCategoryMetrics();
            assertEquals(2, catMetrics.size());

            CategoryMetricResponse alumbradoCat = catMetrics.stream()
                    .filter(c -> "Alumbrado".equals(c.getCategory()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(2L, alumbradoCat.getTotalIncidents());
            assertEquals(1L, alumbradoCat.getResolvedCount());
        }
    }

    @Nested
    @DisplayName("Integración con Asistencia Inteligente (RF-18)")
    class AssistantPerformanceIntegration {

        @Test
        @DisplayName("getAnalyticsSummary: delega en IntelligentRecommendationService y consolida métricas")
        void getAnalyticsSummary_integraDesempenoDeAsistenteInteligente() {
            OffsetDateTime t0 = fixedNow.minusDays(1);
            Incidencia inc = createIncident(1L, "Incidencia 1", EstadoIncidencia.REPORTED, PrioridadIncidencia.LOW, "Centro", "Soho", "Limpieza", t0);

            when(incidenciaRepository.findByDateRangeAndTerritory(any(), any(), any(), any()))
                    .thenReturn(List.of(inc));
            when(cambiosEstadoRepository.findByIncidentIdInOrderByCreatedAtAsc(anyCollection()))
                    .thenReturn(Collections.emptyList());
            when(tareasRepository.findByIncidentIdIn(anyCollection()))
                    .thenReturn(Collections.emptyList());
            when(departamentoRepository.findAllByOrderByNameAsc())
                    .thenReturn(Collections.emptyList());

            AssistantPerformanceResponse mockPerf = AssistantPerformanceResponse.builder()
                    .totalRecommendations(10L)
                    .acceptedRecommendations(8L)
                    .adoptionRate(80.0)
                    .averageConfidence(88.5)
                    .build();
            when(intelligentRecommendationService.evaluatePerformance(anyList()))
                    .thenReturn(mockPerf);

            AnalyticsSummaryResponse response = analyticsService.getAnalyticsSummary(null);

            assertNotNull(response);
            assertEquals(mockPerf, response.getAssistantPerformance());
            assertEquals(10L, response.getAssistantPerformance().getTotalRecommendations());
            assertEquals(80.0, response.getAssistantPerformance().getAdoptionRate());
            assertEquals(88.5, response.getAssistantPerformance().getAverageConfidence());
        }
    }
}

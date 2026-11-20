package org.bulbaseros.mappers;

import org.bulbaseros.dto.AnalyticsFilterRequest;
import org.bulbaseros.dto.AnalyticsSummaryResponse;
import org.bulbaseros.dto.AnalyticsTimePreset;
import org.bulbaseros.dto.AssistantPerformanceResponse;
import org.bulbaseros.dto.CategoryMetricResponse;
import org.bulbaseros.dto.DepartmentMetricResponse;
import org.bulbaseros.dto.IncidentResponse;
import org.bulbaseros.dto.OperationalMetricsResponse;
import org.bulbaseros.dto.TerritorialMetricResponse;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link AnalyticsMapper}.
 * <p>
 * Verifica transformaciones unitarias, colecciones, casos nulos, conjuntos vacíos,
 * agregaciones territoriales (distrito/barrio), categóricas y operacionales de nivel de servicio.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class AnalyticsMapperTest {

    private AnalyticsMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AnalyticsMapper();
    }

    private Incidencia createSampleIncident(
            Long id,
            String title,
            String category,
            String district,
            String neighborhood,
            EstadoIncidencia status,
            PrioridadIncidencia priority) {

        Incidencia inc = new Incidencia();
        inc.setId(id);
        inc.setTitle(title);
        inc.setDescription("Descripción de prueba para " + title);
        inc.setCategory(category);
        inc.setDistrict(district);
        inc.setNeighborhood(neighborhood);
        inc.setStatus(status);
        inc.setPriority(priority);
        inc.setCreatedAt(OffsetDateTime.now().minusDays(2));
        inc.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        return inc;
    }

    // ==========================================
    // 1. toDto(Incidencia) - Mapeo Individual
    // ==========================================

    @Test
    @DisplayName("toDto: Debe retornar null cuando la entidad de entrada es nula")
    void shouldReturnNullWhenEntityIsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    @DisplayName("toDto: Debe transformar una entidad Incidencia a un AnalyticsSummaryResponse unitario")
    void shouldMapSingleIncidentToSummaryResponse() {
        Incidencia inc = createSampleIncident(
                1L, "Farola fundida", "Alumbrado", "Centro", "Soho",
                EstadoIncidencia.RESOLVED, PrioridadIncidencia.MEDIUM);

        AnalyticsSummaryResponse summary = mapper.toDto(inc);

        assertNotNull(summary);
        assertNotNull(summary.getOperationalMetrics());
        assertEquals(1L, summary.getOperationalMetrics().getTotalIncidents());
        assertEquals(1L, summary.getOperationalMetrics().getResolvedIncidents());
        assertEquals(0L, summary.getOperationalMetrics().getPendingIncidents());

        assertEquals(1, summary.getTerritorialMetrics().size());
        assertEquals("Centro", summary.getTerritorialMetrics().get(0).getTerritoryName());
        assertEquals(1L, summary.getTerritorialMetrics().get(0).getTotalIncidents());

        assertEquals(1, summary.getCategoryMetrics().size());
        assertEquals("Alumbrado", summary.getCategoryMetrics().get(0).getCategory());

        assertEquals(1, summary.getFilteredIncidents().size());
        assertEquals(1L, summary.getFilteredIncidents().get(0).getId());
        assertEquals("Farola fundida", summary.getFilteredIncidents().get(0).getTitle());
    }

    @Test
    @DisplayName("toDto: Debe reflejar incidencia pendiente cuando no está resuelta")
    void shouldReflectPendingIncidentWhenNotResolved() {
        Incidencia inc = createSampleIncident(
                2L, "Bache en calzada", "Vías Públicas", "Cruz de Humilladero", "Los Tilos",
                EstadoIncidencia.IN_PROGRESS, PrioridadIncidencia.HIGH);

        AnalyticsSummaryResponse summary = mapper.toDto(inc);

        assertNotNull(summary);
        assertEquals(1L, summary.getOperationalMetrics().getTotalIncidents());
        assertEquals(0L, summary.getOperationalMetrics().getResolvedIncidents());
        assertEquals(1L, summary.getOperationalMetrics().getPendingIncidents());
    }

    // ==========================================
    // 2. toDtoList - Mapeo de Lista (BaseMapper)
    // ==========================================

    @Test
    @DisplayName("toDtoList: Debe retornar lista vacía ante null o lista vacía")
    void shouldReturnEmptyListWhenInputListIsNullOrEmpty() {
        List<AnalyticsSummaryResponse> fromNull = mapper.toDtoList(null);
        assertNotNull(fromNull);
        assertTrue(fromNull.isEmpty());

        List<AnalyticsSummaryResponse> fromEmpty = mapper.toDtoList(Collections.emptyList());
        assertNotNull(fromEmpty);
        assertTrue(fromEmpty.isEmpty());
    }

    @Test
    @DisplayName("toDtoList: Debe transformar cada elemento de la lista a su respectivo DTO")
    void shouldMapEachIncidentInListToSummaryResponse() {
        Incidencia inc1 = createSampleIncident(10L, "Inc 1", "Alumbrado", "Centro", "Soho", EstadoIncidencia.REPORTED, PrioridadIncidencia.LOW);
        Incidencia inc2 = createSampleIncident(20L, "Inc 2", "Limpieza", "Este", "El Palo", EstadoIncidencia.RESOLVED, PrioridadIncidencia.HIGH);

        List<AnalyticsSummaryResponse> list = mapper.toDtoList(List.of(inc1, inc2));

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(1L, list.get(0).getOperationalMetrics().getTotalIncidents());
        assertEquals(1L, list.get(1).getOperationalMetrics().getTotalIncidents());
    }

    // ==========================================
    // 3. toSummaryResponse con agregaciones automáticas
    // ==========================================

    @Test
    @DisplayName("toSummaryResponse: Debe retornar métricas a cero y listas vacías ante lista nula o vacía")
    void shouldReturnEmptySummaryWhenIncidentListIsNullOrEmpty() {
        AnalyticsFilterRequest filter = AnalyticsFilterRequest.builder().preset(AnalyticsTimePreset.LAST_7_DAYS).build();

        AnalyticsSummaryResponse nullSummary = mapper.toSummaryResponse(null, filter);
        assertNotNull(nullSummary);
        assertEquals(0L, nullSummary.getOperationalMetrics().getTotalIncidents());
        assertEquals(0L, nullSummary.getOperationalMetrics().getResolvedIncidents());
        assertEquals(0L, nullSummary.getOperationalMetrics().getPendingIncidents());
        assertTrue(nullSummary.getTerritorialMetrics().isEmpty());
        assertTrue(nullSummary.getCategoryMetrics().isEmpty());
        assertTrue(nullSummary.getFilteredIncidents().isEmpty());
        assertEquals(filter, nullSummary.getAppliedFilters());
        assertNotNull(nullSummary.getGeneratedAt());

        AnalyticsSummaryResponse emptySummary = mapper.toSummaryResponse(Collections.emptyList(), null);
        assertNotNull(emptySummary);
        assertEquals(0L, emptySummary.getOperationalMetrics().getTotalIncidents());
        assertNull(emptySummary.getAppliedFilters());
    }

    @Test
    @DisplayName("toSummaryResponse: Debe agregar correctamente múltiples incidencias agrupando por distrito y categoría")
    void shouldAggregateMultipleIncidentsCorrectly() {
        Incidencia inc1 = createSampleIncident(1L, "Inc 1", "Alumbrado", "Centro", "Soho", EstadoIncidencia.RESOLVED, PrioridadIncidencia.LOW);
        Incidencia inc2 = createSampleIncident(2L, "Inc 2", "Alumbrado", "Centro", "La Merced", EstadoIncidencia.RESOLVED, PrioridadIncidencia.MEDIUM);
        Incidencia inc3 = createSampleIncident(3L, "Inc 3", "Limpieza", "Cruz de Humilladero", "Los Tilos", EstadoIncidencia.IN_PROGRESS, PrioridadIncidencia.HIGH);

        AnalyticsFilterRequest filter = AnalyticsFilterRequest.builder().preset(AnalyticsTimePreset.LAST_30_DAYS).build();
        AnalyticsSummaryResponse summary = mapper.toSummaryResponse(List.of(inc1, inc2, inc3), filter);

        assertNotNull(summary);
        assertEquals(3L, summary.getOperationalMetrics().getTotalIncidents());
        assertEquals(2L, summary.getOperationalMetrics().getResolvedIncidents());
        assertEquals(1L, summary.getOperationalMetrics().getPendingIncidents());

        // Territorial: 2 en Centro, 1 en Cruz de Humilladero
        assertEquals(2, summary.getTerritorialMetrics().size());
        assertEquals("Centro", summary.getTerritorialMetrics().get(0).getTerritoryName());
        assertEquals(2L, summary.getTerritorialMetrics().get(0).getTotalIncidents());
        assertEquals(2L, summary.getTerritorialMetrics().get(0).getResolvedCount());
        assertEquals("Cruz de Humilladero", summary.getTerritorialMetrics().get(1).getTerritoryName());
        assertEquals(1L, summary.getTerritorialMetrics().get(1).getTotalIncidents());
        assertEquals(0L, summary.getTerritorialMetrics().get(1).getResolvedCount());

        // Categorías: 2 en Alumbrado, 1 en Limpieza
        assertEquals(2, summary.getCategoryMetrics().size());
        assertEquals("Alumbrado", summary.getCategoryMetrics().get(0).getCategory());
        assertEquals(2L, summary.getCategoryMetrics().get(0).getTotalIncidents());
        assertEquals("Limpieza", summary.getCategoryMetrics().get(1).getCategory());
        assertEquals(1L, summary.getCategoryMetrics().get(1).getTotalIncidents());

        // Listado de incidencias filtradas
        assertEquals(3, summary.getFilteredIncidents().size());
    }

    // ==========================================
    // 4. toSummaryResponse - Ensamblado con métricas precalculadas
    // ==========================================

    @Test
    @DisplayName("toSummaryResponse: Ensamblado con bloques externos y resiliencia ante parámetros nulos")
    void shouldAssembleSummaryResponseWithExternalMetrics() {
        OperationalMetricsResponse op = OperationalMetricsResponse.builder()
                .totalIncidents(50L)
                .resolvedIncidents(40L)
                .pendingIncidents(10L)
                .mtttHours(2.5)
                .mttrHours(24.0)
                .slaComplianceRate(95.0)
                .totalSlaBreached(2L)
                .totalSlaCompliant(48L)
                .build();

        TerritorialMetricResponse terr = TerritorialMetricResponse.builder().territoryName("Teatinos").totalIncidents(50L).build();
        DepartmentMetricResponse dept = mapper.toDepartmentMetricResponse(1L, "Vías Públicas", 50L, 40L, 24.0, 95.0);
        CategoryMetricResponse cat = CategoryMetricResponse.builder().category("Pavimento").totalIncidents(50L).build();
        AssistantPerformanceResponse ast = AssistantPerformanceResponse.builder().adoptionRate(88.0).build();

        Incidencia inc = createSampleIncident(100L, "Rotura acera", "Pavimento", "Teatinos", "Universidad", EstadoIncidencia.RESOLVED, PrioridadIncidencia.HIGH);

        AnalyticsSummaryResponse summary = mapper.toSummaryResponse(
                List.of(inc), op, List.of(terr), List.of(dept), List.of(cat), ast, null);

        assertNotNull(summary);
        assertEquals(op, summary.getOperationalMetrics());
        assertEquals(1, summary.getTerritorialMetrics().size());
        assertEquals(1, summary.getDepartmentMetrics().size());
        assertEquals(1, summary.getCategoryMetrics().size());
        assertEquals(ast, summary.getAssistantPerformance());
        assertEquals(1, summary.getFilteredIncidents().size());
        assertNotNull(summary.getGeneratedAt());

        // Verificación con listas nulas para comprobar null-safety
        AnalyticsSummaryResponse nullSafeSummary = mapper.toSummaryResponse(
                null, null, null, null, null, null, null);
        assertNotNull(nullSafeSummary);
        assertNotNull(nullSafeSummary.getOperationalMetrics());
        assertTrue(nullSafeSummary.getTerritorialMetrics().isEmpty());
        assertTrue(nullSafeSummary.getDepartmentMetrics().isEmpty());
        assertTrue(nullSafeSummary.getCategoryMetrics().isEmpty());
        assertNotNull(nullSafeSummary.getAssistantPerformance());
        assertTrue(nullSafeSummary.getFilteredIncidents().isEmpty());
    }

    // ==========================================
    // 5. toOperationalMetrics
    // ==========================================

    @Test
    @DisplayName("toOperationalMetrics: Conjuntos nulos y vacíos retornan ceros sin excepción")
    void shouldReturnZeroOperationalMetricsForEmptyOrNullList() {
        OperationalMetricsResponse nullMetrics = mapper.toOperationalMetrics(null, null, null, null, null, null);
        assertNotNull(nullMetrics);
        assertEquals(0L, nullMetrics.getTotalIncidents());
        assertEquals(0L, nullMetrics.getResolvedIncidents());
        assertEquals(0L, nullMetrics.getPendingIncidents());
        assertEquals(0.0, nullMetrics.getMtttHours());
        assertEquals(0.0, nullMetrics.getMttrHours());
        assertEquals(0.0, nullMetrics.getSlaComplianceRate());

        OperationalMetricsResponse emptyMetrics = mapper.toOperationalMetrics(Collections.emptyList(), 1.0, 2.0, 3.0, 4L, 5L);
        assertNotNull(emptyMetrics);
        assertEquals(0L, emptyMetrics.getTotalIncidents());
    }

    @Test
    @DisplayName("toOperationalMetrics: Contabiliza correctamente estados RESOLVED y CLOSED como resueltos")
    void shouldCountResolvedAndClosedAsResolved() {
        Incidencia inc1 = createSampleIncident(1L, "Inc 1", "A", "D", "N", EstadoIncidencia.RESOLVED, PrioridadIncidencia.LOW);
        Incidencia inc2 = createSampleIncident(2L, "Inc 2", "A", "D", "N", EstadoIncidencia.CLOSED, PrioridadIncidencia.LOW);
        Incidencia inc3 = createSampleIncident(3L, "Inc 3", "A", "D", "N", EstadoIncidencia.ASSIGNED, PrioridadIncidencia.LOW);
        Incidencia inc4 = createSampleIncident(4L, "Inc 4", "A", "D", "N", EstadoIncidencia.REPORTED, PrioridadIncidencia.LOW);

        OperationalMetricsResponse metrics = mapper.toOperationalMetrics(
                List.of(inc1, inc2, inc3, inc4), 5.0, 48.0, 80.0, 1L, 3L);

        assertEquals(4L, metrics.getTotalIncidents());
        assertEquals(2L, metrics.getResolvedIncidents());
        assertEquals(2L, metrics.getPendingIncidents());
        assertEquals(5.0, metrics.getMtttHours());
        assertEquals(48.0, metrics.getMttrHours());
        assertEquals(80.0, metrics.getSlaComplianceRate());
        assertEquals(1L, metrics.getTotalSlaBreached());
        assertEquals(3L, metrics.getTotalSlaCompliant());
    }

    // ==========================================
    // 6. toTerritorialMetricList
    // ==========================================

    @Test
    @DisplayName("toTerritorialMetricList: Retorna lista vacía ante null o lista vacía")
    void shouldReturnEmptyTerritorialMetricsForEmptyInput() {
        assertTrue(mapper.toTerritorialMetricList(null, false).isEmpty());
        assertTrue(mapper.toTerritorialMetricList(Collections.emptyList(), true).isEmpty());
    }

    @Test
    @DisplayName("toTerritorialMetricList: Asigna 'Sin distrito especificado' cuando el distrito es nulo o en blanco")
    void shouldHandleNullOrBlankDistrictGracefully() {
        Incidencia incWithNullDistrict = createSampleIncident(1L, "Inc 1", "A", null, "N1", EstadoIncidencia.RESOLVED, PrioridadIncidencia.LOW);
        Incidencia incWithBlankDistrict = createSampleIncident(2L, "Inc 2", "A", "   ", "N2", EstadoIncidencia.IN_PROGRESS, PrioridadIncidencia.LOW);
        Incidencia incWithValidDistrict = createSampleIncident(3L, "Inc 3", "A", "Centro", "N3", EstadoIncidencia.RESOLVED, PrioridadIncidencia.LOW);

        List<TerritorialMetricResponse> result = mapper.toTerritorialMetricList(
                List.of(incWithNullDistrict, incWithBlankDistrict, incWithValidDistrict), false);

        assertNotNull(result);
        assertEquals(2, result.size());

        TerritorialMetricResponse unknownGroup = result.stream()
                .filter(t -> t.getTerritoryName().equals(AnalyticsMapper.DEFAULT_UNKNOWN_DISTRICT))
                .findFirst()
                .orElse(null);

        assertNotNull(unknownGroup, "Debe existir un grupo con 'Sin distrito especificado'");
        assertEquals(2L, unknownGroup.getTotalIncidents());
        assertEquals(1L, unknownGroup.getResolvedCount());
        assertEquals(1L, unknownGroup.getPendingCount());
        assertEquals(50.0, unknownGroup.getSlaComplianceRate());
    }

    @Test
    @DisplayName("toTerritorialMetricList: Agrupa por barrio y asigna 'Sin barrio especificado' cuando es nulo")
    void shouldGroupByNeighborhoodAndHandleNulls() {
        Incidencia inc1 = createSampleIncident(1L, "Inc 1", "A", "Centro", "Soho", EstadoIncidencia.RESOLVED, PrioridadIncidencia.LOW);
        Incidencia inc2 = createSampleIncident(2L, "Inc 2", "A", "Centro", null, EstadoIncidencia.REPORTED, PrioridadIncidencia.LOW);

        List<TerritorialMetricResponse> result = mapper.toTerritorialMetricList(List.of(inc1, inc2), true);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(t -> t.getTerritoryName().equals("Soho")));
        assertTrue(result.stream().anyMatch(t -> t.getTerritoryName().equals(AnalyticsMapper.DEFAULT_UNKNOWN_NEIGHBORHOOD)));
    }

    // ==========================================
    // 7. toCategoryMetricList
    // ==========================================

    @Test
    @DisplayName("toCategoryMetricList: Retorna lista vacía ante null o vacía")
    void shouldReturnEmptyCategoryListForEmptyInput() {
        assertTrue(mapper.toCategoryMetricList(null).isEmpty());
        assertTrue(mapper.toCategoryMetricList(Collections.emptyList()).isEmpty());
    }

    @Test
    @DisplayName("toCategoryMetricList: Asigna 'Sin categoría' para categorías nulas o vacías")
    void shouldHandleNullOrBlankCategoryGracefully() {
        Incidencia inc1 = createSampleIncident(1L, "Inc 1", null, "Centro", "Soho", EstadoIncidencia.RESOLVED, PrioridadIncidencia.LOW);
        Incidencia inc2 = createSampleIncident(2L, "Inc 2", "  ", "Centro", "Soho", EstadoIncidencia.REPORTED, PrioridadIncidencia.LOW);
        Incidencia inc3 = createSampleIncident(3L, "Inc 3", "Limpieza", "Centro", "Soho", EstadoIncidencia.RESOLVED, PrioridadIncidencia.LOW);

        List<CategoryMetricResponse> result = mapper.toCategoryMetricList(List.of(inc1, inc2, inc3));

        assertEquals(2, result.size());
        CategoryMetricResponse unknownCategory = result.stream()
                .filter(c -> c.getCategory().equals(AnalyticsMapper.DEFAULT_UNKNOWN_CATEGORY))
                .findFirst()
                .orElse(null);

        assertNotNull(unknownCategory);
        assertEquals(2L, unknownCategory.getTotalIncidents());
        assertEquals(1L, unknownCategory.getResolvedCount());
    }

    // ==========================================
    // 8. toIncidentResponseList
    // ==========================================

    @Test
    @DisplayName("toIncidentResponseList: Retorna lista vacía ante null o lista vacía")
    void shouldReturnEmptyIncidentResponseListForEmptyInput() {
        assertTrue(mapper.toIncidentResponseList(null).isEmpty());
        assertTrue(mapper.toIncidentResponseList(Collections.emptyList()).isEmpty());
    }

    @Test
    @DisplayName("toIncidentResponseList: Convierte entidades ignorando elementos nulos dentro de la colección")
    void shouldConvertIncidentsToIncidentResponseList() {
        Incidencia inc = createSampleIncident(55L, "Semáforo estropeado", "Movilidad", "Carretera de Cádiz", "Huelin", EstadoIncidencia.VALIDATED, PrioridadIncidencia.CRITICAL);
        List<Incidencia> listWithNull = new ArrayList<>();
        listWithNull.add(inc);
        listWithNull.add(null);

        List<IncidentResponse> dtoList = mapper.toIncidentResponseList(listWithNull);

        assertEquals(1, dtoList.size());
        assertEquals(55L, dtoList.get(0).getId());
        assertEquals("Semáforo estropeado", dtoList.get(0).getTitle());
    }

    // ==========================================
    // 9. toDepartmentMetricResponse
    // ==========================================

    @Test
    @DisplayName("toDepartmentMetricResponse: Construye DTO y asigna valores predeterminados para parámetros nulos")
    void shouldBuildDepartmentMetricResponseWithDefaults() {
        DepartmentMetricResponse deptWithNulls = mapper.toDepartmentMetricResponse(
                5L, "Medio Ambiente", null, null, null, null);

        assertNotNull(deptWithNulls);
        assertEquals(5L, deptWithNulls.getDepartmentId());
        assertEquals("Medio Ambiente", deptWithNulls.getDepartmentName());
        assertEquals(0L, deptWithNulls.getTotalAssigned());
        assertEquals(0L, deptWithNulls.getResolvedCount());
        assertEquals(0.0, deptWithNulls.getAvgResolutionHours());
        assertEquals(0.0, deptWithNulls.getSlaComplianceRate());
    }

    // ==========================================
    // 10. Constructores
    // ==========================================

    @Test
    @DisplayName("Constructores: Constructor con mapper nulo utiliza fallback seguro sin lanzar NPE")
    void shouldHandleNullIncidentMapperInConstructor() {
        AnalyticsMapper customMapper = new AnalyticsMapper(null);
        Incidencia inc = createSampleIncident(1L, "Test", "Cat", "Dist", "Neigh", EstadoIncidencia.REPORTED, PrioridadIncidencia.LOW);

        assertNotNull(customMapper.toDto(inc));
    }
}

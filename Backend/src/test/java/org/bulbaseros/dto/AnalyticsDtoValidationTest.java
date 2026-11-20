package org.bulbaseros.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de Bean Validation, constructores, constructores fluidos (Builder),
 * getters, setters y contratos de equivalencia para los DTOs y enums de analítica urbana.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class AnalyticsDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // ==========================================
    // 1. AnalyticsTimePreset Enum Tests
    // ==========================================

    @Test
    @DisplayName("AnalyticsTimePreset: Debe contener exactamente los 7 presets definidos en RF-8")
    void shouldContainAllExpectedPresets() {
        AnalyticsTimePreset[] presets = AnalyticsTimePreset.values();
        assertEquals(7, presets.length, "Debe haber exactamente 7 presets temporales definidos");

        assertEquals(AnalyticsTimePreset.TODAY, AnalyticsTimePreset.valueOf("TODAY"));
        assertEquals(AnalyticsTimePreset.LAST_7_DAYS, AnalyticsTimePreset.valueOf("LAST_7_DAYS"));
        assertEquals(AnalyticsTimePreset.LAST_30_DAYS, AnalyticsTimePreset.valueOf("LAST_30_DAYS"));
        assertEquals(AnalyticsTimePreset.LAST_3_MONTHS, AnalyticsTimePreset.valueOf("LAST_3_MONTHS"));
        assertEquals(AnalyticsTimePreset.LAST_6_MONTHS, AnalyticsTimePreset.valueOf("LAST_6_MONTHS"));
        assertEquals(AnalyticsTimePreset.LAST_YEAR, AnalyticsTimePreset.valueOf("LAST_YEAR"));
        assertEquals(AnalyticsTimePreset.SPECIFIC_YEAR, AnalyticsTimePreset.valueOf("SPECIFIC_YEAR"));
    }

    // ==========================================
    // 2. AnalyticsFilterRequest Tests
    // ==========================================

    @Test
    @DisplayName("AnalyticsFilterRequest: Debe ser válido cuando los campos opcionales son nulos")
    void shouldBeValidWhenAllFilterFieldsAreNull() {
        AnalyticsFilterRequest request = new AnalyticsFilterRequest();
        Set<ConstraintViolation<AnalyticsFilterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Un filtro vacío no debe tener violaciones de validación");
    }

    @Test
    @DisplayName("AnalyticsFilterRequest: Debe ser válido con todos los campos correctamente formateados")
    void shouldBeValidWithFullPopulatedFilter() {
        OffsetDateTime now = OffsetDateTime.now();
        AnalyticsFilterRequest request = AnalyticsFilterRequest.builder()
                .preset(AnalyticsTimePreset.LAST_30_DAYS)
                .startDate(now.minusDays(30))
                .endDate(now)
                .district("Centro")
                .neighborhood("La Malagueta")
                .year(2025)
                .build();

        Set<ConstraintViolation<AnalyticsFilterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
        assertEquals(AnalyticsTimePreset.LAST_30_DAYS, request.getPreset());
        assertEquals("Centro", request.getDistrict());
        assertEquals("La Malagueta", request.getNeighborhood());
        assertEquals(2025, request.getYear());
        assertNotNull(request.getStartDate());
        assertNotNull(request.getEndDate());
    }

    @Test
    @DisplayName("AnalyticsFilterRequest: Debe fallar si el distrito supera los 100 caracteres")
    void shouldFailWhenDistrictExceeds100Characters() {
        String longDistrict = "A".repeat(101);
        AnalyticsFilterRequest request = AnalyticsFilterRequest.builder()
                .district(longDistrict)
                .build();

        Set<ConstraintViolation<AnalyticsFilterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("district")));
    }

    @Test
    @DisplayName("AnalyticsFilterRequest: Debe fallar si el barrio supera los 100 caracteres")
    void shouldFailWhenNeighborhoodExceeds100Characters() {
        String longNeighborhood = "B".repeat(101);
        AnalyticsFilterRequest request = AnalyticsFilterRequest.builder()
                .neighborhood(longNeighborhood)
                .build();

        Set<ConstraintViolation<AnalyticsFilterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("neighborhood")));
    }

    @Test
    @DisplayName("AnalyticsFilterRequest: Debe fallar si el año es menor al 2000 o mayor al 2100")
    void shouldFailWhenYearIsOutOfRange() {
        AnalyticsFilterRequest requestTooLow = AnalyticsFilterRequest.builder().year(1999).build();
        Set<ConstraintViolation<AnalyticsFilterRequest>> violationsLow = validator.validate(requestTooLow);
        assertFalse(violationsLow.isEmpty());
        assertTrue(violationsLow.stream().anyMatch(v -> v.getPropertyPath().toString().equals("year")));

        AnalyticsFilterRequest requestTooHigh = AnalyticsFilterRequest.builder().year(2101).build();
        Set<ConstraintViolation<AnalyticsFilterRequest>> violationsHigh = validator.validate(requestTooHigh);
        assertFalse(violationsHigh.isEmpty());
        assertTrue(violationsHigh.stream().anyMatch(v -> v.getPropertyPath().toString().equals("year")));

        AnalyticsFilterRequest validMinYear = AnalyticsFilterRequest.builder().year(2000).build();
        assertTrue(validator.validate(validMinYear).isEmpty());

        AnalyticsFilterRequest validMaxYear = AnalyticsFilterRequest.builder().year(2100).build();
        assertTrue(validator.validate(validMaxYear).isEmpty());
    }

    @Test
    @DisplayName("AnalyticsFilterRequest: Verificar getters, setters, toString y equals")
    void shouldVerifyAnalyticsFilterRequestGettersSettersAndContracts() {
        AnalyticsFilterRequest req1 = new AnalyticsFilterRequest();
        req1.setPreset(AnalyticsTimePreset.TODAY);
        req1.setDistrict("Este");
        req1.setNeighborhood("Pedregalejo");
        req1.setYear(2026);

        AnalyticsFilterRequest req2 = AnalyticsFilterRequest.builder()
                .preset(AnalyticsTimePreset.TODAY)
                .district("Este")
                .neighborhood("Pedregalejo")
                .year(2026)
                .build();

        assertEquals(req1, req2);
        assertEquals(req1.hashCode(), req2.hashCode());
        assertTrue(req1.toString().contains("Pedregalejo"));
    }

    // ==========================================
    // 3. OperationalMetricsResponse Tests
    // ==========================================

    @Test
    @DisplayName("OperationalMetricsResponse: Construcción, asignación y lectura de campos")
    void shouldBuildAndReadOperationalMetricsResponse() {
        OperationalMetricsResponse metrics = OperationalMetricsResponse.builder()
                .totalIncidents(100L)
                .resolvedIncidents(80L)
                .pendingIncidents(20L)
                .mtttHours(4.5)
                .mttrHours(36.2)
                .slaComplianceRate(92.5)
                .totalSlaBreached(8L)
                .totalSlaCompliant(92L)
                .build();

        assertEquals(100L, metrics.getTotalIncidents());
        assertEquals(80L, metrics.getResolvedIncidents());
        assertEquals(20L, metrics.getPendingIncidents());
        assertEquals(4.5, metrics.getMtttHours());
        assertEquals(36.2, metrics.getMttrHours());
        assertEquals(92.5, metrics.getSlaComplianceRate());
        assertEquals(8L, metrics.getTotalSlaBreached());
        assertEquals(92L, metrics.getTotalSlaCompliant());

        OperationalMetricsResponse metrics2 = new OperationalMetricsResponse(100L, 80L, 20L, 4.5, 36.2, 92.5, 8L, 92L);
        assertEquals(metrics, metrics2);
        assertEquals(metrics.hashCode(), metrics2.hashCode());
        assertTrue(metrics.toString().contains("mtttHours=4.5"));
    }

    // ==========================================
    // 4. TerritorialMetricResponse Tests
    // ==========================================

    @Test
    @DisplayName("TerritorialMetricResponse: Construcción, asignación y lectura de campos")
    void shouldBuildAndReadTerritorialMetricResponse() {
        TerritorialMetricResponse territorial = TerritorialMetricResponse.builder()
                .territoryName("Teatinos-Universidad")
                .totalIncidents(45L)
                .resolvedCount(35L)
                .pendingCount(10L)
                .slaComplianceRate(88.0)
                .build();

        assertEquals("Teatinos-Universidad", territorial.getTerritoryName());
        assertEquals(45L, territorial.getTotalIncidents());
        assertEquals(35L, territorial.getResolvedCount());
        assertEquals(10L, territorial.getPendingCount());
        assertEquals(88.0, territorial.getSlaComplianceRate());

        TerritorialMetricResponse territorial2 = new TerritorialMetricResponse(
                "Teatinos-Universidad", 45L, 35L, 10L, 88.0);
        assertEquals(territorial, territorial2);
        assertEquals(territorial.hashCode(), territorial2.hashCode());
        assertTrue(territorial.toString().contains("Teatinos-Universidad"));
    }

    // ==========================================
    // 5. DepartmentMetricResponse Tests
    // ==========================================

    @Test
    @DisplayName("DepartmentMetricResponse: Construcción, asignación y lectura de campos")
    void shouldBuildAndReadDepartmentMetricResponse() {
        DepartmentMetricResponse dept = DepartmentMetricResponse.builder()
                .departmentId(1L)
                .departmentName("Alumbrado Público")
                .totalAssigned(50L)
                .resolvedCount(42L)
                .avgResolutionHours(18.5)
                .slaComplianceRate(90.0)
                .build();

        assertEquals(1L, dept.getDepartmentId());
        assertEquals("Alumbrado Público", dept.getDepartmentName());
        assertEquals(50L, dept.getTotalAssigned());
        assertEquals(42L, dept.getResolvedCount());
        assertEquals(18.5, dept.getAvgResolutionHours());
        assertEquals(90.0, dept.getSlaComplianceRate());

        DepartmentMetricResponse dept2 = new DepartmentMetricResponse(
                1L, "Alumbrado Público", 50L, 42L, 18.5, 90.0);
        assertEquals(dept, dept2);
        assertEquals(dept.hashCode(), dept2.hashCode());
        assertTrue(dept.toString().contains("Alumbrado Público"));
    }

    // ==========================================
    // 6. CategoryMetricResponse Tests
    // ==========================================

    @Test
    @DisplayName("CategoryMetricResponse: Construcción, asignación y lectura de campos")
    void shouldBuildAndReadCategoryMetricResponse() {
        CategoryMetricResponse cat = CategoryMetricResponse.builder()
                .category("Vías Públicas")
                .totalIncidents(30L)
                .resolvedCount(25L)
                .slaComplianceRate(83.33)
                .build();

        assertEquals("Vías Públicas", cat.getCategory());
        assertEquals(30L, cat.getTotalIncidents());
        assertEquals(25L, cat.getResolvedCount());
        assertEquals(83.33, cat.getSlaComplianceRate());

        CategoryMetricResponse cat2 = new CategoryMetricResponse("Vías Públicas", 30L, 25L, 83.33);
        assertEquals(cat, cat2);
        assertEquals(cat.hashCode(), cat2.hashCode());
        assertTrue(cat.toString().contains("Vías Públicas"));
    }

    // ==========================================
    // 7. DepartmentRecommendationResponse Tests
    // ==========================================

    @Test
    @DisplayName("DepartmentRecommendationResponse: Construcción, asignación y banderas de advertencia")
    void shouldBuildAndReadDepartmentRecommendationResponse() {
        DepartmentRecommendationResponse recHigh = DepartmentRecommendationResponse.builder()
                .suggestedDepartmentId(2L)
                .suggestedDepartmentName("Parques y Jardines")
                .confidencePercentage(85.0)
                .isLowConfidenceWarning(false)
                .reasoning("Coincidencia léxica con 'árbol caído' y 'rama'")
                .build();

        assertFalse(recHigh.getIsLowConfidenceWarning());
        assertEquals(85.0, recHigh.getConfidencePercentage());
        assertEquals("Parques y Jardines", recHigh.getSuggestedDepartmentName());

        DepartmentRecommendationResponse recLow = DepartmentRecommendationResponse.builder()
                .suggestedDepartmentId(3L)
                .suggestedDepartmentName("Limpieza")
                .confidencePercentage(42.0)
                .isLowConfidenceWarning(true)
                .reasoning("Descripción ambigua con baja frecuencia léxica")
                .build();

        assertTrue(recLow.getIsLowConfidenceWarning());
        assertEquals(42.0, recLow.getConfidencePercentage());

        DepartmentRecommendationResponse rec2 = new DepartmentRecommendationResponse(
                2L, "Parques y Jardines", 85.0, false, "Coincidencia léxica con 'árbol caído' y 'rama'");
        assertEquals(recHigh, rec2);
        assertEquals(recHigh.hashCode(), rec2.hashCode());
        assertTrue(recHigh.toString().contains("Parques y Jardines"));
    }

    // ==========================================
    // 8. AssistantPerformanceResponse Tests
    // ==========================================

    @Test
    @DisplayName("AssistantPerformanceResponse: Construcción, asignación y lectura de campos")
    void shouldBuildAndReadAssistantPerformanceResponse() {
        AssistantPerformanceResponse perf = AssistantPerformanceResponse.builder()
                .totalRecommendations(200L)
                .acceptedRecommendations(170L)
                .adoptionRate(85.0)
                .averageConfidence(81.4)
                .build();

        assertEquals(200L, perf.getTotalRecommendations());
        assertEquals(170L, perf.getAcceptedRecommendations());
        assertEquals(85.0, perf.getAdoptionRate());
        assertEquals(81.4, perf.getAverageConfidence());

        AssistantPerformanceResponse perf2 = new AssistantPerformanceResponse(200L, 170L, 85.0, 81.4);
        assertEquals(perf, perf2);
        assertEquals(perf.hashCode(), perf2.hashCode());
        assertTrue(perf.toString().contains("adoptionRate=85.0"));
    }

    // ==========================================
    // 9. AnalyticsSummaryResponse Tests
    // ==========================================

    @Test
    @DisplayName("AnalyticsSummaryResponse: Consolidación integral de todos los bloques")
    void shouldBuildAndReadAnalyticsSummaryResponse() {
        OffsetDateTime now = OffsetDateTime.now();
        OperationalMetricsResponse op = OperationalMetricsResponse.builder().totalIncidents(10L).build();
        TerritorialMetricResponse terr = TerritorialMetricResponse.builder().territoryName("Centro").build();
        DepartmentMetricResponse dept = DepartmentMetricResponse.builder().departmentName("Limpieza").build();
        CategoryMetricResponse cat = CategoryMetricResponse.builder().category("Alumbrado").build();
        AssistantPerformanceResponse ast = AssistantPerformanceResponse.builder().adoptionRate(90.0).build();
        IncidentResponse inc = new IncidentResponse();
        inc.setId(101L);
        inc.setTitle("Farola apagada");
        AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder().preset(AnalyticsTimePreset.TODAY).build();

        AnalyticsSummaryResponse summary = AnalyticsSummaryResponse.builder()
                .operationalMetrics(op)
                .territorialMetrics(List.of(terr))
                .departmentMetrics(List.of(dept))
                .categoryMetrics(List.of(cat))
                .assistantPerformance(ast)
                .filteredIncidents(List.of(inc))
                .appliedFilters(filters)
                .generatedAt(now)
                .build();

        assertEquals(op, summary.getOperationalMetrics());
        assertEquals(1, summary.getTerritorialMetrics().size());
        assertEquals("Centro", summary.getTerritorialMetrics().get(0).getTerritoryName());
        assertEquals(1, summary.getDepartmentMetrics().size());
        assertEquals("Limpieza", summary.getDepartmentMetrics().get(0).getDepartmentName());
        assertEquals(1, summary.getCategoryMetrics().size());
        assertEquals("Alumbrado", summary.getCategoryMetrics().get(0).getCategory());
        assertEquals(90.0, summary.getAssistantPerformance().getAdoptionRate());
        assertEquals(1, summary.getFilteredIncidents().size());
        assertEquals(101L, summary.getFilteredIncidents().get(0).getId());
        assertEquals(filters, summary.getAppliedFilters());
        assertEquals(now, summary.getGeneratedAt());

        AnalyticsSummaryResponse summary2 = new AnalyticsSummaryResponse(
                op, List.of(terr), List.of(dept), List.of(cat), ast, List.of(inc), filters, now);
        assertEquals(summary, summary2);
        assertEquals(summary.hashCode(), summary2.hashCode());
        assertTrue(summary.toString().contains("farola apagada") || summary.toString().contains("Centro"));
    }
}

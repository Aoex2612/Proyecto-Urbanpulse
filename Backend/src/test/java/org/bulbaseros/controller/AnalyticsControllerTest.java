package org.bulbaseros.controller;

import org.bulbaseros.dto.AnalyticsFilterRequest;
import org.bulbaseros.dto.AnalyticsSummaryResponse;
import org.bulbaseros.dto.AnalyticsTimePreset;
import org.bulbaseros.dto.AssistantPerformanceResponse;
import org.bulbaseros.dto.CategoryMetricResponse;
import org.bulbaseros.dto.DepartmentMetricResponse;
import org.bulbaseros.dto.DepartmentRecommendationResponse;
import org.bulbaseros.dto.OperationalMetricsResponse;
import org.bulbaseros.dto.TerritorialMetricResponse;
import org.bulbaseros.exception.IncidentNotFoundException;
import org.bulbaseros.service.AnalyticsService;
import org.bulbaseros.service.IntelligentRecommendationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite de pruebas de integración y seguridad basada en roles (RBAC) con MockMvc
 * para {@link AnalyticsController}.
 * <p>
 * Verifica exhaustivamente:
 * <ul>
 *   <li>Autenticación y autorización estricta en {@code GET /api/analytics/summary} y
 *       {@code GET /api/analytics/recommend-department}.</li>
 *   <li>Denegación de acceso (401 Unauthorized y 403 Forbidden) para peticiones anónimas
 *       o de usuarios con roles sin privilegios (CITIZEN, TECHNICIAN).</li>
 *   <li>Acceso autorizado (200 OK) para roles con permisos analíticos y operativos
 *       (ANALYST, ADMIN, OPERATOR).</li>
 *   <li>Enlace y validación de parámetros de filtrado temporal y territorial.</li>
 *   <li>Resolución de recomendaciones asistidas con detección de advertencia por baja confianza
 *       y manejo de errores 404 ante incidencias inexistentes.</li>
 * </ul>
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @MockitoBean
    private IntelligentRecommendationService intelligentRecommendationService;

    private AnalyticsSummaryResponse buildMockSummaryResponse() {
        return AnalyticsSummaryResponse.builder()
                .operationalMetrics(OperationalMetricsResponse.builder()
                        .totalIncidents(10L)
                        .resolvedIncidents(8L)
                        .pendingIncidents(2L)
                        .mtttHours(1.5)
                        .mttrHours(24.0)
                        .slaComplianceRate(90.0)
                        .totalSlaCompliant(9L)
                        .totalSlaBreached(1L)
                        .build())
                .territorialMetrics(List.of(
                        TerritorialMetricResponse.builder()
                                .territoryName("Centro")
                                .totalIncidents(5L)
                                .resolvedCount(4L)
                                .pendingCount(1L)
                                .slaComplianceRate(80.0)
                                .build()
                ))
                .departmentMetrics(List.of(
                        DepartmentMetricResponse.builder()
                                .departmentId(1L)
                                .departmentName("Vía Pública")
                                .totalAssigned(6L)
                                .resolvedCount(5L)
                                .avgResolutionHours(18.5)
                                .slaComplianceRate(92.0)
                                .build()
                ))
                .categoryMetrics(List.of(
                        CategoryMetricResponse.builder()
                                .category("Alumbrado")
                                .totalIncidents(4L)
                                .resolvedCount(4L)
                                .slaComplianceRate(100.0)
                                .build()
                ))
                .assistantPerformance(AssistantPerformanceResponse.builder()
                        .totalRecommendations(10L)
                        .acceptedRecommendations(8L)
                        .adoptionRate(80.0)
                        .averageConfidence(85.5)
                        .build())
                .filteredIncidents(Collections.emptyList())
                .appliedFilters(AnalyticsFilterRequest.builder()
                        .preset(AnalyticsTimePreset.LAST_7_DAYS)
                        .district("Centro")
                        .build())
                .generatedAt(OffsetDateTime.of(2026, 11, 7, 20, 0, 0, 0, ZoneOffset.UTC))
                .build();
    }

    private DepartmentRecommendationResponse buildMockRecommendationResponse(boolean lowConfidence) {
        if (lowConfidence) {
            return DepartmentRecommendationResponse.builder()
                    .suggestedDepartmentId(2L)
                    .suggestedDepartmentName("Medio Ambiente")
                    .confidencePercentage(35.0)
                    .isLowConfidenceWarning(true)
                    .reasoning("Texto escueto y ambiguo. Se aconseja validación manual.")
                    .build();
        }
        return DepartmentRecommendationResponse.builder()
                .suggestedDepartmentId(1L)
                .suggestedDepartmentName("Vía Pública")
                .confidencePercentage(88.0)
                .isLowConfidenceWarning(false)
                .reasoning("Recomendación de alta confianza basada en coincidencias léxicas con Vía Pública: bache, asfalto")
                .build();
    }

    @Nested
    @DisplayName("Pruebas de Seguridad y Acceso en GET /api/analytics/summary")
    class SummarySecurityTests {

        @Test
        @DisplayName("GET /api/analytics/summary sin autenticación debe responder 401 Unauthorized")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/analytics/summary"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Unauthorized"));
        }

        @Test
        @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
        @DisplayName("GET /api/analytics/summary con rol CITIZEN debe responder 403 Forbidden")
        void shouldReturn403WhenRoleIsCitizen() throws Exception {
            mockMvc.perform(get("/api/analytics/summary"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @WithMockUser(username = "technician@urbanpulse.es", roles = "TECHNICIAN")
        @DisplayName("GET /api/analytics/summary con rol TECHNICIAN debe responder 403 Forbidden")
        void shouldReturn403WhenRoleIsTechnician() throws Exception {
            mockMvc.perform(get("/api/analytics/summary"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @WithMockUser(username = "analyst@urbanpulse.es", roles = "ANALYST")
        @DisplayName("GET /api/analytics/summary con rol ANALYST debe responder 200 OK y retornar estructura consolidada")
        void shouldReturn200AndPayloadWhenRoleIsAnalyst() throws Exception {
            when(analyticsService.getAnalyticsSummary(any())).thenReturn(buildMockSummaryResponse());

            mockMvc.perform(get("/api/analytics/summary")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.operationalMetrics.totalIncidents").value(10))
                    .andExpect(jsonPath("$.operationalMetrics.mtttHours").value(1.5))
                    .andExpect(jsonPath("$.operationalMetrics.mttrHours").value(24.0))
                    .andExpect(jsonPath("$.operationalMetrics.slaComplianceRate").value(90.0))
                    .andExpect(jsonPath("$.territorialMetrics[0].territoryName").value("Centro"))
                    .andExpect(jsonPath("$.departmentMetrics[0].departmentName").value("Vía Pública"))
                    .andExpect(jsonPath("$.categoryMetrics[0].category").value("Alumbrado"))
                    .andExpect(jsonPath("$.assistantPerformance.adoptionRate").value(80.0));
        }

        @Test
        @WithMockUser(username = "admin@urbanpulse.es", roles = "ADMIN")
        @DisplayName("GET /api/analytics/summary con rol ADMIN debe responder 200 OK")
        void shouldReturn200WhenRoleIsAdmin() throws Exception {
            when(analyticsService.getAnalyticsSummary(any())).thenReturn(buildMockSummaryResponse());

            mockMvc.perform(get("/api/analytics/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.operationalMetrics.totalIncidents").value(10));
        }

        @Test
        @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
        @DisplayName("GET /api/analytics/summary con rol OPERATOR debe responder 200 OK")
        void shouldReturn200WhenRoleIsOperator() throws Exception {
            when(analyticsService.getAnalyticsSummary(any())).thenReturn(buildMockSummaryResponse());

            mockMvc.perform(get("/api/analytics/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.operationalMetrics.totalIncidents").value(10));
        }
    }

    @Nested
    @DisplayName("Pruebas de Parámetros de Filtrado en GET /api/analytics/summary")
    class SummaryFilteringTests {

        @Test
        @WithMockUser(username = "analyst@urbanpulse.es", roles = "ANALYST")
        @DisplayName("GET /api/analytics/summary con parámetros válidos de filtro preset y distrito")
        void shouldBindValidFilterParametersCorrectly() throws Exception {
            when(analyticsService.getAnalyticsSummary(any())).thenReturn(buildMockSummaryResponse());

            mockMvc.perform(get("/api/analytics/summary")
                            .param("preset", "LAST_7_DAYS")
                            .param("district", "Centro")
                            .param("neighborhood", "Soho"))
                    .andExpect(status().isOk());

            ArgumentCaptor<AnalyticsFilterRequest> captor = ArgumentCaptor.forClass(AnalyticsFilterRequest.class);
            verify(analyticsService).getAnalyticsSummary(captor.capture());

            AnalyticsFilterRequest captured = captor.getValue();
            assertEquals(AnalyticsTimePreset.LAST_7_DAYS, captured.getPreset());
            assertEquals("Centro", captured.getDistrict());
            assertEquals("Soho", captured.getNeighborhood());
        }

        @Test
        @WithMockUser(username = "analyst@urbanpulse.es", roles = "ANALYST")
        @DisplayName("GET /api/analytics/summary con año inválido fuera de rango debe responder 400 Bad Request")
        void shouldReturn400WhenYearIsInvalid() throws Exception {
            mockMvc.perform(get("/api/analytics/summary")
                            .param("year", "1995"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("Pruebas de Seguridad y Asistencia en GET /api/analytics/recommend-department")
    class RecommendationEndpointTests {

        @Test
        @DisplayName("GET /api/analytics/recommend-department sin autenticación debe responder 401 Unauthorized")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/analytics/recommend-department")
                            .param("incidentId", "10"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
        @DisplayName("GET /api/analytics/recommend-department con rol CITIZEN debe responder 403 Forbidden")
        void shouldReturn403WhenRoleIsCitizen() throws Exception {
            mockMvc.perform(get("/api/analytics/recommend-department")
                            .param("incidentId", "10"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @WithMockUser(username = "technician@urbanpulse.es", roles = "TECHNICIAN")
        @DisplayName("GET /api/analytics/recommend-department con rol TECHNICIAN debe responder 403 Forbidden")
        void shouldReturn403WhenRoleIsTechnician() throws Exception {
            mockMvc.perform(get("/api/analytics/recommend-department")
                            .param("incidentId", "10"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
        @DisplayName("GET /api/analytics/recommend-department con incidentId retorna recomendación asistida 200 OK")
        void shouldReturnRecommendationByIncidentId() throws Exception {
            when(intelligentRecommendationService.recommendDepartment(eq(42L)))
                    .thenReturn(buildMockRecommendationResponse(false));

            mockMvc.perform(get("/api/analytics/recommend-department")
                            .param("incidentId", "42"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.suggestedDepartmentId").value(1))
                    .andExpect(jsonPath("$.suggestedDepartmentName").value("Vía Pública"))
                    .andExpect(jsonPath("$.confidencePercentage").value(88.0))
                    .andExpect(jsonPath("$.isLowConfidenceWarning").value(false))
                    .andExpect(jsonPath("$.reasoning").isNotEmpty());

            verify(intelligentRecommendationService).recommendDepartment(42L);
        }

        @Test
        @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
        @DisplayName("GET /api/analytics/recommend-department con texto directo retorna recomendación 200 OK")
        void shouldReturnRecommendationByTextAttributes() throws Exception {
            when(intelligentRecommendationService.recommendDepartment(
                    eq("Bache pronunciado"), eq("En mitad del paso peatonal"), eq("Vías")))
                    .thenReturn(buildMockRecommendationResponse(false));

            mockMvc.perform(get("/api/analytics/recommend-department")
                            .param("title", "Bache pronunciado")
                            .param("description", "En mitad del paso peatonal")
                            .param("category", "Vías"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.suggestedDepartmentName").value("Vía Pública"))
                    .andExpect(jsonPath("$.confidencePercentage").value(88.0))
                    .andExpect(jsonPath("$.isLowConfidenceWarning").value(false));

            verify(intelligentRecommendationService).recommendDepartment(
                    "Bache pronunciado", "En mitad del paso peatonal", "Vías");
        }

        @Test
        @WithMockUser(username = "analyst@urbanpulse.es", roles = "ANALYST")
        @DisplayName("GET /api/analytics/recommend-department con baja confianza activa advertencia explícita")
        void shouldReturnLowConfidenceWarning() throws Exception {
            when(intelligentRecommendationService.recommendDepartment(anyString(), anyString(), anyString()))
                    .thenReturn(buildMockRecommendationResponse(true));

            mockMvc.perform(get("/api/analytics/recommend-department")
                            .param("title", "Ruido")
                            .param("description", "molesta")
                            .param("category", "Varios"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isLowConfidenceWarning").value(true))
                    .andExpect(jsonPath("$.confidencePercentage").value(35.0));
        }

        @Test
        @WithMockUser(username = "admin@urbanpulse.es", roles = "ADMIN")
        @DisplayName("GET /api/analytics/recommend-department con incidentId inexistente debe responder 404 Not Found")
        void shouldReturn404WhenIncidentDoesNotExist() throws Exception {
            when(intelligentRecommendationService.recommendDepartment(eq(9999L)))
                    .thenThrow(new IncidentNotFoundException(9999L));

            mockMvc.perform(get("/api/analytics/recommend-department")
                            .param("incidentId", "9999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}

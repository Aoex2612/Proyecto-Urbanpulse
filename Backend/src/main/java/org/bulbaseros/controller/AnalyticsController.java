package org.bulbaseros.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.bulbaseros.dto.AnalyticsFilterRequest;
import org.bulbaseros.dto.AnalyticsSummaryResponse;
import org.bulbaseros.dto.DepartmentRecommendationResponse;
import org.bulbaseros.service.AnalyticsService;
import org.bulbaseros.service.IntelligentRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST que gestiona las operaciones de analítica urbana, métricas operacionales
 * y asistencia inteligente en el sistema UrbanPulse.
 * <p>
 * Centraliza los endpoints protegidos con seguridad basada en roles (RBAC) para el cálculo y consulta
 * de indicadores clave de rendimiento (KPIs como MTTT y MTTR), evaluación de acuerdos de nivel de servicio (SLA),
 * desgloses agregados por territorio (distritos y barrios), por departamento municipal y por categoría temática
 * (RF-1 al RF-11, RF-18, RF-19, RF-20, RF-21), así como la sugerencia heurística asistida del departamento
 * idóneo para el triaje y asignación de incidencias con cálculo de grado de certeza (RF-14, RF-15, RF-16).
 * </p>
 * <p>
 * El acceso a todos los endpoints de este controlador está restringido a usuarios con los roles
 * {@code ANALYST}, {@code ADMIN} u {@code OPERATOR}, aplicando defensa en profundidad mediante
 * {@link PreAuthorize} a nivel de clase complementando las reglas perimetrales de seguridad HTTP.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@RestController
@RequestMapping({"/api/v1/analytics", "/api/analytics"})
@PreAuthorize("hasAnyRole('ANALYST', 'ADMIN', 'OPERATOR')")
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final IntelligentRecommendationService intelligentRecommendationService;

    /**
     * Construye una nueva instancia de {@link AnalyticsController} inyectando los servicios de negocio requeridos.
     *
     * @param analyticsService                 Servicio central de analítica urbana, evaluación de SLAs y agregaciones.
     * @param intelligentRecommendationService Servicio de asistencia inteligente y cálculo de confianza heurística.
     */
    @Autowired
    public AnalyticsController(AnalyticsService analyticsService,
                               IntelligentRecommendationService intelligentRecommendationService) {
        this.analyticsService = analyticsService;
        this.intelligentRecommendationService = intelligentRecommendationService;
    }

    /**
     * Recupera el resumen consolidado de analítica urbana en función de los criterios de filtrado temporal y territorial.
     * <p>
     * Admite parámetros opcionales de filtro mediante enlace de modelo {@link AnalyticsFilterRequest}:
     * preset temporal predefinido ({@code preset}), fechas de inicio y fin personalizadas ({@code startDate}, {@code endDate}),
     * año específico ({@code year}), distrito ({@code district}) y barrio ({@code neighborhood}).
     * </p>
     *
     * @param filterRequest Criterios de filtrado validados mediante Bean Validation.
     * @return {@link ResponseEntity} con código HTTP 200 OK y el DTO consolidado {@link AnalyticsSummaryResponse}.
     */
    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryResponse> getAnalyticsSummary(
            @Valid @ModelAttribute AnalyticsFilterRequest filterRequest) {
        log.info("Petición REST GET /api/analytics/summary con filtros: {}", filterRequest);
        AnalyticsSummaryResponse response = analyticsService.getAnalyticsSummary(filterRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Genera la recomendación asistida del departamento municipal competente para atender una incidencia.
     * <p>
     * Admite dos modalidades operativas:
     * <ul>
     *   <li>Por identificador de incidencia ({@code incidentId}): recupera la incidencia persistida y evalúa
     *       sus atributos (título, descripción, categoría).</li>
     *   <li>Por atributos directos en tiempo real ({@code title}, {@code description}, {@code category}): útil para
     *       asistencia preventiva en formularios de creación o edición previa a la persistencia.</li>
     * </ul>
     * </p>
     *
     * @param incidentId  Identificador único de la incidencia ya registrada (opcional).
     * @param title       Título descriptivo de la incidencia (opcional).
     * @param description Detalle explicativo de la incidencia (opcional).
     * @param category    Categoría temática clasificada (opcional).
     * @return {@link ResponseEntity} con código HTTP 200 OK y la recomendación calculada {@link DepartmentRecommendationResponse}.
     */
    @GetMapping("/recommend-department")
    public ResponseEntity<DepartmentRecommendationResponse> recommendDepartment(
            @RequestParam(required = false) Long incidentId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String category) {
        log.info("Petición REST GET /api/analytics/recommend-department [incidentId={}, title={}, category={}]",
                incidentId, title, category);

        DepartmentRecommendationResponse recommendation;
        if (incidentId != null) {
            recommendation = intelligentRecommendationService.recommendDepartment(incidentId);
        } else {
            recommendation = intelligentRecommendationService.recommendDepartment(title, description, category);
        }

        return ResponseEntity.ok(recommendation);
    }
}
